package com.npstra.casualtinkering.init;

import com.npstra.casualtinkering.CasualTinkering;

import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;

public class ModFluids {
    private static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(CasualTinkering.MODID);

    public static final FlowingFluidObject<ForgeFlowingFluid> MOLTEN_VERDANT_STEEL = FLUIDS.register("molten_verdant_steel")
        .type(() -> new MoltenVerdantSteelFluidType(FluidType.Properties.create()
            .descriptionId("fluid.casualtinkering.molten_verdant_steel")
            .temperature(800).density(3000).viscosity(6000).lightLevel(10)
            .motionScale(0.1)))
        .block(fluid -> new VerdantSteelLiquidBlock(fluid, FluidDeferredRegister.createProperties(MapColor.COLOR_GREEN, 10)))
        .bucket()
        .flowing();

    public static void register(IEventBus bus) {
        FLUIDS.register(bus);
    }
}
