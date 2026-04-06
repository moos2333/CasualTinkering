package com.npstra.casualtinkering;

import com.npstra.casualtinkering.client.renderer.RenderMagicSword;
import com.npstra.casualtinkering.common.CasualTinkeringCommonProxy;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import com.npstra.casualtinkering.modifiers.ModAutoDevice;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

@Mod(modid = CasualTinkering.MODID, name = CasualTinkering.NAME, version = CasualTinkering.VERSION,
        dependencies = "required-after:forge@[14.23.5.2855,);" +
                "required-after:mantle@[1.12-1.3.3.55,);" +
                "required-after:tconstruct@[1.12.2-2.13.0.183,);")
public class CasualTinkering {
    public static final String MODID = "casualtinkering";
    public static final String NAME = "Casual Tinkering";
    public static final String VERSION = "0.1.3";

    @SidedProxy(clientSide = "com.npstra.casualtinkering.client.CasualTinkeringClientProxy",
            serverSide = "com.npstra.casualtinkering.common.CasualTinkeringCommonProxy")
    public static CasualTinkeringCommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "magic_sword"), EntityMagicSword.class, "magic_sword", 0, this, 64, 10, true);
        new ModAutoDevice();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.initToolGuis();
        proxy.registerRenderers();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}