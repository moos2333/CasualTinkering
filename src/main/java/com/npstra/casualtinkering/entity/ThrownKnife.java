package com.npstra.casualtinkering.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithKnockback;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithPower;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ScheduledProjectileTaskModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Schedule;
import slimeknights.tconstruct.tools.entity.ToolProjectile;
import com.npstra.casualtinkering.init.ModEntities;

public class ThrownKnife extends Projectile implements ToolProjectile, ProjectileWithPower, ProjectileWithKnockback {
    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(ThrownKnife.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> WATER_INERTIA = SynchedEntityData.defineId(ThrownKnife.class, EntityDataSerializers.FLOAT);

    private ItemStack stack = ItemStack.EMPTY;
    private IToolStackView tool;
    private float power = 4.0f;
    private float knockback = 0.0f;
    private Schedule tasks = Schedule.EMPTY;

    public ThrownKnife(EntityType<? extends ThrownKnife> type, Level level) {
        super(type, level);
    }

    public ThrownKnife(Level level, LivingEntity shooter) {
        this(ModEntities.THROWN_KNIFE.get(), level);
        this.setOwner(shooter);
        Vec3 pos = shooter.getEyePosition();
        this.setPos(pos.x, pos.y - 0.1, pos.z);
    }

    private void setStack(ItemStack stack) {
        this.stack = stack;
        this.entityData.set(STACK, stack);
    }

    private IToolStackView getTool() {
        if (tool == null) tool = ToolStack.from(stack);
        return tool;
    }

    public IToolStackView onCreate(ItemStack stack, LivingEntity shooter) {
        this.setStack(stack.copyWithCount(1));
        IToolStackView tool = getTool();
        EntityModifierCapability.getCapability(this).addModifiers(tool.getModifiers());
        this.power = ConditionalStatModifierHook.getModifiedStat(tool, shooter, ToolStats.PROJECTILE_DAMAGE);
        this.entityData.set(WATER_INERTIA, ConditionalStatModifierHook.getModifiedStat(tool, shooter, ToolStats.WATER_INERTIA));
        return tool;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        if (!stack.isEmpty()) {
            IToolStackView tool = getTool();
            LivingEntity shooter = ModifierUtil.asLiving(getOwner());
            inaccuracy *= ModifierUtil.getInaccuracy(tool, shooter);
            super.shoot(x, y, z, velocity, inaccuracy);
            ModDataNBT arrowData = PersistentDataCapability.getOrWarn(this);
            for (ModifierEntry entry : tool.getModifiers()) {
                entry.getHook(ModifierHooks.PROJECTILE_SHOT).onProjectileShoot(tool, entry, shooter, stack, this, null, arrowData, true);
            }
            this.tasks = ScheduledProjectileTaskModifierHook.createSchedule(tool, stack, this, null, arrowData);
        } else {
            super.shoot(x, y, z, velocity, inaccuracy);
        }
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !level().isClientSide) {
            this.onHit(hit);
        }
        Vec3 movement = this.getDeltaMovement();
        double x = this.getX() + movement.x;
        double y = this.getY() + movement.y;
        double z = this.getZ() + movement.z;
        this.updateRotation();
        float reduction = this.isInWater() ? this.entityData.get(WATER_INERTIA) : 0.99f;
        this.setDeltaMovement(movement.scale(reduction));
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.03, 0));
        }
        this.setPos(x, y, z);
        if (!tasks.isEmpty() && !stack.isEmpty()) {
            ScheduledProjectileTaskModifierHook.checkSchedule(getTool(), stack, this, null, tasks);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        DamageSource source = damageSources().thrown(this, getOwner());
        boolean hit = target.hurt(source, power);
        if (hit && knockback > 0 && target instanceof LivingEntity living) {
            double resistance = Math.max(0, 1 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 motion = this.getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback * 0.6 * resistance);
            if (motion.lengthSqr() > 0) target.push(motion.x, 0.1, motion.z);
        }
        if (!level().isClientSide) {
            if (!hit && !this.isRemoved()) this.spawnAtLocation(stack.copy());
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && !this.isRemoved()) {
            this.spawnAtLocation(stack.copy());
            this.discard();
        }
    }

    @Override
    public void setPower(float power) { this.power = power; }
    @Override
    public void addKnockback(float amount) { this.knockback += amount; }

    @Override
    public float getPower() { return power; }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STACK, ItemStack.EMPTY);
        this.entityData.define(WATER_INERTIA, 0.8f);
    }

    @Override
    public ItemStack getDisplayTool() { return this.entityData.get(STACK); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("stack", stack.save(new CompoundTag()));
        tag.putFloat("water_inertia", this.entityData.get(WATER_INERTIA));
        if (!tasks.isEmpty()) tag.put("tasks", tasks.serialize());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("stack")) setStack(ItemStack.of(tag.getCompound("stack")));
        this.entityData.set(WATER_INERTIA, tag.getFloat("water_inertia"));
        if (tag.contains("tasks")) this.tasks = Schedule.deserialize(tag.getList("tasks", 10));
    }
}