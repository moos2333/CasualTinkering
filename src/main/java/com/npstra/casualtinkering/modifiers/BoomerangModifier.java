package com.npstra.casualtinkering.modifiers;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import com.npstra.casualtinkering.entity.ThrownBoomerang;
import com.npstra.casualtinkering.init.ModEntities;

public class BoomerangModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook {
    private static final int BASE_CHARGE_TICKS = 10;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING);
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.SPEAR;
    }

    private int getChargeTicks(IToolStackView tool) {
        float attackSpeed = tool.getStats().get(ToolStats.ATTACK_SPEED);
        if (attackSpeed >= 2.0f) {
            return BASE_CHARGE_TICKS;
        } else if (attackSpeed <= 0.5f) {
            return BASE_CHARGE_TICKS * 4;
        } else {
            float ratio = (2.0f - attackSpeed) / 1.5f;
            return BASE_CHARGE_TICKS + (int) (BASE_CHARGE_TICKS * 3 * ratio);
        }
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.isBroken() || source != InteractionSource.RIGHT_CLICK) {
            return InteractionResult.PASS;
        }
        int chargeTicks = getChargeTicks(tool);
        GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, chargeTicks / 20f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || entity.level().isClientSide) return;
        int total = getUseDuration(tool, modifier);
        int used = total - timeLeft;
        int required = getChargeTicks(tool);
        if (used >= required) {
            player.releaseUsingItem();
        }
    }

    @Override
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if (entity.level().isClientSide || !modifier.matches(activeModifier.getId()) || !(entity instanceof Player player)) return;
        int used = useDuration - timeLeft;
        int required = getChargeTicks(tool);
        if (used < required) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || stack != player.getUseItem()) return;

        // 消耗1点耐久
        boolean broken = ToolDamageUtil.damage(tool, 1, player, stack);
        if (broken) {
            // 工具已损坏，无法投掷，播放损坏音效
            player.playSound(SoundEvents.ITEM_BREAK, 1.0f, 1.0f);
            return;
        }

        Level level = player.level();
        float velocity = ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.VELOCITY);
        float inaccuracy = ModifierUtil.getInaccuracy(tool, player);
        ThrownBoomerang boomerang = new ThrownBoomerang(level, player, stack, tool, velocity, inaccuracy);
        level.addFreshEntity(boomerang);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, 1f);
        player.getCooldowns().addCooldown(stack.getItem(), 10);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) player.getInventory().removeItem(stack);
        }
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {}
}