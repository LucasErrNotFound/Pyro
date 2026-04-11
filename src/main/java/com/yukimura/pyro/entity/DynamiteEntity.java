package com.yukimura.pyro.entity;

import com.yukimura.pyro.item.PyroItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class DynamiteEntity extends ThrowableItemProjectile {

    // Factory constructor — used by EntityType.Builder.of(DynamiteEntity::new, ...)
    public DynamiteEntity(EntityType<? extends DynamiteEntity> type, Level level) {
        super(type, level);
    }

    // Throw constructor — used by DynamiteItem via Projectile.spawnProjectileFromRotation
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
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        this.discard();
        this.level().explode(this, getX(), getY(), getZ(), 2.3f, Level.ExplosionInteraction.TNT);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        this.discard();
        this.level().explode(this, getX(), getY(), getZ(), 2.3f, Level.ExplosionInteraction.TNT);
    }
}