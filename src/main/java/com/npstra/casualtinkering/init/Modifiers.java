package com.npstra.casualtinkering.init;

import com.npstra.casualtinkering.CasualTinkering;
import com.npstra.casualtinkering.modifiers.*;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class Modifiers {
    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(CasualTinkering.MODID);

    public static final StaticModifier<CircularSawAttackModifier> CIRCULAR_SAW_ATTACK = MODIFIERS.register("circular_saw_attack", CircularSawAttackModifier::new);

    public static final StaticModifier<MagicSwordModifier> MAGIC_SWORD = MODIFIERS.register("magic_sword", MagicSwordModifier::new);

    public static final StaticModifier<MagicSwordControlModifier> MAGIC_SWORD_CONTROL = MODIFIERS.register("magic_sword_control", MagicSwordControlModifier::new);

    public static final StaticModifier<MagicSwordAutoModifier> MAGIC_SWORD_AUTO = MODIFIERS.register("magic_sword_auto", MagicSwordAutoModifier::new);

    public static final StaticModifier<MagicSwordCoopModifier> MAGIC_SWORD_COOP = MODIFIERS.register("magic_sword_coop", MagicSwordCoopModifier::new);

    public static void register(IEventBus bus) {
        MODIFIERS.register(bus);
    }
}