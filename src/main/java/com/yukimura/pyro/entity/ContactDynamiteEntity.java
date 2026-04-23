package com.yukimura.pyro.entity;

import com.yukimura.pyro.damage.PyroDamageTypes;
import com.yukimura.pyro.item.PyroItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ContactDynamiteEntity extends ThrowableItemProjectile {

    // Factory constructor — used by EntityType.Builder.of(ContactDynamiteEntity::new, ...)
    public ContactDynamiteEntity(EntityType<? extends ContactDynamiteEntity> type, Level level) {
        super(type, level);
    }

    // Throw constructor — used by ContactDynamiteItem via Projectile.spawnProjectileFromRotation
    public ContactDynamiteEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(PyroEntities.CONTACT_DYNAMITE_ENTITY, owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return PyroItems.CONTACT_DYNAMITE;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 2 || id == 3) {
            ItemStack itemStack = this.getItem();
            ParticleOptions particleEffect = itemStack.isEmpty() ? ParticleTypes.CLOUD : new ItemParticleOption(ParticleTypes.ITEM, itemStack.getItem());
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleEffect, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!level().isClientSide() && hitResult.getEntity() instanceof LivingEntity target) {
            target.hurt(PyroDamageTypes.contactDynamiteDirect(level().registryAccess(), this, this.getOwner()), 3.0f);
        }
        explodeOnImpact();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            super.onHit(hitResult);
            return;
        }
        if (hitResult.getType() != HitResult.Type.MISS) {
            explodeOnImpact();
        }
    }

    private void explodeOnImpact() {
        float blastRadius  = 3.0f;
        float damageRadius = 5.0f;
        float maxDamage    = 15.0f;

        if (!level().isClientSide()) {
            DamageSource damageSource = PyroDamageTypes.contactDynamite(level().registryAccess(), this, this.getOwner());
            level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(damageRadius))
                .forEach(entity -> {
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
