package com.npstra.casualtinkering;

import com.npstra.casualtinkering.common.CasualTinkeringCommonProxy;
import com.npstra.casualtinkering.client.CasualTinkeringClientProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = CasualTinkering.MODID, name = CasualTinkering.NAME, version = CasualTinkering.VERSION,
        dependencies = "required-after:forge@[14.23.5.2855,);" +
                "required-after:mantle@[1.12-1.3.3.55,);" +
                "required-after:tconstruct@[1.12.2-2.13.0.183,);")
public class CasualTinkering {
    public static final String MODID = "casualtinkering";
    public static final String NAME = "Casual Tinkering";
    public static final String VERSION = "0.0.5";

    @SidedProxy(clientSide = "com.npstra.casualtinkering.client.CasualTinkeringClientProxy",
            serverSide = "com.npstra.casualtinkering.common.CasualTinkeringCommonProxy")
    public static CasualTinkeringCommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.initToolGuis();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}