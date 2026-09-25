package com.npstra.casualtinkering.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.stats.StatList;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import slimeknights.tconstruct.common.TinkerNetwork;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.library.tools.ranged.IProjectile;
import slimeknights.tconstruct.library.traits.ITrait;
import slimeknights.tconstruct.library.utils.TinkerUtil;
import slimeknights.tconstruct.library.utils.ToolHelper;

import java.util.List;

public final class ParticlelessToolAttack {
    private ParticlelessToolAttack() {}

    public static boolean attackEntity(ItemStack stack, ToolCore tool, EntityLivingBase attacker, Entity target) {
        return attackEntity(stack, tool, attacker, target, null, true);
    }

    public static boolean attackEntity(ItemStack stack, ToolCore tool, EntityLivingBase attacker, Entity target,
                                       Entity projectile, boolean applyCooldown) {
        if (target == null || !target.canBeAttackedWithItem() || target.isEntityEqual(attacker) || !stack.hasTagCompound()) return false;
        if (ToolHelper.isBroken(stack) || attacker == null) return false;

        boolean isProjectile = projectile != null;
        EntityLivingBase living = target instanceof EntityLivingBase ? (EntityLivingBase) target : null;
        EntityPlayer player = attacker instanceof EntityPlayer ? (EntityPlayer) attacker : null;
        if (player != null && living instanceof EntityPlayer && !player.canAttackPlayer((EntityPlayer) living)) return false;

        List<ITrait> traits = TinkerUtil.getTraitsOrdered(stack);
        float baseDamage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float baseKnockback = attacker.isSprinting() ? 1.0F : 0.0F;
        boolean crit = attacker.fallDistance > 0.0F && !attacker.onGround && !attacker.isOnLadder()
                && !attacker.isInWater() && !attacker.isPotionActive(MobEffects.BLINDNESS) && !attacker.isRiding();
        for (ITrait t : traits) if (t.isCriticalHit(stack, attacker, living)) crit = true;

        float damage = baseDamage;
        if (living != null) for (ITrait t : traits) damage = t.damage(stack, attacker, living, baseDamage, damage, crit);
        if (crit) damage *= 1.5F;
        damage = ToolHelper.calcCutoffDamage(damage, tool.damageCutoff());

        float knockback = baseKnockback;
        if (living != null) for (ITrait t : traits) knockback = t.knockBack(stack, attacker, living, damage, baseKnockback, knockback, crit);

        float oldHP = living != null ? living.getHealth() : 0F;
        double oldX = target.motionX, oldY = target.motionY, oldZ = target.motionZ;

        SoundEvent sound = null;
        if (player != null) {
            float cd = player.getCooledAttackStrength(0.5F);
            sound = cd > 0.9F ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG : SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;
            damage *= 0.2F + cd * cd * 0.8F;
        }

        if (living != null) {
            int hrt = living.hurtResistantTime;
            for (ITrait t : traits) { t.onHit(stack, attacker, living, damage, crit); living.hurtResistantTime = hrt; }
        }

        boolean hit = isProjectile && tool instanceof IProjectile
                ? ((IProjectile) tool).dealDamageRanged(stack, projectile, attacker, target, damage)
                : tool.dealDamage(stack, attacker, target, damage);

        if (hit && living != null) {
            float dealt = oldHP - living.getHealth();
            living.motionX = oldX + (living.motionX - oldX) * tool.knockback();
            living.motionY = oldY + (living.motionY - oldY) * tool.knockback() / 3.0;
            living.motionZ = oldZ + (living.motionZ - oldZ) * tool.knockback();
            if (knockback > 0F) {
                double vx = -MathHelper.sin(attacker.rotationYaw * (float) Math.PI / 180F) * knockback * 0.5F;
                double vz = MathHelper.cos(attacker.rotationYaw * (float) Math.PI / 180F) * knockback * 0.5F;
                target.addVelocity(vx, 0.1, vz);
                attacker.motionX *= 0.6; attacker.motionZ *= 0.6;
                attacker.setSprinting(false);
            }
            if (target instanceof EntityPlayerMP && target.velocityChanged) {
                TinkerNetwork.sendPacket(target, new SPacketEntityVelocity(target));
                target.velocityChanged = false;
                target.motionX = oldX; target.motionY = oldY; target.motionZ = oldZ;
            }
            if (player != null) {
                if (crit) { player.onCriticalHit(living); sound = SoundEvents.ENTITY_PLAYER_ATTACK_CRIT; }
                if (damage > baseDamage) player.onEnchantmentCritical(target);
            }
            attacker.setLastAttackedEntity(living);
            for (ITrait t : traits) t.afterHit(stack, attacker, living, dealt, crit, true);
            if (player != null) {
                stack.hitEntity(living, player);
                if (!player.capabilities.isCreativeMode && !isProjectile) tool.reduceDurabilityOnHit(stack, player, damage);
                player.addStat(StatList.DAMAGE_DEALT, Math.round(dealt * 10F));
                player.addExhaustion(0.3F);
                if (!isProjectile && applyCooldown) player.resetCooldown();
            } else if (!isProjectile) {
                tool.reduceDurabilityOnHit(stack, null, damage);
            }
        } else {
            sound = SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE;
        }

        if (player != null && sound != null) {
            player.world.playSound(null, player.posX, player.posY, player.posZ, sound, player.getSoundCategory(), 1F, 1F);
        }
        return true;
    }
}