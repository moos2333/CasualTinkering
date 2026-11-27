package com.npstra.casualtinkering.modifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

public class ThermalRayModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook, InventoryTickModifierHook {
    private static final int FLUID_COST_MB = 50;
    private static final int MIN_TEMP = 400;
    private static final float BASE_MULTIPLIER = 1.5f;
    private static final float MAX_MULTIPLIER = 10.0f;
    private static final float EFFECT_FLUID_MULTIPLIER = 0.5f;
    private static final int OVERHEAT_THRESHOLD = 10;
    private static final int OVERHEAT_TICKS = 100;
    private static final double RAY_RANGE = 32.0;

    private static final ResourceLocation KEY_OVERHEAT_END = ResourceLocation.tryBuild("casualtinkering", "thermal_ray_overheat_end");
    private static final ResourceLocation KEY_SHOTS = ResourceLocation.tryBuild("casualtinkering", "thermal_ray_shots");

    private static long getOverheatEnd(ModDataNBT data) {
        return data.get(KEY_OVERHEAT_END, CompoundTag::getLong);
    }

    private static void setOverheatEnd(ModDataNBT data, long value) {
        data.put(KEY_OVERHEAT_END, LongTag.valueOf(value));
    }

    private static int getChargeTicks(IToolStackView tool) {
        float attackSpeed = tool.getStats().get(ToolStats.ATTACK_SPEED);
        return Mth.clamp(Math.round(45f - (attackSpeed - 0.5f) * 10f), 20, 45);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING, ModifierHooks.INVENTORY_TICK);
        hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
        hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(1000));
    }

    @Override
    public Component getDisplayName(int level) {
        return getDisplayName();
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.isBroken() || source != InteractionSource.RIGHT_CLICK) return InteractionResult.PASS;
        if (isOverheated(tool, player.level())) {
            if (player.level().isClientSide) player.displayClientMessage(Component.translatable("modifier.thermal_ray.overheat").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!canFire(ToolTankHelper.TANK_HELPER.getFluid(tool))) {
            if (player.level().isClientSide) player.displayClientMessage(Component.translatable("modifier.thermal_ray.no_fuel").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        int chargeTicks = getChargeTicks(tool);
        GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, chargeTicks / 20f);
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
        int chargeTicks = getChargeTicks(tool);
        if (usedTicks >= chargeTicks) player.releaseUsingItem();
    }

    @Override
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
        if (entity.level().isClientSide || !modifier.matches(activeModifier.getId()) || !(entity instanceof Player player)) return;
        int usedTicks = useDuration - timeLeft;
        int chargeTicks = getChargeTicks(tool);
        if (usedTicks >= chargeTicks) executeShot(tool, player);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide) return;
        ModDataNBT data = tool.getPersistentData();
        long end = getOverheatEnd(data);
        if (end != 0L && end <= world.getGameTime()) {
            setOverheatEnd(data, 0L);
            data.putInt(KEY_SHOTS, 0);
        }
    }

    private void executeShot(IToolStackView tool, Player player) {
        Level level = player.level();
        FluidStack fluid = ToolTankHelper.TANK_HELPER.getFluid(tool);
        if (!canFire(fluid)) return;

        ModDataNBT data = tool.getPersistentData();
        int shots = data.getInt(KEY_SHOTS) + 1;
        data.putInt(KEY_SHOTS, shots);
        if (shots >= OVERHEAT_THRESHOLD) {
            setOverheated(data, level);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.75, player.getZ(), 8, 0.5, 0.75, 0.5, 0.05);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 0.6f);
            }
        }

        FluidStack effectFluid = fluid.copy();
        fluid.shrink(FLUID_COST_MB);
        if (fluid.isEmpty()) ToolTankHelper.TANK_HELPER.setFluid(tool, FluidStack.EMPTY);
        else ToolTankHelper.TANK_HELPER.setFluid(tool, fluid);

        ToolDamageUtil.damage(tool, 1, player, player.getUseItem());

        float baseDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        if (baseDamage <= 0) baseDamage = 1.0f;
        boolean fuel = isFuel(effectFluid);
        float totalDamage = baseDamage * getDamageMultiplier(effectFluid);

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(RAY_RANGE));

        LivingEntity target = null;
        double closest = RAY_RANGE;
        AABB searchBox = new AABB(start, end).inflate(1.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive());
        for (LivingEntity entity : entities) {
            AABB bb = entity.getBoundingBox().inflate(0.3);
            Vec3 hitVec = bb.clip(start, end).orElse(null);
            if (hitVec != null) {
                double dist = start.distanceTo(hitVec);
                if (dist < closest) {
                    closest = dist;
                    target = entity;
                }
            }
        }

        if (target != null) {
            DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), player);
            target.hurt(source, totalDamage);
            if (fuel) {
                target.setSecondsOnFire(4);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
                }
            } else {
                FluidEffects effects = FluidEffectManager.INSTANCE.find(effectFluid.getFluid());
                if (effects.hasEntityEffects()) {
                    int dose = Math.min(effects.getAmount(effectFluid.getFluid()), FLUID_COST_MB);
                    FluidEffectContext.Entity context = FluidEffectContext.builder(level).user(player).stack(player.getMainHandItem()).target(target);
                    effects.applyToEntity(new FluidStack(effectFluid.getFluid(), dose), dose, context, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
            double distance = start.distanceTo(end);
            int steps = Math.max(1, (int) (distance * 0.5));
            for (int i = 0; i <= steps; i++) {
                double f = distance * i / steps;
                double px = start.x + look.x * f, py = start.y + look.y * f, pz = start.z + look.z * f;
                serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0, 0, 0);
                if (i % 4 == 0 && level.random.nextInt(2) == 0)
                    serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0, 0, 0);
            }
        }
    }

    private boolean isOverheated(IToolStackView tool, Level level) {
        return getOverheatEnd(tool.getPersistentData()) > level.getGameTime();
    }

    private void setOverheated(ModDataNBT data, Level level) {
        setOverheatEnd(data, level.getGameTime() + OVERHEAT_TICKS);
        data.putInt(KEY_SHOTS, 0);
    }

    private static boolean isFuel(FluidStack fluid) {
        Fluid fluidType = fluid.getFluid();
        return fluidType == Fluids.LAVA || fluidType == TinkerFluids.blazingBlood.get();
    }

    private static boolean canFire(FluidStack fluid) {
        return !fluid.isEmpty() && fluid.getAmount() >= FLUID_COST_MB && fluid.getFluid().getFluidType().getTemperature() >= MIN_TEMP;
    }

    private static float getDamageMultiplier(FluidStack fluid) {
        int temp = fluid.getFluid().getFluidType().getTemperature();
        float multiplier = BASE_MULTIPLIER * (1.0f + Math.max(0, temp - 1000) / 2000.0f);
        if (!isFuel(fluid)) multiplier *= EFFECT_FLUID_MULTIPLIER;
        return Math.min(multiplier, MAX_MULTIPLIER);
    }
}
