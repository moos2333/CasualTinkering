package com.npstra.casualtinkering.modifiers;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircularSawAttackModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook {

    private static final Map<Integer, Integer> LAST_ATTACK_TICK = new HashMap<>();

    @Override
    protected void registerHooks(Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (!tool.isBroken() && source == InteractionSource.RIGHT_CLICK) {
            float attackSpeed = tool.getStats().get(ToolStats.ATTACK_SPEED);
            attackSpeed = Mth.clamp(attackSpeed, 1.0f, 4.0f);
            float drawTime = 1.0f / attackSpeed;
            GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, drawTime);
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

        int currentTick = player.tickCount;
        Integer lastTick = LAST_ATTACK_TICK.get(player.getId());
        if (lastTick == null) {
            LAST_ATTACK_TICK.put(player.getId(), currentTick);
            return;
        }

        float attackSpeed = tool.getStats().get(ToolStats.ATTACK_SPEED);
        attackSpeed = Mth.clamp(attackSpeed, 1.0f, 4.0f);
        int intervalTicks = (int)(20.0f / attackSpeed);
        if (currentTick - lastTick >= intervalTicks) {
            performSweepAttack(tool, player);
            LAST_ATTACK_TICK.put(player.getId(), currentTick);
        }
    }

    @Override
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;
        if (!modifier.matches(activeModifier.getId())) return;

        // 确保最后一次攻击也会在松开时触发
        int currentTick = player.tickCount;
        Integer lastTick = LAST_ATTACK_TICK.get(player.getId());
        if (lastTick == null || currentTick - lastTick >= 1) {
            performSweepAttack(tool, player);
        }
        LAST_ATTACK_TICK.remove(player.getId());
    }

    private void performSweepAttack(IToolStackView tool, Player player) {
        Level level = player.level();
        float radius = 4.0f;
        double halfAngleRad = Math.toRadians(45.0);
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);

        AABB aabb = new AABB(eyePos.x - radius, eyePos.y - radius, eyePos.z - radius,
                eyePos.x + radius, eyePos.y + radius, eyePos.z + radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != player && !(e instanceof Player));
        List<LivingEntity> targets = new ArrayList<>();

        for (LivingEntity target : entities) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eyePos);
            double dist = toTarget.length();
            if (dist > radius || dist < 0.5) continue;
            double angle = Math.acos(Mth.clamp(lookVec.dot(toTarget.normalize()), -1.0, 1.0));
            if (angle <= halfAngleRad) targets.add(target);
        }

        if (targets.isEmpty()) return;

        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        boolean hit = ToolAttackUtil.attackEntity(tool, player, targets.get(0));

        InteractionHand hand = InteractionHand.MAIN_HAND;
        EquipmentSlot slotType = Util.getSlotType(hand);
        for (int i = 1; i < targets.size(); i++) {
            ToolAttackContext context = ToolAttackContext.attacker(player)
                    .target(targets.get(i))
                    .slot(slotType, hand)
                    .cooldown(1.0f)
                    .extraAttack()
                    .toolAttributes(tool)
                    .build();
            if (ToolAttackUtil.performAttack(tool, context)) hit = true;
        }

        if (hit) {
            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
            ToolDamageUtil.damageAnimated(tool, 1, player, InteractionHand.MAIN_HAND);
        }
    }
}