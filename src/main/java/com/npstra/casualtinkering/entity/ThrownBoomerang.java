package com.npstra.casualtinkering.entity;

import com.npstra.casualtinkering.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ScheduledProjectileTaskModifierHook;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Schedule;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

public class ThrownBoomerang extends Projectile implements ToolProjectile {
    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(ThrownBoomerang.class, EntityDataSerializers.ITEM_STACK);

    private static final int MAX_LIFE = 600;
    private static final int RETURN_TICK = 30;
    private static final float WATER_INERTIA = 0.8f;
    private static final float KNOCKBACK_RATIO = 0.5f;

    private ItemStack toolStack = ItemStack.EMPTY;
    private IToolStackView tool;
    private float damage;
    private float knockback;
    private Schedule tasks = Schedule.EMPTY;
    private boolean returning;
    private boolean inheritTraits;
    private double initialSpeed;
    private int life;

    public ThrownBoomerang(EntityType<? extends ThrownBoomerang> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public ThrownBoomerang(Level level, Player shooter, ItemStack stack, IToolStackView tool, float velocity, float inaccuracy, boolean inheritTraits) {
        this(ModEntities.THROWN_BOOMERANG.get(), level);
        this.setOwner(shooter);
        this.toolStack = stack.copy();
        this.tool = tool;
        this.applyToolStats(tool);
        this.initialSpeed = velocity;
        this.inheritTraits = inheritTraits;
        Vec3 pos = shooter.getEyePosition();
        this.setPos(pos.x, pos.y - 0.1, pos.z);
        this.entityData.set(STACK, this.toolStack);
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0f, velocity, inaccuracy);
    }

    private void setStack(ItemStack stack) {
        this.toolStack = stack;
        this.tool = ToolStack.from(stack);
        this.applyToolStats(this.tool);
        this.entityData.set(STACK, stack);
    }

    private void applyToolStats(IToolStackView tool) {
        this.damage = (float) tool.getStats().get(ToolStats.ATTACK_DAMAGE);
        this.knockback = (float) tool.getStats().get(ToolStats.KNOCKBACK_RESISTANCE) * KNOCKBACK_RATIO;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STACK, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getDisplayTool() {
        return this.entityData.get(STACK);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.life++ > MAX_LIFE) {
            this.discard();
            return;
        }
        if (!this.returning && this.life > RETURN_TICK) {
            this.returning = true;
        }
        Entity hitOwner = this.getOwner();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, e -> e != hitOwner);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
        }
        if (this.returning && hitOwner instanceof Player owner && owner.isAlive()) {
            double dx = owner.getX() - this.getX(), dy = owner.getEyeY() - this.getY(), dz = owner.getZ() - this.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 1.2) {
                double speed = Math.max(0.8, this.initialSpeed * 0.6);
                this.setDeltaMovement(new Vec3(dx / len * speed, dy / len * speed, dz / len * speed));
            } else {
                if (!owner.getAbilities().instabuild) {
                    ItemStack toGive = this.toolStack.copy();
                    if (!owner.getInventory().add(toGive)) {
                        owner.drop(toGive, false);
                    }
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
        this.setDeltaMovement(movement.scale(this.isInWater() ? WATER_INERTIA : 0.99f));
        if (!this.tasks.isEmpty() && this.tool != null) {
            ScheduledProjectileTaskModifierHook.checkSchedule(this.tool, this.toolStack, this, null, this.tasks);
        }
    }

    protected void updateRotation() {
        Vec3 movement = this.getDeltaMovement();
        double horizontal = movement.horizontalDistance();
        this.setXRot((float) (Math.atan2(movement.y, horizontal) * 180.0 / Math.PI));
        this.setYRot((float) (Math.atan2(movement.x, movement.z) * 180.0 / Math.PI));
    }

    @Override
    protected void onHit(HitResult hit) {
        if (hit.getType() == HitResult.Type.ENTITY) {
            Entity target = ((net.minecraft.world.phys.EntityHitResult) hit).getEntity();
            if (target == this.getOwner()) return;
            if (this.inheritTraits && this.tool != null && this.getOwner() instanceof Player player) {
                ItemStack originalMain = player.getMainHandItem();
                try {
                    player.setItemInHand(InteractionHand.MAIN_HAND, this.toolStack);
                    ToolAttackContext context = ToolAttackContext.attacker(player)
                            .target(target)
                            .hand(InteractionHand.MAIN_HAND)
                            .baseDamage(this.damage)
                            .cooldown(1.0f)
                            .projectile(this)
                            .build();
                    ToolAttackUtil.performAttack(this.tool, context);
                } finally {
                    player.setItemInHand(InteractionHand.MAIN_HAND, originalMain);
                }
            } else {
                target.hurt(this.damageSources().thrown(this, this.getOwner()), this.damage);
            }
            if (this.knockback > 0 && target instanceof LivingEntity living) {
                Vec3 motion = this.getDeltaMovement().normalize();
                living.knockback(this.knockback, -motion.x, -motion.z);
            }
        }
        this.returning = true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!this.toolStack.isEmpty()) tag.put("stack", this.toolStack.save(new CompoundTag()));
        tag.putFloat("damage", this.damage);
        tag.putFloat("knockback", this.knockback);
        tag.putBoolean("returning", this.returning);
        tag.putBoolean("inheritTraits", this.inheritTraits);
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
        }
        if (tag.contains("damage")) this.damage = tag.getFloat("damage");
        if (tag.contains("knockback")) this.knockback = tag.getFloat("knockback");
        this.returning = tag.getBoolean("returning");
        this.inheritTraits = tag.getBoolean("inheritTraits");
        this.initialSpeed = tag.getDouble("initialSpeed");
        this.life = tag.getInt("life");
        if (tag.contains("tasks")) {
            this.tasks = Schedule.deserialize(tag.getList("tasks", 10));
        }
    }
}
