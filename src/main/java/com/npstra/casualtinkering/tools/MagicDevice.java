package com.npstra.casualtinkering.tools;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.materials.HandleMaterialStats;
import slimeknights.tconstruct.library.materials.HeadMaterialStats;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.materials.MaterialTypes;
import slimeknights.tconstruct.library.tinkering.Category;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.tools.SwordCore;
import slimeknights.tconstruct.library.tools.ToolNBT;
import slimeknights.tconstruct.library.utils.EntityUtil;
import slimeknights.tconstruct.library.utils.ToolHelper;
import slimeknights.tconstruct.tools.TinkerTools;
import com.npstra.casualtinkering.entity.EntityMagicSword;

import java.util.List;
import java.util.Random;

public class MagicDevice extends SwordCore
{
    public static final float DURABILITY_MODIFIER = 1.0F;
    private static final float MAGIC_DAMAGE_RATIO = 0.1F;

    public MagicDevice()
    {
        super(PartMaterialType.handle(TinkerTools.toolRod),
                PartMaterialType.head(TinkerTools.swordBlade),
                PartMaterialType.head(TinkerTools.panHead));
        addCategory(Category.WEAPON);
        setRegistryName("casualtinkering", "magicdevice");
        setTranslationKey("casualtinkering.magicdevice");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase player, int timeLeft)
    {
        if (world.isRemote) return;

        int useTime = getMaxItemUseDuration(stack) - timeLeft;
        if (useTime < 30) return;

        float range = 8.0F;
        Vec3d eye = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d look = player.getLook(1.0F);
        RayTraceResult mop = EntityUtil.raytraceEntity(player, eye, look, range, true);
        if (mop == null || mop.typeOfHit != RayTraceResult.Type.ENTITY) return;

        Entity target = mop.entityHit;
        if (target == player) return;
        if (!(target instanceof EntityLivingBase)) return;

        float totalDamage = ToolHelper.getActualAttack(stack);
        float magicDamage = totalDamage * 0.5F;
        Random rand = world.rand;

        for (int i = 0; i < 2; i++)
        {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double radius = 2.0;
            double offsetX = radius * Math.cos(angle);
            double offsetZ = radius * Math.sin(angle);
            double x = target.posX + offsetX;
            double z = target.posZ + offsetZ;
            double y = target.posY + 1.5;
            EntityMagicSword sword = new EntityMagicSword(world, player, (EntityLivingBase) target, magicDamage, x, y, z);
            world.spawnEntity(sword);
        }

        if (player instanceof EntityPlayer && !((EntityPlayer) player).isCreative())
        {
            ToolHelper.damageTool(stack, 1, player);
        }

        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack)
    {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.BOW;
    }

    @Override
    public float damagePotential()
    {
        return 1.0F;
    }

    @Override
    public double attackSpeed()
    {
        return 1.8D;
    }

    @Override
    public int[] getRepairParts()
    {
        return new int[]{1, 2};
    }

    @Override
    public float getRepairModifierForPart(int index)
    {
        return index == 1 || index == 2 ? 0.5F : 1.0F;
    }

    @Override
    public boolean dealDamage(ItemStack stack, EntityLivingBase player, Entity entity, float damage)
    {
        boolean hit;
        if (player instanceof EntityPlayer)
        {
            hit = dealMagicDamage(DamageSource.causePlayerDamage((EntityPlayer) player), entity, damage);
            if (hit && !player.world.isRemote && entity instanceof EntityLivingBase)
            {
                EntityPlayer thePlayer = (EntityPlayer) player;
                if (thePlayer.getCooledAttackStrength(0.0F) == 1.0F)
                {
                    float totalDamage = ToolHelper.getActualAttack(stack);
                    float magicDamage = totalDamage * 0.5F;
                    Random rand = player.world.rand;
                    double angle = rand.nextDouble() * 2 * Math.PI;
                    double radius = 2.0;
                    double offsetX = radius * Math.cos(angle);
                    double offsetZ = radius * Math.sin(angle);
                    double x = entity.posX + offsetX;
                    double z = entity.posZ + offsetZ;
                    double y = entity.posY + 1.5;
                    EntityMagicSword sword = new EntityMagicSword(player.world, player, (EntityLivingBase) entity, magicDamage, x, y, z);
                    player.world.spawnEntity(sword);
                }
            }
        }
        else
        {
            hit = dealMagicDamage(DamageSource.causeMobDamage(player), entity, damage);
        }
        return hit;
    }

    private boolean dealMagicDamage(DamageSource source, Entity target, float damage)
    {
        float normalDamage = damage * (1.0F - MAGIC_DAMAGE_RATIO);
        float magicDamage = damage * MAGIC_DAMAGE_RATIO;

        boolean hit = target.attackEntityFrom(source, normalDamage);

        if (hit && target instanceof EntityLivingBase)
        {
            EntityLivingBase targetLiving = (EntityLivingBase) target;
            int hurtResistantTime = targetLiving.hurtResistantTime;
            float lastDamage = targetLiving.lastDamage;

            targetLiving.hurtResistantTime = 0;
            targetLiving.lastDamage = 0;
            targetLiving.attackEntityFrom(source.setDamageBypassesArmor().setMagicDamage(), magicDamage);

            targetLiving.hurtResistantTime = Math.max(hurtResistantTime, targetLiving.hurtResistantTime);
            targetLiving.lastDamage = Math.max(lastDamage, targetLiving.lastDamage);
        }

        return hit;
    }

    @Override
    public ToolNBT buildTagData(List<Material> materials)
    {
        HandleMaterialStats handle = materials.get(0).getStatsOrUnknown(MaterialTypes.HANDLE);
        HeadMaterialStats blade = materials.get(1).getStatsOrUnknown(MaterialTypes.HEAD);
        HeadMaterialStats disk = materials.get(2).getStatsOrUnknown(MaterialTypes.HEAD);

        ToolNBT data = new ToolNBT();
        data.head(blade, disk);
        data.handle(handle);

        data.attack = 1.5F + blade.attack * 0.25F + disk.attack * 0.55F;
        data.durability *= DURABILITY_MODIFIER;

        return data;
    }
}