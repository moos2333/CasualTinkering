package com.npstra.casualtinkering;

import com.npstra.casualtinkering.client.CasualTinkeringClientProxy;
import com.npstra.casualtinkering.common.CasualTinkeringCommonProxy;
import com.npstra.casualtinkering.config.ModConfig;
import com.npstra.casualtinkering.entity.EntityMagicLance;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import com.npstra.casualtinkering.modifiers.ModAutoDevice;
import com.npstra.casualtinkering.modifiers.ModMagicLance;
import com.npstra.casualtinkering.modifiers.ModOverclock;
import com.npstra.casualtinkering.modifiers.ModPrecisionSawing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = CasualTinkering.MODID, name = CasualTinkering.NAME, version = CasualTinkering.VERSION,
        dependencies = "required-after:forge@[14.23.5.2855,);" +
                "required-after:mantle@[1.12-1.3.3.55,);" +
                "required-after:tconstruct@[1.12.2-2.13.0.183,);")
public class CasualTinkering {
    public static final String MODID = "casualtinkering";
    public static final String NAME = "Casual Tinkering";
    public static final String VERSION = "0.1.14";

    @SidedProxy(clientSide = "com.npstra.casualtinkering.client.CasualTinkeringClientProxy",
            serverSide = "com.npstra.casualtinkering.common.CasualTinkeringCommonProxy")
    public static CasualTinkeringCommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "magic_sword"), EntityMagicSword.class, "magic_sword", 0, this, 64, 10, true);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "magic_lance"), EntityMagicLance.class, "magic_lance", 1, this, 64, 10, true);
        if (ModConfig.enableAutoDevice) {
            new ModAutoDevice();
        }
        if (ModConfig.enablePrecisionSawing) {
            new ModPrecisionSawing();
        }
        if (ModConfig.enableOverclock) {
            new ModOverclock();
        }
        if (ModConfig.enableMagicLance) {
            new ModMagicLance();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.initToolGuis();
        proxy.registerRenderers();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            proxy.registerBookPages();
        }
    }
}