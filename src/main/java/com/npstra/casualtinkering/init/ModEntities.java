package com.npstra.casualtinkering.init;

import com.npstra.casualtinkering.CasualTinkering;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import com.npstra.casualtinkering.entity.ThrownBoomerang;
import com.npstra.casualtinkering.entity.ThrownKnife;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CasualTinkering.MODID);

    public static final RegistryObject<EntityType<EntityMagicSword>> MAGIC_SWORD = ENTITIES.register("magic_sword",
            () -> EntityType.Builder.<EntityMagicSword>of(EntityMagicSword::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(CasualTinkering.MODID, "magic_sword").toString()));

    public static final RegistryObject<EntityType<ThrownKnife>> THROWN_KNIFE = ENTITIES.register("thrown_knife",
            () -> EntityType.Builder.<ThrownKnife>of(ThrownKnife::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(new ResourceLocation(CasualTinkering.MODID, "thrown_knife").toString()));

    public static final RegistryObject<EntityType<ThrownBoomerang>> THROWN_BOOMERANG = ENTITIES.register("thrown_boomerang",
            () -> EntityType.Builder.<ThrownBoomerang>of(ThrownBoomerang::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build(new ResourceLocation(CasualTinkering.MODID, "thrown_boomerang").toString()));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}