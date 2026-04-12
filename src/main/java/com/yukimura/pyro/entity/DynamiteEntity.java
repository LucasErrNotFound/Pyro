package com.yukimura.pyro.entity;

import com.yukimura.pyro.item.PyroItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class DynamiteEntity extends ThrowableItemProjectile {

    private static final int FUSE_TICKS = 80;
    private int fuseTicks = FUSE_TICKS;
    private boolean inGround = false;

    // Factory constructor — used by EntityType.Builder.of(DynamiteEntity::new, ...)
    public DynamiteEntity(EntityType<? extends DynamiteEntity> type, Level level) {
        super(type, level);
    }

    // Throw constructor — used by IgnitedDynamiteItem via Projectile.spawnProjectileFromRotation
    public DynamiteEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(PyroEntities.DYNAMITE_ENTITY, owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return PyroItems.DYNAMITE;
    }

    private ParticleOptions getParticleParameters() {
        ItemStack itemStack = this.getItem();
        return itemStack.isEmpty() ? ParticleTypes.CLOUD : new ItemParticleOption(ParticleTypes.ITEM, itemStack.getItem());
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 2 || id == 3) {
            ParticleOptions particleEffect = this.getParticleParameters();
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleEffect, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // After super.tick() may apply gravity; re-zero velocity if already stuck
        if (inGround) setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (!level().isClientSide()) {
            if (--fuseTicks <= 0) explodeWithReducedDamage();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!level().isClientSide() && hitResult.getEntity() instanceof Player player) {
            player.hurt(level().damageSources().explosion(this, this.getOwner()), Float.MAX_VALUE);
        }
        explodeWithReducedDamage();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            super.onHit(hitResult);
            return;
        }
        // Block hit: halt the projectile and let the fuse handle the explosion
        if (!inGround) {
            inGround = true;
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
    }

    private void explodeWithReducedDamage() {
        float blastRadius  = 2.3f;  // block destruction radius — keep in sync with visuals
        float damageRadius = 4.0f;  // how far the blast hurts entities (blocks)
        float maxDamage    = 6.0f;  // damage at point-blank (3 hearts)

        if (!level().isClientSide()) {
            DamageSource damageSource = level().damageSources().explosion(this, this.getOwner());
            level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(damageRadius)
            ).forEach(entity -> {
                double distance = entity.distanceTo(this);
                if (distance < damageRadius) {
                    float scaled = (float) (1.0 - distance / damageRadius) * maxDamage;
                    entity.hurt(damageSource, scaled);
                }
            });
        }

        this.discard();
        level().explode(this, getX(), getY(), getZ(), blastRadius, Level.ExplosionInteraction.BLOCK);
    }
}