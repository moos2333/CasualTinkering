package com.npstra.casualtinkering.modifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.VolatileData;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class ThermalRayModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook {
    private static final int FLUID_COST_MB = 50;
    private static final float BASE_DAMAGE_MULTIPLIER = 1.5f;
    private static final int OVERHEAT_THRESHOLD = 10;
    private static final int OVERHEAT_TICKS = 100;
    private static final int CHARGE_DURATION_TICKS = 10;
    private static final float MAX_TEMP_BONUS = 1.5f;
    private static final int BASE_TEMP = 1000;

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACTION);
        hookBuilder.addHook(this, ModifierHooks.TOOL_USING);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.isBroken() || source != InteractionSource.RIGHT_CLICK) return InteractionResult.PASS;
        VolatileData data = tool.getVolatileData();
        if (isOverheated(data)) {
            if (player.level().isClientSide) player.displayClientMessage(Component.translatable("modifier.thermal_ray.overheat").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (ToolTankHelper.TANK_HELPER.getFluid(tool).getAmount() < FLUID_COST_MB) {
            if (player.level().isClientSide) player.displayClientMessage(Component.translatable("modifier.thermal_ray.no_fuel").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, CHARGE_DURATION_TICKS / 20f);
        return InteractionResult.SUCCESS;
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
        if (!(entity instanceof Player player) || entity.level().isClientSide) return;
        int usedTicks = getUseDuration(tool, modifier) - timeLeft;
        if (usedTicks >= CHARGE_DURATION_TICKS) player.releaseUsingItem();
    }

    @Override
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if (entity.level().isClientSide || !modifier.matches(activeModifier.getId()) || !(entity instanceof Player player)) return;
        if ((useDuration - timeLeft) >= CHARGE_DURATION_TICKS) executeShot(tool, modifier, player);
    }

    private void executeShot(IToolStackView tool, ModifierEntry modifier, Player player) {
        Level level = player.level();
        VolatileData data = tool.getVolatileData();
        int shots = data.getInt("thermal_ray_shots") + 1;
        data.putInt("thermal_ray_shots", shots);
        if (shots >= OVERHEAT_THRESHOLD) {
            setOverheated(data, true);
            data.putInt("thermal_ray_shots", 0);
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 8; i++)
                    serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX() + (level.random.nextDouble() - 0.5) * 1.0, player.getY() + level.random.nextDouble() * 1.5, player.getZ() + (level.random.nextDouble() - 0.5) * 1.0, 0, 0, 0.1, 0, 0.1);
            }
        }
        ToolTankHelper.TANK_HELPER.drain(tool, FLUID_COST_MB);
        float baseDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        float tempBonus = getFluidTemperatureBonus(tool);
        float totalDamage = baseDamage * (BASE_DAMAGE_MULTIPLIER + tempBonus);
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(player, e -> true, 32);
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), player);
            target.hurt(source, totalDamage);
            target.setSecondsOnFire(4);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
            Vec3 end = start.add(look.scale(32));
            double distance = start.distanceTo(end);
            int steps = (int) (distance * 0.5);
            for (int i = 0; i <= steps; i++) {
                Vec3 pos = start.add(look.scale(distance * i / steps));
                serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                if (i % 4 == 0 && level.random.nextInt(2) == 0)
                    serverLevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private boolean isOverheated(VolatileData data) {
        if (!data.getBoolean("thermal_ray_overheated")) return false;
        int ticks = data.getInt("thermal_ray_overheat_ticks");
        if (ticks <= 0) {
            data.putBoolean("thermal_ray_overheated", false);
            return false;
        }
        data.putInt("thermal_ray_overheat_ticks", ticks - 1);
        return true;
    }

    private void setOverheated(VolatileData data, boolean overheated) {
        data.putBoolean("thermal_ray_overheated", overheated);
        if (overheated) data.putInt("thermal_ray_overheat_ticks", OVERHEAT_TICKS);
    }

    private float getFluidTemperatureBonus(IToolStackView tool) {
        int temp = ToolTankHelper.TANK_HELPER.getFluid(tool).getFluid().getFluidType().getTemperature();
        if (temp <= BASE_TEMP) return 0.0f;
        float bonus = (temp - BASE_TEMP) / 500.0f * MAX_TEMP_BONUS;
        return Math.min(bonus, MAX_TEMP_BONUS);
    }
}