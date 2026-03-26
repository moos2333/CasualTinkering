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
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CircularSawAttackModifier extends Modifier implements GeneralInteractionModifierHook {

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        float speed = tool.getStats().get(ToolStats.ATTACK_SPEED);
        speed = Mth.clamp(speed, 1.0f, 4.0f);
        return (int)(20.0f / speed);
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BOW;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        int maxUse = getUseDuration(tool, modifier);
        int useDuration = maxUse - timeLeft;
        if (useDuration >= maxUse * 0.9f) {
            performSweepAttack(tool, player);
            player.stopUsingItem();
        }
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