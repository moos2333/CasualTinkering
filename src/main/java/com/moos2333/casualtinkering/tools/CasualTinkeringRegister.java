package com.moos2333.casualtinkering.tools;

import com.moos2333.casualtinkering.config.ModConfig;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.tconstruct.common.ModelRegisterUtil;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.tools.ToolCore;

@Mod.EventBusSubscriber
public class CasualTinkeringRegister {
    public static ToolCore circularSaw;
    public static ToolCore magicdevice;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (ModConfig.enableCircularSaw) {
            circularSaw = new CircularSaw();
            event.getRegistry().register(circularSaw);
            TinkerRegistry.registerToolForgeCrafting(circularSaw);
        }

        if (ModConfig.enableMagicDevice) {
            magicdevice = new MagicDevice();
            event.getRegistry().register(magicdevice);
            TinkerRegistry.registerToolCrafting(magicdevice);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (circularSaw != null) {
            ModelRegisterUtil.registerToolModel(circularSaw);
        }
        if (magicdevice != null) {
            ModelRegisterUtil.registerToolModel(magicdevice);
        }
    }
}