package com.yukimura.pyro.mixin;

import com.yukimura.pyro.entity.MolotovEntity;
import com.yukimura.pyro.item.MolotovItem;
import com.yukimura.pyro.item.PyroItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class IgnitedMolotovDropMixin {

    @Unique
    private boolean pyro_molotovCrackRollDone = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickIgnitedMolotov(CallbackInfo callbackInfo) {
        ItemEntity self = (ItemEntity)(Object)this;

        ItemStack stack = self.getItem();
        if (!stack.is(PyroItems.MOLOTOV)) return;
        long igniteTime = MolotovItem.getIgniteTimeIfPresent(stack);
        if (igniteTime == Long.MIN_VALUE) return;

        if (self.level().isClientSide()) {
            if (!self.isInWater()) pyro_spawnFuseParticles(self);
            return;
        }

        ServerLevel serverLevel = (ServerLevel) self.level();

        if (!pyro_molotovCrackRollDone && self.onGround()) {
            pyro_molotovCrackRollDone = true;
            if (serverLevel.getRandom().nextBoolean()) {
                pyro_triggerExplosion(self, serverLevel, stack);
                return;
            }
        }

        if (self.isInWater()) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE, self.getX(), self.getY(), self.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            serverLevel.sendParticles(ParticleTypes.SPLASH, self.getX(), self.getY(), self.getZ(), 6, 0.2, 0.0, 0.2, 0.1);
            ItemStack unignited = stack.copy();
            MolotovItem.clearIgnited(unignited);
            self.setItem(unignited);
            return;
        }

        long elapsed = serverLevel.getGameTime() - igniteTime;
        if (elapsed > 0 && elapsed % 80 == 0) {
            serverLevel.playSound(null, self.getX(), self.getY(), self.getZ(),
                SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.0F);
        }
        if (elapsed < MolotovItem.FUSE_TICKS) return;

        pyro_triggerExplosion(self, serverLevel, stack);
    }

    @Unique
    private static void pyro_triggerExplosion(ItemEntity itemEntity, ServerLevel serverLevel, ItemStack stack) {
        MolotovEntity ghostEntity = new MolotovEntity(
            serverLevel,
            itemEntity.getX(),
            itemEntity.getY(),
            itemEntity.getZ(),
            stack
        );
        serverLevel.addFreshEntity(ghostEntity);
        ghostEntity.triggerImpact();
        itemEntity.discard();
    }

    @Unique
    private static void pyro_spawnFuseParticles(ItemEntity entity) {
        Level level = entity.level();
        level.addParticle(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.6, entity.getZ(), 0.0, 0.04, 0.0);
        if (level.getRandom().nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, entity.getX(), entity.getY() + 0.6, entity.getZ(), 0.0, 0.02, 0.0);
        }
    }
}
