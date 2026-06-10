package com.npstra.casualtinkering.modifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
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
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

public class ThermalRayModifier extends Modifier implements GeneralInteractionModifierHook, UsingToolModifierHook, InventoryTickModifierHook {
    private static final int FLUID_COST_MB = 50;
    private static final float BASE_DAMAGE_MULTIPLIER = 1.5f;
    private static final int OVERHEAT_THRESHOLD = 10;
    private static final int OVERHEAT_TICKS = 100;
    private static final int CHARGE_DURATION_TICKS = 30;
    private static final float MAX_TEMP_BONUS = 1.5f;
    private static final int BASE_TEMP = 1000;

    private static final ResourceLocation KEY_OVERHEATED = new ResourceLocation("casualtinkering", "thermal_ray_overheated");
    private static final ResourceLocation KEY_OVERHEAT_TICKS = new ResourceLocation("casualtinkering", "thermal_ray_overheat_ticks");
    private static final ResourceLocation KEY_SHOTS = new ResourceLocation("casualtinkering", "thermal_ray_shots");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING, ModifierHooks.INVENTORY_TICK);
        hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
        hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(1000));
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (tool.isBroken() || source != InteractionSource.RIGHT_CLICK) return InteractionResult.PASS;
        ModDataNBT data = tool.getPersistentData();
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
        if ((useDuration - timeLeft) >= CHARGE_DURATION_TICKS) executeShot(tool, player);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide) return;
        ModDataNBT data = tool.getPersistentData();
        if (data.getBoolean(KEY_OVERHEATED)) {
            int ticks = data.getInt(KEY_OVERHEAT_TICKS);
            if (ticks > 0) {
                data.putInt(KEY_OVERHEAT_TICKS, ticks - 1);
                if (ticks - 1 <= 0) data.putBoolean(KEY_OVERHEATED, false);
            } else {
                data.putBoolean(KEY_OVERHEATED, false);
            }
        }
    }

    private void executeShot(IToolStackView tool, Player player) {
        Level level = player.level();
        ModDataNBT data = tool.getPersistentData();
        int shots = data.getInt(KEY_SHOTS) + 1;
        data.putInt(KEY_SHOTS, shots);
        if (shots >= OVERHEAT_THRESHOLD) {
            setOverheated(data, true);
            data.putInt(KEY_SHOTS, 0);
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 8; i++)
                    serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX() + (level.random.nextDouble() - 0.5) * 1.0, player.getY() + level.random.nextDouble() * 1.5, player.getZ() + (level.random.nextDouble() - 0.5) * 1.0, 0, 0, 0.1, 0, 0.1);
            }
        }

        FluidStack fluid = ToolTankHelper.TANK_HELPER.getFluid(tool);
        if (!fluid.isEmpty() && fluid.getAmount() >= FLUID_COST_MB) {
            fluid.shrink(FLUID_COST_MB);
            if (fluid.isEmpty()) ToolTankHelper.TANK_HELPER.setFluid(tool, FluidStack.EMPTY);
            else ToolTankHelper.TANK_HELPER.setFluid(tool, fluid);
        }

        float baseDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        if (baseDamage <= 0) baseDamage = 1.0f;
        float tempBonus = getFluidTemperatureBonus(tool);
        float totalDamage = baseDamage * (BASE_DAMAGE_MULTIPLIER + tempBonus);

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(32));

        LivingEntity target = null;
        double closest = 32.0;
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
            target.setSecondsOnFire(4);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
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

    private boolean isOverheated(ModDataNBT data) {
        return data.getBoolean(KEY_OVERHEATED);
    }

    private void setOverheated(ModDataNBT data, boolean overheated) {
        data.putBoolean(KEY_OVERHEATED, overheated);
        if (overheated) data.putInt(KEY_OVERHEAT_TICKS, OVERHEAT_TICKS);
    }

    private float getFluidTemperatureBonus(IToolStackView tool) {
        FluidStack fluid = ToolTankHelper.TANK_HELPER.getFluid(tool);
        if (fluid.isEmpty()) return 0.0f;
        int temp = fluid.getFluid().getFluidType().getTemperature();
        if (temp <= BASE_TEMP) return 0.0f;
        float bonus = (temp - BASE_TEMP) / 500.0f * MAX_TEMP_BONUS;
        return Math.min(bonus, MAX_TEMP_BONUS);
    }
}