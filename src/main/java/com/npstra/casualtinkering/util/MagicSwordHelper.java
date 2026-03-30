package com.npstra.casualtinkering.util;

import com.npstra.casualtinkering.entity.EntityMagicSword;
import com.npstra.casualtinkering.init.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MagicSwordHelper {
    public static void spawnMagicSwords(Level level, LivingEntity shooter, LivingEntity target, int magicSwordLevel, float magicDamage, ItemStack renderSword) {
        if (level.isClientSide) return;
        for (int i = 0; i < magicSwordLevel; i++) {
            EntityMagicSword sword = new EntityMagicSword(ModEntities.MAGIC_SWORD.get(), level, shooter, target, magicDamage, renderSword);
            level.addFreshEntity(sword);
        }
    }
}