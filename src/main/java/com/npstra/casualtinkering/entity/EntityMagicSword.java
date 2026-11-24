package com.npstra.casualtinkering.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityMagicSword extends Entity {
    private static final EntityDataAccessor<String> MATERIAL = SynchedEntityData.defineId(EntityMagicSword.class, EntityDataSerializers.STRING);
    private static final int MAX_LIFE = 100;
    private static final double SPAWN_RADIUS = 2.0;
    private static final double LAUNCH_SPEED = 0.6;
    private static final double CHASE_SPEED = 0.5;

    private float damage;
    private int life = MAX_LIFE;
    private LivingEntity shooter;
    private LivingEntity target;

    public EntityMagicSword(EntityType<? extends EntityMagicSword> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public EntityMagicSword(EntityType<? extends EntityMagicSword> type, Level level, LivingEntity shooter, LivingEntity target, float damage, String material) {
        this(type, level);
        this.shooter = shooter;
        this.target = target;
        this.damage = damage;
        this.entityData.set(MATERIAL, material);

        double angle = level.random.nextDouble() * Mth.TWO_PI;
        this.setPos(
                target.getX() + SPAWN_RADIUS * Math.cos(angle),
                target.getY() + 1.5,
                target.getZ() + SPAWN_RADIUS * Math.sin(angle));
        this.setDeltaMovement(target.position().subtract(this.position()).normalize().scale(LAUNCH_SPEED));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.tickCount % 5 == 0) {
                this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.5, this.getZ(), 0, 0, 0);
            }
            return;
        }
        if (this.life-- <= 0 || this.target == null || !this.target.isAlive()) {
            this.discard();
            return;
        }
        if (this.getBoundingBox().intersects(this.target.getBoundingBox())) {
            this.attack();
            this.discard();
            return;
        }
        Vec3 toTarget = this.target.position().subtract(this.position());
        if (toTarget.lengthSqr() > 1.0E-4) {
            this.setDeltaMovement(toTarget.normalize().scale(CHASE_SPEED));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void attack() {
        DamageSource source = this.level().damageSources().indirectMagic(this, this.shooter);
        LivingEntity target = this.target;
        int invulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        try {
            target.hurt(source, this.damage);
        } finally {
            target.invulnerableTime = invulnerableTime;
        }
    }

    public String getMaterial() {
        return this.entityData.get(MATERIAL);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MATERIAL, "");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(MATERIAL, tag.getString("material"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("material", getMaterial());
    }
}
