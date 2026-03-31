package com.npstra.casualtinkering.modifiers;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.mantle.util.RecipeMatch;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.library.modifiers.ModifierNBT;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.library.utils.TinkerUtil;
import slimeknights.tconstruct.library.utils.ToolHelper;
import com.npstra.casualtinkering.entity.EntityMagicSword;
import com.npstra.casualtinkering.tools.MagicDevice;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;

public class ModAutoDevice extends ModifierTrait {
    private static final int COOLDOWN_TICKS = 20;
    private static final int RADIUS = 4;
    private static final Random RAND = new Random();

    public ModAutoDevice() {
        super("auto_device", 0xCC64FF, 3, 0);
        addRecipeMatch(new ItemCombination(1,
                new ItemStack(Items.REPEATER),
                new ItemStack(Items.REPEATER),
                new ItemStack(Items.REPEATER),
                new ItemStack(Items.REPEATER),
                new ItemStack(Blocks.REDSTONE_TORCH)
        ));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public boolean canApplyCustom(ItemStack stack) {
        return stack.getItem() instanceof MagicDevice;
    }

    private float getDamageFactor(int level) {
        switch (level) {
            case 2: return 0.4F;
            case 3: return 0.5F;
            default: return 0.25F;
        }
    }

    @Override
    public void onUpdate(ItemStack tool, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (world.isRemote) return;
        if (!isSelected) return;
        if (!(entity instanceof EntityPlayer)) return;
        if (ToolHelper.isBroken(tool)) return;

        EntityPlayer player = (EntityPlayer) entity;
        if (player.getHeldItemOffhand() == tool) return;
        if (player.isHandActive()) return;

        NBTTagCompound tag = TagUtil.getToolTag(tool);
        long lastTick = tag.getLong("auto_device_last_tick");
        long currentTick = world.getTotalWorldTime();
        if (currentTick - lastTick < COOLDOWN_TICKS) return;

        AxisAlignedBB aabb = new AxisAlignedBB(player.getPosition()).grow(RADIUS);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != player && e instanceof IMob);

        if (targets.isEmpty()) return;

        EntityLivingBase nearest = targets.stream()
                .min(Comparator.comparingDouble(e -> e.getDistanceSq(player)))
                .orElse(null);

        NBTTagCompound modifierTag = TinkerUtil.getModifierTag(tool, identifier);
        int level = ModifierNBT.readInteger(modifierTag).level;
        float magicDamage = ToolHelper.getActualAttack(tool) * getDamageFactor(level);
        if (magicDamage <= 0) return;

        double angle = RAND.nextDouble() * 2 * Math.PI;
        double radius = 2.0;
        double offsetX = radius * Math.cos(angle);
        double offsetZ = radius * Math.sin(angle);
        double x = nearest.posX + offsetX;
        double z = nearest.posZ + offsetZ;
        double y = nearest.posY + 1.5;

        EntityMagicSword sword = new EntityMagicSword(world, player, nearest, magicDamage, x, y, z);
        world.spawnEntity(sword);

        tag.setLong("auto_device_last_tick", currentTick);
        TagUtil.setToolTag(tool, tag);

        if (!player.isCreative()) {
            ToolHelper.damageTool(tool, 1, player);
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