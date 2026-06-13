package com.npstra.casualtinkering.entity;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ScheduledProjectileTaskModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Schedule;
import slimeknights.tconstruct.tools.entity.ToolProjectile;
import com.npstra.casualtinkering.init.ModEntities;

public class ThrownBoomerang extends Projectile implements ToolProjectile, IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(ThrownBoomerang.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> WATER_INERTIA = SynchedEntityData.defineId(ThrownBoomerang.class, EntityDataSerializers.FLOAT);

    private ItemStack toolStack = ItemStack.EMPTY;
    private IToolStackView tool;
    private float damage;
    private float knockback;
    private Schedule tasks = Schedule.EMPTY;
    private boolean returning = false;
    private double initialSpeed = 0.0;
    private int life = 0;
    private static final int MAX_LIFE = 30;

    public ThrownBoomerang(EntityType<? extends ThrownBoomerang> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public ThrownBoomerang(Level level, Player shooter, ItemStack stack, IToolStackView tool, float velocity, float inaccuracy) {
        super(ModEntities.THROWN_BOOMERANG.get(), level);
        this.setOwner(shooter);
        this.toolStack = stack.copy();
        this.tool = tool;
        this.damage = (float) tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        this.knockback = (float) tool.getStats().get(ToolStats.KNOCKBACK_RESISTANCE) * 0.5f;
        this.initialSpeed = velocity;
        Vec3 pos = shooter.getEyePosition();
        this.setPos(pos.x, pos.y - 0.1, pos.z);
        this.entityData.set(STACK, toolStack);
        this.entityData.set(WATER_INERTIA, 0.8f);
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0f, velocity, inaccuracy);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    private void setStack(ItemStack stack) {
        this.toolStack = stack;
        this.entityData.set(STACK, stack);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STACK, ItemStack.EMPTY);
        this.entityData.define(WATER_INERTIA, 0.8f);
    }

    @Override
    public ItemStack getDisplayTool() {
        return this.entityData.get(STACK);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            super.tick();
            return;
        }
        if (this.life++ > 600) {
            this.discard();
            return;
        }
        if (!this.returning && this.life > MAX_LIFE) {
            this.returning = true;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, e -> e != this.getOwner());
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
        }
        if (this.returning && this.getOwner() instanceof Player owner && owner.isAlive()) {
            Vec3 target = owner.getEyePosition();
            Vec3 toTarget = target.subtract(this.position());
            double dist = toTarget.length();
            if (dist > 1.2) {
                double speed = Math.max(0.8, this.initialSpeed * 0.6);
                this.setDeltaMovement(toTarget.normalize().scale(speed));
            } else {
                ItemStack toGive = this.toolStack.copy();
                if (owner.getInventory().add(toGive)) {
                    toGive.shrink(1);
                } else {
                    owner.drop(toGive, false);
                }
                this.discard();
                return;
            }
        } else if (this.returning) {
            this.spawnAtLocation(this.toolStack);
            this.discard();
            return;
        }
        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        this.updateRotation();
        float reduction = this.isInWater() ? this.entityData.get(WATER_INERTIA) : 0.99f;
        this.setDeltaMovement(movement.scale(reduction));
        if (!this.tasks.isEmpty()) {
            ScheduledProjectileTaskModifierHook.checkSchedule(this.tool, this.toolStack, this, null, this.tasks);
        }
    }

    protected void updateRotation() {
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.horizontalDistance();
        this.setXRot((float) (Math.atan2(vec3.y, d0) * 180.0 / Math.PI));
        this.setYRot((float) (Math.atan2(vec3.x, vec3.z) * 180.0 / Math.PI));
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    @Override
    protected void onHit(HitResult hit) {
        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            Entity target = entityHit.getEntity();
            if (target == this.getOwner()) return;
            DamageSource source = this.damageSources().thrown(this, this.getOwner());
            boolean hurt = target.hurt(source, this.damage);
            if (hurt && this.knockback > 0 && target instanceof LivingEntity living) {
                Vec3 motion = this.getDeltaMovement().normalize();
                living.knockback(this.knockback, -motion.x, -motion.z);
            }
        }
        this.returning = true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("stack", this.toolStack.save(new CompoundTag()));
        tag.putFloat("water_inertia", this.entityData.get(WATER_INERTIA));
        tag.putBoolean("returning", this.returning);
        tag.putDouble("initialSpeed", this.initialSpeed);
        tag.putInt("life", this.life);
        if (!this.tasks.isEmpty()) {
            tag.put("tasks", this.tasks.serialize());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("stack")) {
            this.setStack(ItemStack.of(tag.getCompound("stack")));
            this.tool = ToolStack.from(this.toolStack);
        }
        this.entityData.set(WATER_INERTIA, tag.getFloat("water_inertia"));
        this.returning = tag.getBoolean("returning");
        this.initialSpeed = tag.getDouble("initialSpeed");
        this.life = tag.getInt("life");
        if (tag.contains("tasks")) {
            this.tasks = Schedule.deserialize(tag.getList("tasks", 10));
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeItem(this.toolStack);
        buffer.writeFloat(this.entityData.get(WATER_INERTIA));
        buffer.writeBoolean(this.returning);
        buffer.writeDouble(this.initialSpeed);
        buffer.writeInt(this.life);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.setStack(additionalData.readItem());
        this.entityData.set(WATER_INERTIA, additionalData.readFloat());
        this.returning = additionalData.readBoolean();
        this.initialSpeed = additionalData.readDouble();
        this.life = additionalData.readInt();
        this.tool = ToolStack.from(this.toolStack);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}