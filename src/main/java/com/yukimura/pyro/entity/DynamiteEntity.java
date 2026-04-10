package com.yukimura.pyro.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class DynamiteEntity extends ThrowableProjectile {

    public DynamiteEntity(Level level, LivingEntity owner) {
        super(EntityType.SNOWBALL, level);
    }

    public DynamiteEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(PyroEntities.DYNAMITE_ENTITY, level);
    }

    public DynamiteEntity(EntityType<? extends ThrowableProjectile> type, double x, double y, double z, Level level) {
        super(PyroEntities.DYNAMITE_ENTITY, x, y, z, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {

    }

    private ParticleOptions getParticleParameters() {
        ItemStack itemStack = this.getWeaponItem();
        return itemStack.isEmpty() ? ParticleTypes.CLOUD : new ItemParticleOption(ParticleTypes.ITEM, itemStack.getItem());
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 2 || id == 3) {
            ParticleOptions particleEffect = this.getParticleParameters();

            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleEffect, this.getX(), this.getY(), this.getZ(), 0.0 , 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        this.discard();
        this.level().explode(this, getX(), getY(), getZ(), 1.0f, Level.ExplosionInteraction.TNT);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        this.discard();
        this.level().explode(this, getX(), getY(), getZ(), 1.0f, Level.ExplosionInteraction.TNT);
    }

    @Override
    public @Nullable ItemStack getWeaponItem() {
        return super.getWeaponItem();
    }
}