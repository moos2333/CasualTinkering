package com.npstra.casualtinkering.modifiers;

import com.npstra.casualtinkering.util.MagicSwordHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;

public class MagicSwordControlModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (!tool.isBroken() && source == InteractionSource.RIGHT_CLICK) {
            GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, 0.5f);
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
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if (entity.level().isClientSide) return;
        if (!modifier.matches(activeModifier.getId())) return;
        if (!(entity instanceof Player player)) return;
        if (tool.isBroken()) return;

        int usedTicks = useDuration - timeLeft;
        if (usedTicks < 5) return;

        int level = (int) Math.min(modifier.getEffectiveLevel(), 3);
        if (level < 1) return;

        float magicDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE) * (0.25f - (level - 1) * 0.025f);
        if (magicDamage <= 0) return;

        LivingEntity target = findTarget(player, 8.0);
        if (target == null) return;

        MagicSwordHelper.spawnMagicSwords(player.level(), player, tool, target, level, magicDamage);
        ToolDamageUtil.damageAnimated(tool, 1, player, player.getUsedItemHand());
    }

    @Nullable
    private LivingEntity findTarget(Player player, double range) {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        AABB area = new AABB(eyePos, eyePos).inflate(range);

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eyePos);
            double distance = toTarget.length();
            if (distance > range || lookVec.dot(toTarget.normalize()) <= 0.5) continue;
            double distanceSq = target.distanceToSqr(player);
            if (distanceSq < nearestDistance) {
                nearestDistance = distanceSq;
                nearest = target;
            }
        }
        return nearest;
    }
}
