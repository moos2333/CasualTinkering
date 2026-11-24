package com.npstra.casualtinkering.init;

import java.util.function.Supplier;

import com.npstra.casualtinkering.CasualTinkering;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import slimeknights.tconstruct.library.utils.CustomExplosion;

public class VerdantSteelLiquidBlock extends LiquidBlock {
    private static final String KEY_BOOM = ResourceLocation.tryBuild(CasualTinkering.MODID, "boom").toString();
    private static final int BOOM_COOLDOWN = 20;
    private static final float BOOM_RADIUS = 1.0F;
    private static final float BOOM_DAMAGE = 2.0F;
    private static final float BOOM_KNOCKBACK = 0.5F;

    public VerdantSteelLiquidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity)) {
            return;
        }
        long time = level.getGameTime();
        CompoundTag data = entity.getPersistentData();
        if (data.getLong(KEY_BOOM) > time) {
            return;
        }
        data.putLong(KEY_BOOM, time + BOOM_COOLDOWN);
        new CustomExplosion(level, entity.position(), BOOM_RADIUS, null, null, BOOM_DAMAGE,
            level.damageSources().explosion(null, null), BOOM_KNOCKBACK, null, false,
            Explosion.BlockInteraction.KEEP).handleServer();
    }
}
