package com.npstra.casualtinkering.modules;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithPower;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

public record SnipingModule(LevelingValue bonus, int maxDistance) implements ModifierModule, ProjectileHitModifierHook, MeleeDamageModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.defaultHooks(ModifierHooks.PROJECTILE_HIT, ModifierHooks.MELEE_DAMAGE);
    private static final ResourceLocation BASE_VALUE = ResourceLocation.tryBuild("casualtinkering", "sniping_base");

    public static final RecordLoadable<SnipingModule> LOADER = RecordLoadable.create(
        LevelingValue.LOADABLE.defaultField("bonus", new LevelingValue(0.0F, 0.01F), SnipingModule::bonus),
        IntLoadable.FROM_ONE.defaultField("max_distance", 10, SnipingModule::maxDistance),
        SnipingModule::new);

    @Override
    public RecordLoadable<SnipingModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    private float getBonus(double distance, int level) {
        return (float) Math.min(distance, maxDistance) * bonus.computeForLevel(level);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, LivingEntity attacker, LivingEntity target, boolean notCancelled) {
        if (attacker == null || projectile.level().isClientSide) return false;

        float increase = getBonus(projectile.distanceTo(attacker), modifier.getLevel());
        if (increase <= 0.0F) return false;

        if (projectile instanceof AbstractArrow arrow) {
            double base = persistentData.getFloat(BASE_VALUE);
            if (base <= 0.0D) {
                base = arrow.getBaseDamage();
                persistentData.putFloat(BASE_VALUE, (float) base);
            }
            arrow.setBaseDamage(base * (1.0D + increase));
        } else if (projectile instanceof ProjectileWithPower power) {
            float base = persistentData.getFloat(BASE_VALUE);
            if (base <= 0.0F) {
                base = power.getPower();
                persistentData.putFloat(BASE_VALUE, base);
            }
            power.setPower(base * (1.0F + increase));
        }
        return false;
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (context.isProjectile()) return damage;
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null) return damage;

        float increase = getBonus(attacker.distanceTo(target), modifier.getLevel());
        return increase <= 0.0F ? damage : damage * (1.0F + increase);
    }
}
