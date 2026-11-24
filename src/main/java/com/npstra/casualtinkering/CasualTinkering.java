package com.npstra.casualtinkering;

import com.npstra.casualtinkering.init.ModEntities;
import com.npstra.casualtinkering.init.ModFluids;
import com.npstra.casualtinkering.init.ModifierModules;
import com.npstra.casualtinkering.init.Modifiers;
import com.npstra.casualtinkering.modifiers.OversiphonModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CasualTinkering.MODID)
public class CasualTinkering {
    public static final String MODID = "casualtinkering";

    public CasualTinkering(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        Modifiers.register(bus);
        ModEntities.register(bus);
        ModFluids.register(bus);
        ModifierModules.register();
        MinecraftForge.EVENT_BUS.addListener(OversiphonModifier::onPickupXp);
    }
}
