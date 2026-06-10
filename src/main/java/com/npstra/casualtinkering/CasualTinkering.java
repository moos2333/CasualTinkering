package com.npstra.casualtinkering;

import com.npstra.casualtinkering.init.ModEntities;
import com.npstra.casualtinkering.init.ModItems;
import com.npstra.casualtinkering.init.Modifiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CasualTinkering.MODID)
public class CasualTinkering {
    public static final String MODID = "casualtinkering";

    public CasualTinkering() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        Modifiers.register(bus);
        ModEntities.register(bus);
    }
}