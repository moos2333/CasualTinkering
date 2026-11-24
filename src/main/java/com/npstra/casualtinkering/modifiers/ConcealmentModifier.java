package com.npstra.casualtinkering.modifiers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.armor.ProtectionModifierHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class ConcealmentModifier extends Modifier implements ProtectionModifierHook {
    private static final float PER_BLOCK = 0.125F;
    private static final float CAP = 1.25F;

    @Override
    public float getProtectionModifier(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slot, DamageSource source, float amount) {
        if (!DamageSourcePredicate.CAN_PROTECT.matches(source)) {
            return amount;
        }
        Entity attacker = source.getEntity();
        LivingEntity holder = context.getEntity();
        if (attacker == null || attacker == holder) {
            return amount;
        }
        float blocks = attacker.distanceTo(holder);
        return amount + Math.min(blocks * PER_BLOCK, CAP) * modifier.getEffectiveLevel();
    }
}
