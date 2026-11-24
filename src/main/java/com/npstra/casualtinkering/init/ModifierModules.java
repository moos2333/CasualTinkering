package com.npstra.casualtinkering.init;

import com.npstra.casualtinkering.modules.SnipingModule;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;

public class ModifierModules {
    public static void register() {
        ModifierModule.LOADER.register(ResourceLocation.tryBuild("casualtinkering", "sniping"), SnipingModule.LOADER);
    }
}
