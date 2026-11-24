package com.npstra.casualtinkering.entity;

import com.npstra.casualtinkering.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public class GunbladeBullet extends Projectile {
    private static final int MAX_LIFE = 20;
    private static final float DRAG = 0.99F;

    private ItemStack toolStack = ItemStack.EMPTY;
    private IToolStackView tool;
    private float damage;
    private int life;

    public GunbladeBullet(EntityType<? extends GunbladeBullet> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public GunbladeBullet(Level level, Player shooter, ItemStack stack, IToolStackView tool, float damage, float speed) {
        this(ModEntities.GUNBLADE_BULLET.get(), level);
        this.setOwner(shooter);
        this.toolStack = stack.copy();
        this.tool = tool;
        this.damage = damage;
        Vec3 look = shooter.getLookAngle();
        Vec3 start = shooter.getEyePosition().add(look.scale(0.6));
        this.setPos(start.x, start.y, start.z);
        this.setDeltaMovement(look.scale(speed));
    }

    private void setStack(ItemStack stack) {
        this.toolStack = stack;
        this.tool = ToolStack.from(stack);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            return;
        }
        if (this.life++ > MAX_LIFE) {
            this.discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, e -> e != this.getOwner());
        if (hit.getType() != HitResult.Type.MISS) {
            if (hit.getType() == HitResult.Type.ENTITY) this.hitEntity(((EntityHitResult) hit).getEntity());
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6F, 1.25F);
            this.discard();
            return;
        }
        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        this.setDeltaMovement(movement.scale(DRAG));
    }

    private void hitEntity(Entity target) {
        if (!(this.getOwner() instanceof Player player) || this.toolStack.isEmpty()) {
            target.hurt(this.damageSources().thrown(this, this.getOwner()), this.damage);
            return;
        }
        ItemStack original = player.getMainHandItem();
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, this.toolStack);
            ToolAttackContext context = ToolAttackContext.attacker(player)
                    .target(target)
                    .hand(InteractionHand.MAIN_HAND)
                    .baseDamage(this.damage)
                    .cooldown(1.0F)
                    .projectile(this)
                    .build();
            ToolAttackUtil.performAttack(this.tool, context);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, original);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!this.toolStack.isEmpty()) tag.put("stack", this.toolStack.save(new CompoundTag()));
        tag.putFloat("damage", this.damage);
        tag.putInt("life", this.life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("stack")) this.setStack(ItemStack.of(tag.getCompound("stack")));
        this.damage = tag.getFloat("damage");
        this.life = tag.getInt("life");
    }
}
