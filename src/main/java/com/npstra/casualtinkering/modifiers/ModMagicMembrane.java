package com.npstra.casualtinkering.modifiers;

import com.google.common.collect.ImmutableList;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import slimeknights.mantle.util.RecipeMatch;
import slimeknights.tconstruct.library.modifiers.ModifierNBT;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.library.utils.ToolBuilder;
import com.npstra.casualtinkering.tools.MagicDevice;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;

public class ModMagicMembrane extends ModifierTrait {

    public ModMagicMembrane() {
        super("magic_membrane", 0x00CCCC, 2, 0);
        addRecipeMatch(new ItemCombination(1,
                new ItemStack(Items.LEATHER),
                new ItemStack(Items.LEATHER),
                new ItemStack(Items.LEATHER),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.GOLD_INGOT)
        ));
    }

    @Override
    public boolean canApplyCustom(ItemStack stack) {
        return stack.getItem() instanceof MagicDevice;
    }

    @Override
    public void applyEffect(NBTTagCompound rootCompound, NBTTagCompound modifierTag) {
        super.applyEffect(rootCompound, modifierTag);
        int level = ModifierNBT.readInteger(modifierTag).level;
        int targetLevel = Math.min(level * 2, Enchantments.PROTECTION.getMaxLevel());
        while (ToolBuilder.getEnchantmentLevel(rootCompound, Enchantments.PROTECTION) < targetLevel) {
            ToolBuilder.addEnchantment(rootCompound, Enchantments.PROTECTION);
        }
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
                java.util.Iterator<Integer> iter = needed.iterator();
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