package com.npstra.casualtinkering;

import com.npstra.casualtinkering.init.ModEntities;
import com.npstra.casualtinkering.init.Modifiers;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CasualTinkering.MODID)
public class CasualTinkering {
    public static final String MODID = "casualtinkering";

    public CasualTinkering(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        Modifiers.register(bus);
        ModEntities.register(bus);
    }
}
