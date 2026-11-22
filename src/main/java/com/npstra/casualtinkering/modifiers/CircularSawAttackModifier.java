package com.npstra.casualtinkering.modifiers;

import net.minecraft.network.chat.Component;
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
import java.util.List;

public class CircularSawAttackModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook {

    private static final float[] SWEEP_COS_HALF_ANGLE = {
            (float) Math.cos(Math.toRadians(45.0)),
            (float) Math.cos(Math.toRadians(50.0)),
            (float) Math.cos(Math.toRadians(55.0)),
            (float) Math.cos(Math.toRadians(60.0))
    };
    private static final ModifierId SWEEPING_EDGE_ID = new ModifierId("tconstruct", "sweeping_edge");
    private static final float RADIUS = 4.0f;
    private static final float SWEEP_SPEED_MULTIPLIER = 4.0f;
    private static final float SWEEP_DAMAGE_RATIO = 0.20f;
    private static final float SECONDARY_BASE_RATIO = 0.40f;
    private static final float SECONDARY_RATIO_PER_LEVEL = 0.15f;

    @Override
    protected void registerHooks(Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING);
    }

    @Override
    public Component getDisplayName(int level) {
        return getDisplayName();
    }

    private static float getSweepInterval(IToolStackView tool) {
        float attackSpeed = Mth.clamp(tool.getStats().get(ToolStats.ATTACK_SPEED), 0.5f, 2.5f) * SWEEP_SPEED_MULTIPLIER;
        return Math.max(1.0f, 20.0f / attackSpeed);
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
        int elapsed = getUseDuration(tool, modifier) - timeLeft - 1;
        float interval = getSweepInterval(tool);
        if ((int) (elapsed / interval) > (int) ((elapsed - 1) / interval)) performSweepAttack(tool, player);
    }

    @Override
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if (entity.level().isClientSide || !modifier.matches(activeModifier.getId())) return;
        if (entity instanceof Player player) performSweepAttack(tool, player);
    }

    private void performSweepAttack(IToolStackView tool, Player player) {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        AABB area = new AABB(eyePos, eyePos).inflate(RADIUS);
        int sweepingLevel = tool.getModifierLevel(SWEEPING_EDGE_ID);
        double cosHalfAngle = SWEEP_COS_HALF_ANGLE[Math.min(sweepingLevel, SWEEP_COS_HALF_ANGLE.length - 1)];

        List<LivingEntity> targets = new ArrayList<>();
        LivingEntity primary = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player))) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eyePos);
            double distance = toTarget.length();
            if (distance > RADIUS || distance < 0.5 || lookVec.dot(toTarget.normalize()) < cosHalfAngle) continue;
            targets.add(target);
            double distanceSq = target.distanceToSqr(player);
            if (distanceSq < nearest) {
                nearest = distanceSq;
                primary = target;
            }
        }
        if (primary == null) return;

        ToolAttackContext context = ToolAttackContext.attacker(player)
                .target(primary)
                .hand(InteractionHand.MAIN_HAND)
                .baseDamage(tool.getStats().get(ToolStats.ATTACK_DAMAGE) * SWEEP_DAMAGE_RATIO)
                .cooldown(1.0f)
                .build();
        float oldHealth = primary.getHealth();
        boolean hit = ToolAttackUtil.performAttack(tool, context);
        float primaryDamage = oldHealth - primary.getHealth();

        if (primaryDamage > 0 && targets.size() > 1) {
            float secondaryDamage = primaryDamage * Math.min(SECONDARY_BASE_RATIO + SECONDARY_RATIO_PER_LEVEL * sweepingLevel, 1.0f);
            DamageSource source = player.damageSources().playerAttack(player);
            for (LivingEntity secondary : targets) {
                if (secondary != primary) {
                    ToolAttackUtil.attackEntitySecondary(source, secondaryDamage, secondary, secondary, false);
                }
            }
        }

        if (hit) {
            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
            ToolDamageUtil.damageAnimated(tool, 1, player, player.getUsedItemHand());
        }
    }
}
