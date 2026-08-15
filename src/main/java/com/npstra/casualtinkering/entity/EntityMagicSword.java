package com.npstra.casualtinkering.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityMagicSword extends Entity implements IEntityAdditionalSpawnData {
    private EntityLivingBase target;
    private EntityLivingBase shooter;
    private float damage;
    private int ticksAlive;
    private static final int MAX_TICKS = 100;
    private String bladeMaterialId = "manyullyn";
    private boolean direct;

    public EntityMagicSword(World world) {
        super(world);
        setSize(0.5F, 0.5F);
        ticksAlive = 0;
    }

    public EntityMagicSword(World world, EntityLivingBase shooter, EntityLivingBase target, float damage, double posX, double posY, double posZ, String bladeMaterialId, boolean direct) {
        this(world);
        this.shooter = shooter;
        this.target = target;
        this.damage = damage;
        this.bladeMaterialId = bladeMaterialId;
        this.direct = direct;
        setPosition(posX, posY, posZ);
        double dx = target.posX - posX;
        double dy = target.posY + target.height / 2.0D - posY;
        double dz = target.posZ - posZ;
        double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 0.0D) {
            double speed = 0.5D;
            motionX = dx / distance * speed;
            motionY = dy / distance * speed;
            motionZ = dz / distance * speed;
        }
        if (direct) {
            updateRotationToTarget();
        }
    }

    public String getBladeMaterialId() {
        return bladeMaterialId;
    }

    public boolean isDirect() {
        return direct;
    }

    private void updateRotationToTarget() {
        if (target != null && target.isEntityAlive()) {
            double dx = target.posX - this.posX;
            double dy = target.posY + target.height * 0.5 - this.posY;
            double dz = target.posZ - this.posZ;
            double dist = MathHelper.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) (-Math.atan2(dy, dist) * 180.0 / Math.PI);
            this.rotationYaw = yaw;
            this.rotationPitch = pitch;
        }
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        ticksAlive++;
        if (ticksAlive > MAX_TICKS) {
            setDead();
            return;
        }
        if (world.isRemote) {
            if (direct) {
                updateRotationToTarget();
            }
            move(MoverType.SELF, motionX, motionY, motionZ);
        } else {
            if (target == null || !target.isEntityAlive()) {
                setDead();
                return;
            }
            move(MoverType.SELF, motionX, motionY, motionZ);
            if (this.getEntityBoundingBox().intersects(target.getEntityBoundingBox())) {
                attackTarget();
                setDead();
            }
        }
    }

    private void attackTarget() {
        if (!world.isRemote) {
            int hurtResistantTime = target.hurtResistantTime;
            float lastDamage = target.lastDamage;
            target.hurtResistantTime = 0;
            target.lastDamage = 0;
            DamageSource source;
            if (shooter instanceof EntityPlayer) {
                source = DamageSource.causePlayerDamage((EntityPlayer) shooter).setDamageBypassesArmor().setMagicDamage();
            } else {
                source = DamageSource.causeMobDamage(shooter).setDamageBypassesArmor().setMagicDamage();
            }
            target.attackEntityFrom(source, damage);
            target.hurtResistantTime = Math.max(hurtResistantTime, target.hurtResistantTime);
            target.lastDamage = Math.max(lastDamage, target.lastDamage);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {}

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(shooter == null ? -1 : shooter.getEntityId());
        buffer.writeInt(target == null ? -1 : target.getEntityId());
        buffer.writeFloat(damage);
        buffer.writeDouble(posX);
        buffer.writeDouble(posY);
        buffer.writeDouble(posZ);
        buffer.writeDouble(motionX);
        buffer.writeDouble(motionY);
        buffer.writeDouble(motionZ);
        ByteBufUtils.writeUTF8String(buffer, bladeMaterialId);
        buffer.writeBoolean(direct);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        int shooterId = buffer.readInt();
        int targetId = buffer.readInt();
        damage = buffer.readFloat();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        motionX = buffer.readDouble();
        motionY = buffer.readDouble();
        motionZ = buffer.readDouble();
        bladeMaterialId = ByteBufUtils.readUTF8String(buffer);
        setPosition(x, y, z);
        if (shooterId != -1) shooter = (EntityLivingBase) world.getEntityByID(shooterId);
        if (targetId != -1) target = (EntityLivingBase) world.getEntityByID(targetId);
        direct = buffer.readBoolean();
        if (direct) {
            updateRotationToTarget();
        }
    }
}