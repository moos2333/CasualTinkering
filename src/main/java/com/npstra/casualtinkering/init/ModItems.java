package com.npstra.casualtinkering.init;

import com.npstra.casualtinkering.CasualTinkering;
import com.npstra.casualtinkering.item.ThrowingKnifeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CasualTinkering.MODID);
    public static final RegistryObject<ThrowingKnifeItem> THROWING_KNIFE = ITEMS.register("throwing_knife",
            () -> new ThrowingKnifeItem(
                    new Item.Properties().stacksTo(16).fireResistant(),
                    ToolDefinition.create(new ResourceLocation(CasualTinkering.MODID, "throwing_knife"))
            ));
}