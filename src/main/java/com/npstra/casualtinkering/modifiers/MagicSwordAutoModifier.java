package com.npstra.casualtinkering.modifiers;

import com.npstra.casualtinkering.CasualTinkering;
import com.npstra.casualtinkering.util.MagicSwordHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class MagicSwordAutoModifier extends Modifier implements InventoryTickModifierHook {

    private static final ResourceLocation LAST_TICK_KEY = ResourceLocation.tryBuild(CasualTinkering.MODID, "auto_last_tick");
    private static final int[] INTERVALS = {0, 60, 40, 20};

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity entity, int slot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (level.isClientSide || !(entity instanceof Player player) || !isCorrectSlot || tool.isBroken()) return;

        int autoLevel = (int) Math.min(modifier.getEffectiveLevel(), 3);
        if (autoLevel < 1) return;

        ModDataNBT data = tool.getPersistentData();
        int currentTick = player.tickCount;
        int lastTick = data.getInt(LAST_TICK_KEY);
        if (lastTick <= 0 || lastTick > currentTick) {
            data.putInt(LAST_TICK_KEY, currentTick);
            return;
        }
        if (currentTick - lastTick < INTERVALS[autoLevel]) return;
        data.putInt(LAST_TICK_KEY, currentTick);

        LivingEntity nearest = findNearestEnemy(player, 9);
        if (nearest != null) {
            float magicDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE) * (0.15f - (autoLevel - 1) * 0.025f);
            if (magicDamage > 0) {
                MagicSwordHelper.spawnMagicSwords(level, player, tool, nearest, 1, magicDamage);
                ToolDamageUtil.damageAnimated(tool, 1, player);
            }
        }
    }

    private LivingEntity findNearestEnemy(Player player, int range) {
        AABB area = player.getBoundingBox().inflate(range);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            double distanceSq = target.distanceToSqr(player);
            if (distanceSq >= nearestDistance || !player.hasLineOfSight(target)) continue;
            nearestDistance = distanceSq;
            nearest = target;
        }
        return nearest;
    }
}
