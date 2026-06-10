package com.npstra.casualtinkering.client;

import com.npstra.casualtinkering.CasualTinkering;
import com.npstra.casualtinkering.client.renderer.RenderMagicSword;
import com.npstra.casualtinkering.client.renderer.ThrownKnifeRenderer;
import com.npstra.casualtinkering.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CasualTinkering.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (ModEntities.MAGIC_SWORD.isPresent()) {
            event.registerEntityRenderer(ModEntities.MAGIC_SWORD.get(), RenderMagicSword::new);
        }
        if (ModEntities.THROWN_KNIFE.isPresent()) {
            event.registerEntityRenderer(ModEntities.THROWN_KNIFE.get(), ThrownKnifeRenderer::new);
        }
    }
}