package com.npstra.casualtinkering.modifiers;

import com.google.common.collect.ImmutableList;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import slimeknights.mantle.util.RecipeMatch;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.List;
import java.util.Optional;

public class ModMagicLance extends ModifierTrait {
    public ModMagicLance() {
        super("magic_lance", 0xE5C07B, 1, 1);
        addRecipeMatch(new ItemCombination(1,
                new ItemStack(Items.DIAMOND_SHOVEL),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.GOLD_INGOT)
        ));
    }

    @Override
    public boolean canApplyCustom(ItemStack stack) {
        return stack.getItem() instanceof com.npstra.casualtinkering.tools.MagicDevice;
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
            List<ItemStack> found = new java.util.LinkedList<>();
            java.util.Set<Integer> needed = new java.util.HashSet<>();
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