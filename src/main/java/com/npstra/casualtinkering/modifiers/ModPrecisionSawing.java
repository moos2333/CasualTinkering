package com.npstra.casualtinkering.modifiers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.oredict.OreDictionary;
import slimeknights.mantle.util.RecipeMatch;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.library.utils.ToolHelper;
import com.npstra.casualtinkering.tools.CircularSaw;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;

public class ModPrecisionSawing extends ModifierTrait {

    private static final Map<String, ItemStack> LOG_TO_PLANK = Maps.newHashMap();

    public ModPrecisionSawing() {
        super("precision_sawing", 0xBBAA66, 1, 0);
        addRecipeMatch(new ItemCombination(1,
                new ItemStack(Items.COMPASS),
                new ItemStack(Items.COMPASS),
                new ItemStack(Items.COMPASS),
                new ItemStack(Items.COMPASS),
                new ItemStack(Items.COMPASS)
        ));
    }

    @Override
    public boolean canApplyCustom(ItemStack stack) {
        return stack.getItem() instanceof CircularSaw;
    }

    @Override
    public void blockHarvestDrops(ItemStack tool, BlockEvent.HarvestDropsEvent event) {
        if (!(tool.getItem() instanceof CircularSaw)) return;
        if (!ToolHelper.isToolEffective2(tool, event.getState())) return;

        IBlockState state = event.getState();
        ItemStack logStack = new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state));
        if (!isLog(logStack)) return;

        String key = keyForLog(logStack);
        ItemStack plankResult = LOG_TO_PLANK.get(key);
        if (plankResult == null) {
            plankResult = findPlankForLog(logStack, event.getWorld());
            LOG_TO_PLANK.put(key, plankResult);
        }

        if (!plankResult.isEmpty()) {
            event.getDrops().clear();
            event.getDrops().add(plankResult.copy());
        }
    }

    private static boolean isLog(ItemStack stack) {
        for (ItemStack log : OreDictionary.getOres("logWood", false)) {
            if (OreDictionary.itemMatches(log, stack, false)) return true;
        }
        return false;
    }

    private static String keyForLog(ItemStack stack) {
        ResourceLocation name = stack.getItem().getRegistryName();
        return (name != null ? name.toString() : "unknown") + ":" + stack.getMetadata();
    }

    private static ItemStack findPlankForLog(ItemStack logStack, World world) {
        InventoryCrafting inv = new InventoryCrafting(new net.minecraft.inventory.Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) { return false; }
        }, 1, 1);
        inv.setInventorySlotContents(0, logStack.copy());
        IRecipe recipe = CraftingManager.findMatchingRecipe(inv, world);
        if (recipe != null) {
            ItemStack result = recipe.getCraftingResult(inv);
            if (!result.isEmpty() && isPlank(result)) {
                result.setCount(6);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isPlank(ItemStack stack) {
        for (ItemStack plank : OreDictionary.getOres("plankWood", false)) {
            if (OreDictionary.itemMatches(plank, stack, false)) return true;
        }
        return false;
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