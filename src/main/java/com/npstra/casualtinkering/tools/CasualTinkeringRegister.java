package com.npstra.casualtinkering.tools;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.tconstruct.common.ModelRegisterUtil;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.tools.TinkerModifiers;

import static slimeknights.tconstruct.common.ModelRegisterUtil.registerModifierModel;

@Mod.EventBusSubscriber
public class CasualTinkeringRegister {
    public static ToolCore circularSaw;

    @SideOnly(Side.CLIENT)
    private static void registerModifierModels() {
        registerModifierModel(TinkerModifiers.modSharpness, new ResourceLocation("casual_tinkering:models/item/modifiers/sharpness"));
        registerModifierModel(TinkerModifiers.modDiamond, new ResourceLocation("casual_tinkering:models/item/modifiers/diamond"));
        registerModifierModel(TinkerModifiers.modEmerald, new ResourceLocation("casual_tinkering:models/item/modifiers/emerald"));
        registerModifierModel(TinkerModifiers.modHaste, new ResourceLocation("casual_tinkering:models/item/modifiers/haste"));
        registerModifierModel(TinkerModifiers.modFiery, new ResourceLocation("casual_tinkering:models/item/modifiers/fiery"));
        registerModifierModel(TinkerModifiers.modKnockback, new ResourceLocation("casual_tinkering:models/item/modifiers/knockback"));
        registerModifierModel(TinkerModifiers.modBaneOfArthopods, new ResourceLocation("casual_tinkering:models/item/modifiers/bane_of_arthopods"));
        registerModifierModel(TinkerModifiers.modBeheading, new ResourceLocation("casual_tinkering:models/item/modifiers/beheading"));
        registerModifierModel(TinkerModifiers.modGlowing, new ResourceLocation("casual_tinkering:models/item/modifiers/glowing"));
        registerModifierModel(TinkerModifiers.modLuck, new ResourceLocation("casual_tinkering:models/item/modifiers/luck"));
        registerModifierModel(TinkerModifiers.modMendingMoss, new ResourceLocation("casual_tinkering:models/item/modifiers/mending_moss"));
        registerModifierModel(TinkerModifiers.modNecrotic, new ResourceLocation("casual_tinkering:models/item/modifiers/necrotic"));
        registerModifierModel(TinkerModifiers.modReinforced, new ResourceLocation("casual_tinkering:models/item/modifiers/reinforced"));
        registerModifierModel(TinkerModifiers.modShulking, new ResourceLocation("casual_tinkering:models/item/modifiers/shulking"));
        registerModifierModel(TinkerModifiers.modSilktouch, new ResourceLocation("casual_tinkering:models/item/modifiers/silktouch"));
        registerModifierModel(TinkerModifiers.modSmite, new ResourceLocation("casual_tinkering:models/item/modifiers/smite"));
        registerModifierModel(TinkerModifiers.modSoulbound, new ResourceLocation("casual_tinkering:models/item/modifiers/soulbound"));
        registerModifierModel(TinkerModifiers.modWebbed, new ResourceLocation("casual_tinkering:models/item/modifiers/webbed"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        circularSaw = new CircularSaw();
        event.getRegistry().register(circularSaw);
        TinkerRegistry.registerToolForgeCrafting(circularSaw);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (circularSaw != null) {
            ModelRegisterUtil.registerToolModel(circularSaw);
            registerModifierModels();
        }
    }
}