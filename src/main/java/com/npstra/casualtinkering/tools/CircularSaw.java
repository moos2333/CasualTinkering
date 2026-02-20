package com.npstra.casualtinkering.tools;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.materials.*;
import slimeknights.tconstruct.library.tinkering.Category;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.tools.ToolNBT;
import slimeknights.tconstruct.library.utils.ToolHelper;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.tools.Hatchet;

import java.util.List;
import java.util.UUID;

public class CircularSaw extends Hatchet {

    public CircularSaw() {
        super(PartMaterialType.handle(TinkerTools.toughToolRod),
                PartMaterialType.extra(TinkerTools.toughBinding),
                PartMaterialType.head(TinkerTools.largePlate),
                PartMaterialType.head(TinkerTools.panHead));
        addCategory(Category.WEAPON);
        addCategory(Category.HARVEST);
        setRegistryName("casualtinkering", "circular_saw");
        setTranslationKey("casual_tinkering.circular_saw");
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        float speed = ToolHelper.getAttackSpeedStat(stack);
        speed = MathHelper.clamp(speed, 1.0f, 4.0f);
        float useTime = 20.0f / speed;
        return Math.max((int) useTime, 5);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase player, int count) {
        if (player.getEntityWorld().isRemote || !(player instanceof EntityPlayer)) {
            return;
        }

        int timeLeft = count;

        if (timeLeft <= 1) {
            EntityPlayer entityPlayer = (EntityPlayer) player;
            performSweepAttack(stack, player.getEntityWorld(), entityPlayer);
            player.stopActiveHand();
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        int maxDuration = getMaxItemUseDuration(stack);
        int useDuration = maxDuration - timeLeft;

        if (useDuration >= maxDuration * 0.9f) {
            performSweepAttack(stack, world, player);
        }
    }

    private void performSweepAttack(ItemStack stack, World world, EntityPlayer player) {
        float radius = 4.0f;
        float halfAngle = (float) Math.toRadians(45.0f);
        Vec3d playerPos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d lookVec = player.getLookVec();
        AxisAlignedBB aabb = new AxisAlignedBB(playerPos.x - radius, playerPos.y - radius, playerPos.z - radius, playerPos.x + radius, playerPos.y + radius, playerPos.z + radius);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);
        List<EntityLivingBase> targets = new java.util.ArrayList<>();
        for (EntityLivingBase target : entities) {
            if (target == player || target instanceof EntityPlayer) continue;
            Vec3d targetPos = new Vec3d(target.posX, target.posY + target.height * 0.5, target.posZ);
            Vec3d toTarget = targetPos.subtract(playerPos);
            double distance = toTarget.length();
            if (distance > radius || distance < 0.5) continue;
            toTarget = toTarget.normalize();
            double dot = lookVec.dotProduct(toTarget);
            double angle = Math.acos(MathHelper.clamp(dot, -1.0, 1.0));
            if (angle <= halfAngle) targets.add(target);
        }
        if (targets.isEmpty()) return;
        targets.sort((e1, e2) -> Double.compare(e1.getDistanceSq(player), e2.getDistanceSq(player)));
        boolean hitAny = false;
        EntityLivingBase primary = targets.get(0);
        if (ToolHelper.attackEntity(stack, this, player, primary, null, true)) hitAny = true;
        if (targets.size() > 1) {
            UUID speedUUID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
            UUID damageUUID = UUID.fromString("c0f8a7b6-9e5d-4c3b-8a2f-1e6d9c8b7a5f");
            AttributeModifier speedModifier = new AttributeModifier(speedUUID, "CircularSaw speed boost", 100.0, 0);
            AttributeModifier damageModifier = new AttributeModifier(damageUUID, "CircularSaw reduced damage", -0.67, 2);
            IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
            IAttributeInstance damageAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
            speedAttr.applyModifier(speedModifier);
            damageAttr.applyModifier(damageModifier);
            for (int i = 1; i < targets.size(); i++) {
                if (ToolHelper.attackEntity(stack, this, player, targets.get(i), null, true)) hitAny = true;
            }
            speedAttr.removeModifier(speedUUID);
            damageAttr.removeModifier(damageUUID);
        }
        if (hitAny) {
            player.playSound(net.minecraft.init.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
        }
    }

    @Override
    public ToolNBT buildTagData(List<Material> materials) {
        ToolNBT data = buildDefaultTag(materials);

        HeadMaterialStats head1 = materials.get(2).getStatsOrUnknown(MaterialTypes.HEAD);
        HeadMaterialStats head2 = materials.get(3).getStatsOrUnknown(MaterialTypes.HEAD);
        HandleMaterialStats handle = materials.get(0).getStatsOrUnknown(MaterialTypes.HANDLE);

        data.attack = 1.0f + (head1.attack * 0.8f) + (head2.attack * 0.6f);
        data.speed = 1.0f + (head1.miningspeed * 0.8f) + (head2.miningspeed * 0.6f);
        data.durability = (int) (head1.durability + (head2.durability * 0.5f));
        data.durability *= handle.modifier;
        data.harvestLevel = head2.harvestLevel;

        return data;
    }

    @Override
    public int[] getRepairParts() {
        return new int[]{2, 3};
    }

    @Override
    public float getRepairModifierForPart(int index) {
        return index == 2 ? 1.0f : 0.5f;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            addDefaultSubItems(subItems);
        }
    }

    @Override
    public float miningSpeedModifier() { return 1.0f; }

    @Override
    public float damagePotential() {
        return 1.0f;
    }

    @Override
    public double attackSpeed() {
        return 1.0d;
    }

    @Override
    public float knockback() {
        return 0.9f;
    }
}