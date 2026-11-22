package com.npstra.casualtinkering.modifiers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileShootModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;

public class TripleShotModifier extends Modifier implements ProjectileShootModifierHook {
    private static final ResourceLocation SPREAD_DONE = ResourceLocation.tryBuild("casualtinkering", "triple_shot_spread_done");
    private static final float SPREAD_ANGLE = 15.0F;
    private static final float[] SPREAD_OFFSETS = {-SPREAD_ANGLE, SPREAD_ANGLE};

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_SHOT);
    }

    @Override
    public Component getDisplayName(int level) {
        return getDisplayName();
    }

    @Override
    public void onProjectileShoot(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack ammo, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        if (!primary || persistentData.getBoolean(SPREAD_DONE) || projectile.level().isClientSide) return;
        if (!(shooter instanceof Player player) || !player.isShiftKeyDown()) return;
        if (ammo.isEmpty() || !(projectile instanceof ThrownShuriken)) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(ammo.getItem()) || held.getCount() < 3) {
            held = player.getOffhandItem();
            if (!held.is(ammo.getItem()) || held.getCount() < 3) return;
        }

        persistentData.putBoolean(SPREAD_DONE, true);
        if (!player.getAbilities().instabuild) held.shrink(2);

        Level level = projectile.level();
        float velocity = ConditionalStatModifierHook.getModifiedStat(tool, shooter, ToolStats.VELOCITY);
        for (float offset : SPREAD_OFFSETS) {
            ThrownShuriken knife = new ThrownShuriken(level, player);
            knife.onCreate(ammo, player);
            PersistentDataCapability.getOrWarn(knife).putBoolean(SPREAD_DONE, true);
            knife.shootFromRotation(player, player.getXRot(), player.getYRot() + offset, 0.0F, velocity, 1.0F);
            level.addFreshEntity(knife);
        }
    }
}
