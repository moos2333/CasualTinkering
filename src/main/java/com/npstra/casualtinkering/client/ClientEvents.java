package com.npstra.casualtinkering.client;

import com.npstra.casualtinkering.CasualTinkering;
import com.npstra.casualtinkering.client.renderer.GunbladeBulletRenderer;
import com.npstra.casualtinkering.client.renderer.RenderMagicSword;
import com.npstra.casualtinkering.client.renderer.ThrownBoomerangRenderer;
import com.npstra.casualtinkering.init.ModEntities;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CasualTinkering.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MAGIC_SWORD.get(), RenderMagicSword::new);
        event.registerEntityRenderer(ModEntities.THROWN_BOOMERANG.get(), ThrownBoomerangRenderer::new);
        event.registerEntityRenderer(ModEntities.GUNBLADE_BULLET.get(), GunbladeBulletRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Item saw = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild(CasualTinkering.MODID, "circular_saw"));
            if (saw != null) {
                ItemProperties.register(saw, ResourceLocation.tryBuild(CasualTinkering.MODID, "sawing"),
                        (stack, level, entity, seed) -> isSawing(saw, entity) ? 1.0F : 0.0F);
            }
        });
    }

    private static boolean isSawing(Item saw, LivingEntity entity) {
        if (entity == null || !entity.isUsingItem()) return false;
        ItemStack used = entity.getUseItem();
        return used.isEmpty() || used.getItem() == saw;
    }
}
