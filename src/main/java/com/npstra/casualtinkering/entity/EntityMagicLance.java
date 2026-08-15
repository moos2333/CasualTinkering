package com.npstra.casualtinkering.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import slimeknights.tconstruct.library.entity.EntityProjectileBase;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.tools.tools.Shovel;

import java.util.List;

public class EntityMagicLance extends EntityProjectileBase {
    private static final DamageSource MAGIC_LANCE_SOURCE = new DamageSource("magic_sword")
            .setMagicDamage()
            .setDamageBypassesArmor();
    private float damage;
    private EntityLivingBase target;
    private ItemStack shovelStack;
    private String bladeMaterialId;

    public EntityMagicLance(World world) {
        super(world);
        setSize(0.3F, 0.5F);
        pickupStatus = PickupStatus.DISALLOWED;
    }

    public EntityMagicLance(World world, EntityLivingBase shooter, EntityLivingBase target, float damage, double posX, double posY, double posZ, String bladeMaterialId) {
        this(world);
        this.shootingEntity = shooter;
        this.target = target;
        this.damage = damage;
        this.bladeMaterialId = bladeMaterialId;
        setPosition(posX, posY, posZ);
        Material mat = slimeknights.tconstruct.library.TinkerRegistry.getMaterial(bladeMaterialId);
        if (mat == null) mat = Material.UNKNOWN;
        ToolCore shovel = new Shovel();
        this.shovelStack = shovel.buildItem(java.util.Arrays.asList(mat, mat, mat));
        motionX = 0.0D;
        motionY = -0.5D;
        motionZ = 0.0D;
    }

    public ItemStack getShovelStack() {
        return shovelStack;
    }

    public String getBladeMaterialId() {
        return bladeMaterialId;
    }

    @Override
    protected void init() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (ticksExisted > 60) {
            setDead();
            return;
        }
        if (!world.isRemote) {
            if (target != null && !target.isEntityAlive()) {
                target = null;
            }
            AxisAlignedBB aabb = this.getEntityBoundingBox().grow(0.5);
            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb, e -> e != shootingEntity);
            if (!entities.isEmpty()) {
                EntityLivingBase hit = entities.get(0);
                applyDirectHit(hit);
                applySplashDamage(hit);
                setDead();
            }
        }
    }

    @Override
    public void onHitBlock(RayTraceResult raytraceResult) {
        if (!world.isRemote) {
            applySplashDamage(null);
            setDead();
        }
    }

    private void applyDirectHit(EntityLivingBase target) {
        if (shootingEntity == null) return;
        EntityLivingBase attacker = (EntityLivingBase) shootingEntity;
        float finalDamage = damage * 1.5f;
        int oldHurt = target.hurtResistantTime;
        float oldLast = target.lastDamage;
        target.hurtResistantTime = 0;
        target.lastDamage = 0;
        target.attackEntityFrom(MAGIC_LANCE_SOURCE, finalDamage);
        target.hurtResistantTime = Math.max(oldHurt, target.hurtResistantTime);
        target.lastDamage = Math.max(oldLast, target.lastDamage);
    }

    private void applySplashDamage(EntityLivingBase exclude) {
        if (shootingEntity == null) return;
        AxisAlignedBB aabb = new AxisAlignedBB(posX - 1.5, posY - 0.5, posZ - 1.5,
                posX + 1.5, posY + 0.5, posZ + 1.5);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb,
                e -> e != shootingEntity && e != exclude);
        for (EntityLivingBase e : entities) {
            if (e == shootingEntity || e == exclude) continue;
            float splashDamage = damage * 0.5f;
            int oldHurt = e.hurtResistantTime;
            float oldLast = e.lastDamage;
            e.hurtResistantTime = 0;
            e.lastDamage = 0;
            e.attackEntityFrom(MAGIC_LANCE_SOURCE, splashDamage);
            e.hurtResistantTime = Math.max(oldHurt, e.hurtResistantTime);
            e.lastDamage = Math.max(oldLast, e.lastDamage);
        }
    }

    @Override
    public ItemStack getArrowStack() {
        return shovelStack;
    }

    @Override
    public double getGravity() {
        return 0.0D;
    }

    @Override
    public double getSlowdown() {
        return 0.0D;
    }

    @Override
    public boolean getIsCritical() {
        return false;
    }

    @Override
    protected void playHitEntitySound() {}

    @Override
    protected void playHitBlockSound(float speed, IBlockState state) {}

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeInt(shootingEntity == null ? -1 : shootingEntity.getEntityId());
        buffer.writeInt(target == null ? -1 : target.getEntityId());
        buffer.writeFloat(damage);
        ByteBufUtils.writeUTF8String(buffer, bladeMaterialId != null ? bladeMaterialId : "manyullyn");
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        super.readSpawnData(buffer);
        int shooterId = buffer.readInt();
        int targetId = buffer.readInt();
        damage = buffer.readFloat();
        bladeMaterialId = ByteBufUtils.readUTF8String(buffer);
        if (shooterId != -1) {
            Entity e = world.getEntityByID(shooterId);
            if (e instanceof EntityLivingBase) shootingEntity = (EntityLivingBase) e;
        }
        if (targetId != -1) {
            Entity e = world.getEntityByID(targetId);
            if (e instanceof EntityLivingBase) target = (EntityLivingBase) e;
        }
        Material mat = slimeknights.tconstruct.library.TinkerRegistry.getMaterial(bladeMaterialId);
        if (mat == null) mat = Material.UNKNOWN;
        ToolCore shovel = new Shovel();
        this.shovelStack = shovel.buildItem(java.util.Arrays.asList(mat, mat, mat));
    }

    @Override
    protected void entityInit() {}

    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
    }
}