package com.npstra.casualtinkering.modifiers;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import slimeknights.mantle.util.RecipeMatch;
import slimeknights.tconstruct.library.modifiers.ModifierNBT;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.library.utils.TinkerUtil;
import com.npstra.casualtinkering.tools.CircularSaw;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ModWhirl extends ModifierTrait {

    private static final String IDENTIFIER = "ct_whirl";
    private static final float[] SPEED_BONUS = {0.0f, 1.0f, 2.0f, 3.0f};
    private static final float[] DAMAGE_PENALTY = {0.0f, 0.375f, 0.53f, 0.65f};
    private static final float[] SWEEP_PENALTY = {0.0f, 0.11f, 0.18f, 0.22f};

    public ModWhirl() {
        super(IDENTIFIER, 0x55FF55, 3, 0);
        addRecipeMatch(new ItemCombination(1,
                new ItemStack(Items.COMPARATOR),
                new ItemStack(Items.COMPARATOR),
                new ItemStack(Items.COMPARATOR),
                new ItemStack(Items.COMPARATOR),
                new ItemStack(Items.COMPARATOR)
        ));
    }

    @Override
    public boolean canApplyCustom(ItemStack stack) {
        return stack.getItem() instanceof CircularSaw;
    }

    public static int getLevel(ItemStack stack) {
        NBTTagCompound tag = TinkerUtil.getModifierTag(stack, IDENTIFIER);
        if (tag != null && tag.getSize() > 0) {
            return ModifierNBT.readInteger(tag).level;
        }
        return 0;
    }

    public static float getSpeedMultiplier(ItemStack stack) {
        int level = getLevel(stack);
        if (level <= 0) return 1.0f;
        return 1.0f + SPEED_BONUS[Math.min(level, 3)];
    }

    @Override
    public float damage(ItemStack tool, EntityLivingBase player, EntityLivingBase target, float damage, float newDamage, boolean isCritical) {
        if (!(player instanceof EntityPlayer)) return newDamage;
        EntityPlayer entityPlayer = (EntityPlayer) player;
        if (!entityPlayer.isHandActive() || entityPlayer.getActiveItemStack() != tool) return newDamage;
        int level = getLevel(tool);
        if (level <= 0) return newDamage;
        return newDamage * (1.0f - DAMAGE_PENALTY[Math.min(level, 3)]);
    }

    @Override
    public void afterHit(ItemStack tool, EntityLivingBase player, EntityLivingBase target, float damageDealt, boolean wasCritical, boolean wasHit) {
        if (!wasHit) return;
        if (!(player instanceof EntityPlayer)) return;
        EntityPlayer entityPlayer = (EntityPlayer) player;
        if (!entityPlayer.isHandActive() || entityPlayer.getActiveItemStack() != tool) return;
        if (getLevel(tool) <= 0) return;
        if (target.hurtResistantTime > 0) {
            target.hurtResistantTime = (int) (target.hurtResistantTime * 0.5f);
        }
    }

    public static float getSweepPenalty(ItemStack stack) {
        int level = getLevel(stack);
        if (level <= 0) return 0.0f;
        return SWEEP_PENALTY[Math.min(level, 3)];
    }

    private static class ItemCombination extends RecipeMatch {
        protected final NonNullList<ItemStack> itemStacks;

        public ItemCombination(int amountMatched, ItemStack... stacks) {
            super(amountMatched, 0);
            NonNullList<ItemStack> nonNullStacks = NonNullList.withSize(stacks.length, ItemStack.EMPTY);
            for (int i = 0; i < stacks.length; i++) {
                if (!stacks[i].isEmpty()) {
                    nonNullStacks.set(i, stacks[i].copy());
                }
            }
            this.itemStacks = nonNullStacks;
        }

        @Override
        public List<ItemStack> getInputs() {
            return ImmutableList.copyOf(this.itemStacks);
        }

        @Override
        public Optional<Match> matches(NonNullList<ItemStack> stacks) {
            List<ItemStack> found = new LinkedList<>();
            Set<Integer> needed = new HashSet<>();
            for (int i = 0; i < this.itemStacks.size(); i++) {
                if (!this.itemStacks.get(i).isEmpty()) {
                    needed.add(i);
                }
            }
            for (ItemStack stack : stacks) {
                Iterator<Integer> iter = needed.iterator();
                while (iter.hasNext()) {
                    int index = iter.next();
                    ItemStack template = this.itemStacks.get(index);
                    if (ItemStack.areItemsEqual(template, stack) && ItemStack.areItemStackTagsEqual(template, stack)) {
                        ItemStack copy = stack.copy();
                        copy.setCount(1);
                        found.add(copy);
                        iter.remove();
                        break;
                    }
                }
            }
            if (needed.isEmpty()) {
                return Optional.of(new Match(found, this.amountMatched));
            }
            return Optional.empty();
        }
    }
}