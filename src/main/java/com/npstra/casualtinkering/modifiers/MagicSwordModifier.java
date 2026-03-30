package com.npstra.casualtinkering.modifiers;

import com.npstra.casualtinkering.init.ModEntities;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class MagicSwordModifier extends Modifier implements MeleeHitModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (context.getLevel().isClientSide()) return;
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target == null || attacker == null) return;

        Player player = context.getPlayerAttacker();
        if (player == null) return;
        if (player.getAttackStrengthScale(0.0f) < 0.999f) return;

        int level = (int) Math.min(modifier.getEffectiveLevel(), 3);
        if (level < 1) return;

        float damagePercent = 0.2f - (level - 1) * 0.025f;
        float magicDamage = damageDealt * damagePercent;
        if (magicDamage <= 0) return;

        ItemStack renderSword = new ItemStack(Items.DIAMOND_SWORD);
        for (int i = 0; i < level; i++) {
            EntityMagicSword sword = new EntityMagicSword(ModEntities.MAGIC_SWORD.get(), context.getLevel(), attacker, target, magicDamage, renderSword);
            context.getLevel().addFreshEntity(sword);
        }
    }
}