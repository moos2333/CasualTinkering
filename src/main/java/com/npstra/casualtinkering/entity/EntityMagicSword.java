package com.npstra.casualtinkering.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class EntityMagicSword extends Entity implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK = SynchedEntityData.defineId(EntityMagicSword.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_SHOOTER_ID = SynchedEntityData.defineId(EntityMagicSword.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(EntityMagicSword.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(EntityMagicSword.class, EntityDataSerializers.FLOAT);

    private static final int MAX_LIFE = 100;
    private int life = MAX_LIFE;
    private LivingEntity target;
    private LivingEntity shooter;

    public EntityMagicSword(EntityType<? extends EntityMagicSword> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public EntityMagicSword(EntityType<? extends EntityMagicSword> type, Level world, LivingEntity shooter, LivingEntity target, float damage, ItemStack renderStack) {
        this(type, world);
        this.shooter = shooter;
        this.target = target;
        this.getEntityData().set(DATA_SHOOTER_ID, shooter == null ? -1 : shooter.getId());
        this.getEntityData().set(DATA_TARGET_ID, target == null ? -1 : target.getId());
        this.getEntityData().set(DATA_DAMAGE, damage);
        this.getEntityData().set(DATA_RENDER_STACK, renderStack.copy());

        double angle = world.random.nextDouble() * 2 * Math.PI;
        double radius = 2.0;
        double x = target.getX() + radius * Math.cos(angle);
        double z = target.getZ() + radius * Math.sin(angle);
        double y = target.getY() + 1.5;
        this.setPos(x, y, z);

        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(dir.scale(0.6));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RENDER_STACK, ItemStack.EMPTY);
        this.entityData.define(DATA_SHOOTER_ID, -1);
        this.entityData.define(DATA_TARGET_ID, -1);
        this.entityData.define(DATA_DAMAGE, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.tickCount % 5 == 0) {
                this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.5, this.getZ(), 0, 0, 0);
            }
        } else {
            if (this.life-- <= 0) {
                this.discard();
                return;
            }
            if (this.target == null || !this.target.isAlive()) {
                this.discard();
                return;
            }
            if (this.getBoundingBox().intersects(this.target.getBoundingBox())) {
                this.attack();
                this.discard();
                return;
            }
            this.moveTowardsTarget();
        }
    }

    private void moveTowardsTarget() {
        Vec3 direction = this.target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(direction.scale(0.5));
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void attack() {
        if (this.level().isClientSide) return;
        LivingEntity target = this.target;
        if (target == null) return;
        float damage = this.getEntityData().get(DATA_DAMAGE);
        int shooterId = this.getEntityData().get(DATA_SHOOTER_ID);
        LivingEntity shooter = shooterId == -1 ? null : (LivingEntity) this.level().getEntity(shooterId);
        DamageSource source = this.level().damageSources().indirectMagic(this, shooter);
        int oldHurtResistant = target.invulnerableTime;
        target.invulnerableTime = 0;
        target.hurt(source, damage);
        target.invulnerableTime = oldHurtResistant;
    }

    public ItemStack getRenderStack() {
        return this.getEntityData().get(DATA_RENDER_STACK);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.getEntityData().get(DATA_SHOOTER_ID));
        buffer.writeInt(this.getEntityData().get(DATA_TARGET_ID));
        buffer.writeFloat(this.getEntityData().get(DATA_DAMAGE));
        buffer.writeItem(this.getEntityData().get(DATA_RENDER_STACK));
        buffer.writeDouble(this.getX());
        buffer.writeDouble(this.getY());
        buffer.writeDouble(this.getZ());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        int shooterId = buffer.readInt();
        int targetId = buffer.readInt();
        float damage = buffer.readFloat();
        ItemStack stack = buffer.readItem();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();

        this.getEntityData().set(DATA_SHOOTER_ID, shooterId);
        this.getEntityData().set(DATA_TARGET_ID, targetId);
        this.getEntityData().set(DATA_DAMAGE, damage);
        this.getEntityData().set(DATA_RENDER_STACK, stack);
        this.setPos(x, y, z);

        if (shooterId != -1) this.shooter = (LivingEntity) this.level().getEntity(shooterId);
        if (targetId != -1) this.target = (LivingEntity) this.level().getEntity(targetId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}