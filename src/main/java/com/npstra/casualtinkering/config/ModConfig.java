package com.npstra.casualtinkering.config;

import com.npstra.casualtinkering.CasualTinkering;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = CasualTinkering.MODID)
public class ModConfig {

    @Mod.EventBusSubscriber(modid = CasualTinkering.MODID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(CasualTinkering.MODID)) {
                ConfigManager.sync(CasualTinkering.MODID, Config.Type.INSTANCE);
            }
        }
    }

    @Config.Comment("Enable or disable the Circular Saw tool")
    @Config.RequiresMcRestart
    public static boolean enableCircularSaw = true;

    @Config.Comment("Enable or disable the Magic Device tool")
    @Config.RequiresMcRestart
    public static boolean enableMagicDevice = true;

    @Config.Comment("Enable or disable the Auto Device modifier")
    @Config.RequiresMcRestart
    public static boolean enableAutoDevice = true;
}