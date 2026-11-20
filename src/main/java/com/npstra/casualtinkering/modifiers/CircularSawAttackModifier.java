package com.npstra.casualtinkering.modifiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CircularSawAttackModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook {

    private static final String KEY_LAST_ATTACK_TICK = "casualtinkering:circular_saw_last_tick";
    private static final double COS_HALF_ANGLE = Math.cos(Math.toRadians(45.0));
    private static final ModifierId SWEEPING_EDGE_ID = new ModifierId("tconstruct", "sweeping_edge");
    private static final float SWEEP_SPEED_MULTIPLIER = 2.0f;
    private static final float SWEEP_DAMAGE_RATIO = 0.25f;
    private static final float SECONDARY_BASE_RATIO = 0.50f;
    private static final float SECONDARY_RATIO_PER_LEVEL = 0.15f;

    @Override
    protected void registerHooks(Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING);
    }

    private static int getSweepInterval(IToolStackView tool) {
        float attackSpeed = Mth.clamp(tool.getStats().get(ToolStats.ATTACK_SPEED), 0.5f, 4.0f) * SWEEP_SPEED_MULTIPLIER;
        return Math.max(1, (int) (20.0f / attackSpeed));
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (!tool.isBroken() && source == InteractionSource.RIGHT_CLICK) {
            GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, getSweepInterval(tool) / 20.0f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BOW;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        long now = player.level().getGameTime();
        CompoundTag pdata = player.getPersistentData();
        long last = pdata.getLong(KEY_LAST_ATTACK_TICK);
        if (last <= 0L || last > now) {
            pdata.putLong(KEY_LAST_ATTACK_TICK, now);
            return;
        }

        int intervalTicks = getSweepInterval(tool);
        if (now - last >= intervalTicks) {
            performSweepAttack(tool, player);
            pdata.putLong(KEY_LAST_ATTACK_TICK, now);
        }
    }

    @Override
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;
        if (!modifier.matches(activeModifier.getId())) return;

        long now = player.level().getGameTime();
        CompoundTag pdata = player.getPersistentData();
        long last = pdata.getLong(KEY_LAST_ATTACK_TICK);
        if (last <= 0L || last > now || now - last >= 1) {
            performSweepAttack(tool, player);
        }
        pdata.remove(KEY_LAST_ATTACK_TICK);
    }

    private void performSweepAttack(IToolStackView tool, Player player) {
        Level level = player.level();
        float radius = 4.0f;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);

        AABB aabb = new AABB(
                eyePos.x - radius, eyePos.y - radius, eyePos.z - radius,
                eyePos.x + radius, eyePos.y + radius, eyePos.z + radius
        );
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != player && !(e instanceof Player));
        List<LivingEntity> targets = new ArrayList<>();

        for (LivingEntity target : entities) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eyePos);
            double dist = toTarget.length();
            if (dist > radius || dist < 0.5) continue;
            if (lookVec.dot(toTarget.normalize()) >= COS_HALF_ANGLE) targets.add(target);
        }

        if (targets.isEmpty()) return;

        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        LivingEntity primary = targets.get(0);
        float sweepDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE) * SWEEP_DAMAGE_RATIO;
        ToolAttackContext context = ToolAttackContext.attacker(player)
                .target(primary)
                .hand(InteractionHand.MAIN_HAND)
                .baseDamage(sweepDamage)
                .cooldown(1.0f)
                .build();
        float oldHealth = primary.getHealth();
        boolean hit = ToolAttackUtil.performAttack(tool, context);
        float primaryDamage = oldHealth - primary.getHealth();

        if (primaryDamage > 0 && targets.size() > 1) {
            int sweepingLevel = tool.getModifierLevel(SWEEPING_EDGE_ID);
            float secondaryRatio = Math.min(SECONDARY_BASE_RATIO + SECONDARY_RATIO_PER_LEVEL * sweepingLevel, 1.0f);
            float secondaryDamage = primaryDamage * secondaryRatio;
            DamageSource source = player.damageSources().playerAttack(player);

            for (int i = 1; i < targets.size(); i++) {
                LivingEntity secondary = targets.get(i);
                ToolAttackUtil.attackEntitySecondary(source, secondaryDamage, secondary, secondary, false);
            }
        }

        if (hit) {
            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
            ToolDamageUtil.damageAnimated(tool, 1, player, player.getUsedItemHand());
        }
    }
}