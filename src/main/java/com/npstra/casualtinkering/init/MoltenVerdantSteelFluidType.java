package com.npstra.casualtinkering.init;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fluids.FluidType;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.tconstruct.common.TinkerTags;

public class MoltenVerdantSteelFluidType extends TextureFluidType {
    public MoltenVerdantSteelFluidType(FluidType.Properties properties) {
        super(properties);
    }

    @Override
    public boolean canDrownIn(LivingEntity entity) {
        return !entity.getType().is(TinkerTags.EntityTypes.SLIMES);
    }
}
