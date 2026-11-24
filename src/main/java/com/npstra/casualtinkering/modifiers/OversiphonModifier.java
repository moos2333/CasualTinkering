package com.npstra.casualtinkering.modifiers;

import com.npstra.casualtinkering.CasualTinkering;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.slotless.OverslimeModifier;

public class OversiphonModifier extends Modifier {
    private static final ModifierId OVERSIPHON = ModifierId.tryBuild(CasualTinkering.MODID, "oversiphon");
    private static final ModifierId OVERSLIME = ModifierId.tryBuild("tconstruct", "overslime");
    private static final ResourceLocation KEY_WINDOW = ResourceLocation.tryBuild(CasualTinkering.MODID, "oversiphon_window");
    private static final ResourceLocation KEY_TAKEN = ResourceLocation.tryBuild(CasualTinkering.MODID, "oversiphon_taken");
    private static final int COOLDOWN = 100;

    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ExperienceOrb orb = event.getOrb();
        int value = orb.getValue();
        if (value <= 0) {
            return;
        }
        long time = player.level().getGameTime();
        int used = siphon(player, player.getMainHandItem(), value, time);
        if (used <= 0) {
            for (ItemStack armor : player.getArmorSlots()) {
                used = siphon(player, armor, value, time);
                if (used > 0) {
                    break;
                }
            }
        }
        if (used <= 0) {
            return;
        }
        event.setCanceled(true);
        orb.discard();
        int leftover = value - used;
        if (leftover > 0) {
            player.giveExperiencePoints(leftover);
        }
    }

    private static int siphon(ServerPlayer player, ItemStack stack, int value, long time) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);
        int level = tool.getModifiers().getLevel(OVERSIPHON);
        ModifierEntry overslimeEntry = tool.getModifiers().getEntry(OVERSLIME);
        if (level <= 0 || overslimeEntry.getLevel() <= 0) {
            return 0;
        }
        OverslimeModifier overslime = TinkerModifiers.overslime.get();
        int capacity = overslime.getCapacity(tool, overslimeEntry);
        int amount = overslime.getAmount(tool);
        if (amount >= capacity) {
            return 0;
        }

        ModDataNBT data = tool.getPersistentData();
        int window = data.getInt(KEY_WINDOW);
        int taken = data.getInt(KEY_TAKEN);
        if ((int) time - window >= COOLDOWN) {
            window = (int) time;
            taken = 0;
        }
        int limit = level + 2;
        if (taken >= limit) {
            return 0;
        }
        int points = Math.min((value + 1) / 2, Math.min(limit - taken, capacity - amount));
        if (points <= 0) {
            return 0;
        }
        overslime.addOverslime(tool, overslimeEntry, points);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.6F, 1.5F);
        data.putInt(KEY_WINDOW, window);
        data.putInt(KEY_TAKEN, taken + points);
        tool.updateStack(stack);
        player.containerMenu.broadcastChanges();
        return Math.min(value, points * 2);
    }
}
