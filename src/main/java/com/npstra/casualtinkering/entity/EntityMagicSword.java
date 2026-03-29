package com.npstra.casualtinkering.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityMagicSword extends Entity
{
    private EntityLivingBase target;
    private EntityLivingBase shooter;
    private float damage;
    private int ticksAlive;
    private static final int MAX_TICKS = 100;

    public EntityMagicSword(World world)
    {
        super(world);
        setSize(0.5F, 0.5F);
        ticksAlive = 0;
    }

    public EntityMagicSword(World world, EntityLivingBase shooter, EntityLivingBase target, float damage, double posX, double posY, double posZ)
    {
        this(world);
        this.shooter = shooter;
        this.target = target;
        this.damage = damage;
        setPosition(posX, posY, posZ);

        double dx = target.posX - posX;
        double dy = target.posY + target.height / 2.0D - posY;
        double dz = target.posZ - posZ;
        double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 0.0D)
        {
            double speed = 0.8D;
            motionX = dx / distance * speed;
            motionY = dy / distance * speed;
            motionZ = dz / distance * speed;
        }
    }

    @Override
    protected void entityInit()
    {
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        ticksAlive++;

        if (ticksAlive > MAX_TICKS)
        {
            setDead();
            return;
        }

        if (!world.isRemote)
        {
            if (target == null || !target.isEntityAlive())
            {
                setDead();
                return;
            }

            move(MoverType.SELF, motionX, motionY, motionZ);

            if (this.getEntityBoundingBox().intersects(target.getEntityBoundingBox()))
            {
                attackTarget();
                setDead();
            }
        }
        else
        {
            move(MoverType.SELF, motionX, motionY, motionZ);
        }
    }

    private void attackTarget()
    {
        if (!world.isRemote)
        {
            int hurtResistantTime = target.hurtResistantTime;
            float lastDamage = target.lastDamage;
            target.hurtResistantTime = 0;
            target.lastDamage = 0;

            DamageSource source;
            if (shooter instanceof EntityPlayer)
            {
                source = DamageSource.causePlayerDamage((EntityPlayer) shooter).setDamageBypassesArmor().setMagicDamage();
            }
            else
            {
                source = DamageSource.causeMobDamage(shooter).setDamageBypassesArmor().setMagicDamage();
            }

            target.attackEntityFrom(source, damage);

            target.hurtResistantTime = Math.max(hurtResistantTime, target.hurtResistantTime);
            target.lastDamage = Math.max(lastDamage, target.lastDamage);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound)
    {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound)
    {
    }
}