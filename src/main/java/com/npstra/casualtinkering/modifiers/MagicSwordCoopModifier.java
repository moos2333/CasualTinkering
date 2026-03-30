package com.npstra.casualtinkering.modifiers;

import com.npstra.casualtinkering.CasualTinkering;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import com.npstra.casualtinkering.init.ModEntities;
import com.npstra.casualtinkering.init.Modifiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Random;

@Mod.EventBusSubscriber(modid = CasualTinkering.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MagicSwordCoopModifier extends Modifier {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getSource().getDirectEntity() instanceof EntityMagicSword) return;
        if (event.getSource().getMsgId().equals("magic_sword")) return;

        if (!(event.getSource().getEntity() instanceof Player player)) return;

        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) return;

        ToolStack tool = ToolStack.from(offhand);
        int coopLevel = tool.getModifierLevel(Modifiers.MAGIC_SWORD_COOP.getId());
        if (coopLevel < 1 || coopLevel > 3) return;

        int swordCount;
        float damagePercent;
        switch (coopLevel) {
            case 1:
                swordCount = 1;
                damagePercent = 0.15f;
                break;
            case 2:
                swordCount = 2;
                damagePercent = 0.125f;
                break;
            case 3:
                swordCount = 3;
                damagePercent = 0.10f;
                break;
            default:
                return;
        }

        float baseDamage = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        float magicDamage = baseDamage * damagePercent;
        if (magicDamage <= 0) return;

        LivingEntity target = event.getEntity();
        Level level = player.level();
        ItemStack renderSword = new ItemStack(Items.DIAMOND_SWORD);

        for (int i = 0; i < swordCount; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double radius = 2.0;
            double offsetX = radius * Math.cos(angle);
            double offsetZ = radius * Math.sin(angle);
            double x = target.getX() + offsetX;
            double z = target.getZ() + offsetZ;
            double y = target.getY() + 1.5;

            EntityMagicSword sword = new EntityMagicSword(ModEntities.MAGIC_SWORD.get(), level, player, target, magicDamage, renderSword);
            sword.setPos(x, y, z);
            level.addFreshEntity(sword);
        }
    }
}