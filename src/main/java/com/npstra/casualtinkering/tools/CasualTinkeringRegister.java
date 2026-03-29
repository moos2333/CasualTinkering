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
    public static ToolCore magicdevice;

    @SideOnly(Side.CLIENT)
    private static void registerModifierModels() {
        registerModifierModel(TinkerModifiers.modSharpness, new ResourceLocation("casualtinkering:models/item/modifiers/sharpness"));
        registerModifierModel(TinkerModifiers.modDiamond, new ResourceLocation("casualtinkering:models/item/modifiers/diamond"));
        registerModifierModel(TinkerModifiers.modEmerald, new ResourceLocation("casualtinkering:models/item/modifiers/emerald"));
        registerModifierModel(TinkerModifiers.modHaste, new ResourceLocation("casualtinkering:models/item/modifiers/haste"));
        registerModifierModel(TinkerModifiers.modFiery, new ResourceLocation("casualtinkering:models/item/modifiers/fiery"));
        registerModifierModel(TinkerModifiers.modKnockback, new ResourceLocation("casualtinkering:models/item/modifiers/knockback"));
        registerModifierModel(TinkerModifiers.modBaneOfArthopods, new ResourceLocation("casualtinkering:models/item/modifiers/bane_of_arthopods"));
        registerModifierModel(TinkerModifiers.modBeheading, new ResourceLocation("casualtinkering:models/item/modifiers/beheading"));
        registerModifierModel(TinkerModifiers.modGlowing, new ResourceLocation("casualtinkering:models/item/modifiers/glowing"));
        registerModifierModel(TinkerModifiers.modLuck, new ResourceLocation("casualtinkering:models/item/modifiers/luck"));
        registerModifierModel(TinkerModifiers.modMendingMoss, new ResourceLocation("casualtinkering:models/item/modifiers/mending_moss"));
        registerModifierModel(TinkerModifiers.modNecrotic, new ResourceLocation("casualtinkering:models/item/modifiers/necrotic"));
        registerModifierModel(TinkerModifiers.modReinforced, new ResourceLocation("casualtinkering:models/item/modifiers/reinforced"));
        registerModifierModel(TinkerModifiers.modShulking, new ResourceLocation("casualtinkering:models/item/modifiers/shulking"));
        registerModifierModel(TinkerModifiers.modSilktouch, new ResourceLocation("casualtinkering:models/item/modifiers/silktouch"));
        registerModifierModel(TinkerModifiers.modSmite, new ResourceLocation("casualtinkering:models/item/modifiers/smite"));
        registerModifierModel(TinkerModifiers.modSoulbound, new ResourceLocation("casualtinkering:models/item/modifiers/soulbound"));
        registerModifierModel(TinkerModifiers.modWebbed, new ResourceLocation("casualtinkering:models/item/modifiers/webbed"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        circularSaw = new CircularSaw();
        event.getRegistry().register(circularSaw);
        TinkerRegistry.registerToolForgeCrafting(circularSaw);

        magicdevice = new MagicDevice();
        event.getRegistry().register(magicdevice);
        TinkerRegistry.registerToolCrafting(magicdevice);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (circularSaw != null) {
            ModelRegisterUtil.registerToolModel(circularSaw);
            registerModifierModels();
        }
        if (magicdevice != null) {
            ModelRegisterUtil.registerToolModel(magicdevice);
            registerModifierModels();
        }
    }
}