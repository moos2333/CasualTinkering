package com.npstra.casualtinkering.tools;

import com.npstra.casualtinkering.config.ModConfig;
import com.npstra.casualtinkering.modifiers.ModOverclock;
import com.npstra.casualtinkering.modifiers.ModWhirl;
import com.npstra.casualtinkering.util.ParticlelessToolAttack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.ResourceLocation;
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

    private static final UUID SPEED_BOOST_UUID = UUID.fromString("a1b2c3d4-e5f6-4789-9123-4567890abcde");
    private static final float BASE_SWEEP_MODIFIER = 0.33f - 1.0f;

    public CircularSaw() {
        super(PartMaterialType.handle(TinkerTools.toughToolRod),
                PartMaterialType.extra(TinkerTools.toughToolRod),
                PartMaterialType.head(TinkerTools.largePlate),
                PartMaterialType.head(TinkerTools.panHead));
        addCategory(Category.WEAPON);
        addCategory(Category.HARVEST);
        setRegistryName("casualtinkering", "circular_saw");
        setTranslationKey("casual_tinkering.circular_saw");
        addPropertyOverride(new ResourceLocation("casualtinkering", "charging"),
                (stack, worldIn, entityIn) -> entityIn != null && entityIn.isHandActive() && entityIn.getActiveItemStack() == stack ? 1.0F : 0.0F);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (ToolHelper.isBroken(stack)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase player, int count) {
        if (player.getEntityWorld().isRemote || !(player instanceof EntityPlayer)) return;
        if (ToolHelper.isBroken(stack)) return;

        EntityPlayer entityPlayer = (EntityPlayer) player;
        int useDuration = getMaxItemUseDuration(stack) - count;

        if (useDuration > 0 && useDuration % getAttackInterval(stack) == 0) {
            performSweepAttack(stack, player.getEntityWorld(), entityPlayer);
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        int useDuration = getMaxItemUseDuration(stack) - timeLeft;
        int attacks = useDuration / getAttackInterval(stack);
        if (attacks > 0) {
            ToolHelper.damageTool(stack, attacks, player);
        }
    }

    @Override
    public void reduceDurabilityOnHit(ItemStack stack, EntityPlayer player, float damage) {
        if (player != null && player.isHandActive() && player.getActiveItemStack() == stack) {
            return;
        }
        super.reduceDurabilityOnHit(stack, player, damage);
    }

    private int getAttackInterval(ItemStack stack) {
        float speed = ToolHelper.getAttackSpeedStat(stack);
        speed = MathHelper.clamp(speed, 1.0f, 4.0f);
        speed *= ModWhirl.getSpeedMultiplier(stack);
        int minInterval = 5 - ModWhirl.getLevel(stack);
        return Math.max((int)(20.0f / speed), minInterval);
    }

    private boolean attack(ItemStack stack, EntityPlayer player, EntityLivingBase target) {
        if (ModConfig.particlelessAttack) {
            return ParticlelessToolAttack.attackEntity(stack, this, player, target, null, true);
        }
        return ToolHelper.attackEntity(stack, this, player, target, null, true);
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
            if (target == player) continue;
            if (!ModConfig.allowCircularSawPvP && target instanceof EntityPlayer) continue;
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
        if (attack(stack, player, primary)) hitAny = true;
        if (targets.size() > 1) {
            AttributeModifier speedModifier = new AttributeModifier(SPEED_BOOST_UUID, "CircularSaw speed boost", 99.0, 0);
            IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
            speedAttr.applyModifier(speedModifier);
            float sweepModifier = BASE_SWEEP_MODIFIER + ModOverclock.getSweepBonus(stack) - ModWhirl.getSweepPenalty(stack);
            ModOverclock.applyDamageReduction(player, sweepModifier);
            for (int i = 1; i < targets.size(); i++) {
                if (attack(stack, player, targets.get(i))) hitAny = true;
            }
            speedAttr.removeModifier(SPEED_BOOST_UUID);
            ModOverclock.removeDamageReduction(player);
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
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (ModConfig.particlelessAttack) {
            return ParticlelessToolAttack.attackEntity(stack, this, player, entity);
        }
        return super.onLeftClickEntity(stack, player, entity);
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
    public float damageCutoff() {
        return 20.0f;
    }

    @Override
    public float knockback() {
        return 0.9f;
    }
}