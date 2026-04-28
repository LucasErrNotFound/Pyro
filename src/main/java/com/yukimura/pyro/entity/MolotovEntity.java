package com.yukimura.pyro.entity;

import com.yukimura.pyro.damage.PyroDamageTypes;
import com.yukimura.pyro.item.MolotovItem;
import com.yukimura.pyro.item.PyroItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class MolotovEntity extends ThrowableItemProjectile {

    private static final int FUSE_TICKS = 600;
    private static final byte EVENT_IMPACT = 12;
    private int fuseTicks = FUSE_TICKS;

    private boolean hasImpacted = false;
    private int fireSpreadTick = 0;
    private int fireSpreadInitialRadius = 4;
    private int fireSpreadMaxRadius = 12;

    private boolean clientHasImpacted = false;
    private int clientSpreadTick = 0;

    public MolotovEntity(EntityType<? extends MolotovEntity> type, Level level) {
        super(type, level);
    }

    public MolotovEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(PyroEntities.MOLOTOV_ENTITY, owner, level, stack);
        if (MolotovItem.isIgnited(stack)) {
            this.fuseTicks = MolotovItem.getRemainingTicks(stack, level.getGameTime());
        }
    }

    public MolotovEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(PyroEntities.MOLOTOV_ENTITY, x, y, z, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return PyroItems.MOLOTOV;
    }

    @Override
    public void tick() {
        super.tick();

        if (hasImpacted) {
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide()) {
                fireSpreadTick++;
                int currentRadius = Math.min(fireSpreadInitialRadius + fireSpreadTick, fireSpreadMaxRadius);
                spreadFire((ServerLevel) level(), BlockPos.containing(getX(), getY(), getZ()), currentRadius);
                if (currentRadius >= fireSpreadMaxRadius) {
                    discard();
                }
            } else {
                if (clientHasImpacted) {
                    clientSpreadTick++;
                    spawnSpreadParticles();
                }
            }
            return;
        }

        if (level().isClientSide()) {
            if (!isInWater()) spawnFuseParticles();
        } else {
            if (isInWater()) {
                ServerLevel serverLevel = (ServerLevel) level();
                serverLevel.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                serverLevel.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 8, 0.2, 0.0, 0.2, 0.1);
                ItemEntity droppedItem = new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(PyroItems.MOLOTOV));
                level().addFreshEntity(droppedItem);
                discard();
                return;
            }
            if (--fuseTicks <= 0) {
                triggerImpact();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!level().isClientSide() && hitResult.getEntity() instanceof LivingEntity target) {
            target.hurt(PyroDamageTypes.molotovDirect(level().registryAccess(), this, this.getOwner()), 3.0f);
            target.igniteForSeconds(8.0F);
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        triggerImpact();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_IMPACT) {
            clientHasImpacted = true;
            clientSpreadTick = 0;
            spawnImpactParticles();
        } else {
            super.handleEntityEvent(id);
        }
    }

    public void triggerImpact() {
        if (hasImpacted) return;
        hasImpacted = true;
        setDeltaMovement(Vec3.ZERO);

        if (!level().isClientSide()) {
            fireSpreadInitialRadius = 3 + level().getRandom().nextInt(3); // 3–5 blocks initial
            fireSpreadMaxRadius = 5 + level().getRandom().nextInt(4);     // 5–8 blocks max
            spreadFire((ServerLevel) level(), BlockPos.containing(getX(), getY(), getZ()), fireSpreadInitialRadius);

            level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL,
                1.0F, level().getRandom().nextFloat() * 0.1F + 0.9F);
            level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL,
                1.0F, level().getRandom().nextFloat() * 0.1F + 0.9F);

            level().broadcastEntityEvent(this, EVENT_IMPACT);
        }
    }

    private void spawnFuseParticles() {
        Level level = level();
        level.addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 0.0, 0.04, 0.0);
        if (level.getRandom().nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.3, getZ(), 0.0, 0.02, 0.0);
        }
    }

    private void spawnImpactParticles() {
        Level level = level();
        RandomSource random = level.getRandom();
        Vec3 origin = new Vec3(getX(), getY() + 0.2, getZ());

        for (int i = 0; i < 32; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.3 + random.nextDouble() * 0.4;
            double velocityX = Math.cos(angle) * speed;
            double velocityY = 0.1 + random.nextDouble() * 0.2;
            double velocityZ = Math.sin(angle) * speed;
            Vec3 targetPosition = origin.add(new Vec3(velocityX, velocityY, velocityZ).normalize().scale(3.0));
            HitResult lineOfSightResult = level.clip(new ClipContext(
                origin, targetPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()
            ));
            if (lineOfSightResult.getType() == HitResult.Type.BLOCK) continue;
            level.addParticle(ParticleTypes.FLAME, origin.x, origin.y, origin.z, velocityX, velocityY, velocityZ);
        }

        for (int i = 0; i < 12; i++) {
            level.addParticle(ParticleTypes.LAVA, origin.x, origin.y, origin.z, 0.0, 0.0, 0.0);
        }
    }

    private void spawnSpreadParticles() {
        Level level = level();
        RandomSource random = level.getRandom();
        double expansionSpeed = 0.1 + 0.04 * clientSpreadTick;
        Vec3 origin = new Vec3(getX(), getY() + 0.2, getZ());

        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = expansionSpeed + random.nextDouble() * 0.15;
            double velocityX = Math.cos(angle) * speed;
            double velocityY = 0.05 + random.nextDouble() * 0.1;
            double velocityZ = Math.sin(angle) * speed;
            Vec3 targetPosition = origin.add(new Vec3(velocityX, velocityY, velocityZ).normalize().scale(3.0));
            HitResult lineOfSightResult = level.clip(new ClipContext(
                origin, targetPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()
            ));
            if (lineOfSightResult.getType() == HitResult.Type.BLOCK) continue;
            level.addParticle(ParticleTypes.FLAME, origin.x, origin.y, origin.z, velocityX, velocityY, velocityZ);
        }
    }

    public static void spreadFire(ServerLevel level, BlockPos center, int radius) {
        RandomSource random = level.getRandom();
        int attempts = radius * radius * 2;

        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;
            int xOffset = (int)(Math.cos(angle) * distance);
            int zOffset = (int)(Math.sin(angle) * distance);

            for (int yOffset = 2; yOffset >= -4; yOffset--) {
                BlockPos firePosition = center.offset(xOffset, yOffset, zOffset);
                BlockState stateBelow = level.getBlockState(firePosition.below());
                if (level.isEmptyBlock(firePosition) && !stateBelow.isAir()) {
                    HitResult lineOfSightResult = level.clip(new ClipContext(
                        Vec3.atCenterOf(center),
                        Vec3.atCenterOf(firePosition),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        CollisionContext.empty()
                    ));
                    if (lineOfSightResult.getType() == HitResult.Type.MISS) {
                        level.setBlock(firePosition, BaseFireBlock.getState(level, firePosition), 3);
                    }
                    break;
                }
            }
        }
    }
}
