package com.yukimura.pyro.mixin;

import com.yukimura.pyro.damage.PyroDamageTypes;
import com.yukimura.pyro.item.DynamiteItem;
import com.yukimura.pyro.item.PyroItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class IgnitedDropMixin {

    @Unique
    private boolean pyro_clearedRemainingFromDropper = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickIgnitedDynamite(CallbackInfo callbackInfo) {
        ItemEntity self = (ItemEntity)(Object)this;

        ItemStack stack = self.getItem();
        if (!stack.is(PyroItems.DYNAMITE)) return;
        long igniteTime = DynamiteItem.getIgniteTimeIfPresent(stack);
        if (igniteTime == Long.MIN_VALUE) return;

        if (self.level().isClientSide()) {
            pyro_spawnFuseParticles(self);
            return;
        }

        ServerLevel serverLevel = (ServerLevel) self.level();

        if (!pyro_clearedRemainingFromDropper) {
            pyro_clearedRemainingFromDropper = true;
            serverLevel.getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(4.0))
                .forEach(player -> {
                    int inventorySize = player.getInventory().getContainerSize();
                    for (int slotIndex = 0; slotIndex < inventorySize; slotIndex++) {
                        clearIfMatchingIgnited(player.getInventory().getItem(slotIndex), igniteTime);
                    }
                    clearIfMatchingIgnited(player.getOffhandItem(), igniteTime);
                });
        }

        if (serverLevel.getGameTime() - igniteTime < DynamiteItem.FUSE_TICKS) return;

        int stackCount = stack.getCount();
        float damageRadius = 5.0f + (stackCount - 1) * 0.75f;
        float maxDamage = 15.0f * stackCount;

        DamageSource damageSource = PyroDamageTypes.dynamite(serverLevel.registryAccess(), self, null);
        serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            self.getBoundingBox().inflate(damageRadius)
        ).forEach(livingEntity -> {
            double distance = livingEntity.distanceTo(self);
            if (distance < damageRadius) {
                float scaled = (float)(1.0 - distance / damageRadius) * maxDamage;
                livingEntity.hurt(damageSource, scaled);
            }
        });

        double spread = (stackCount - 1) * 0.75;
        for (int i = 0; i < stackCount; i++) {
            double offsetX = i == 0 ? 0.0 : (serverLevel.getRandom().nextDouble() * 2.0 - 1.0) * spread;
            double offsetY = i == 0 ? 0.0 : (serverLevel.getRandom().nextDouble() * 2.0 - 1.0) * spread * 0.4;
            double offsetZ = i == 0 ? 0.0 : (serverLevel.getRandom().nextDouble() * 2.0 - 1.0) * spread;
            serverLevel.explode(self,
                self.getX() + offsetX, self.getY() + offsetY, self.getZ() + offsetZ,
                4.0f, Level.ExplosionInteraction.BLOCK);
        }

        self.discard();
    }

    @Unique
    private static void pyro_spawnFuseParticles(ItemEntity entity) {
        Level level = entity.level();
        level.addParticle(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.6, entity.getZ(), 0.0, 0.04, 0.0);
        if (level.getRandom().nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, entity.getX(), entity.getY() + 0.6, entity.getZ(), 0.0, 0.02, 0.0);
        }
    }

    @Unique
    private static void clearIfMatchingIgnited(ItemStack inventoryStack, long igniteTime) {
        if (!inventoryStack.is(PyroItems.DYNAMITE)) return;
        if (DynamiteItem.getIgniteTimeIfPresent(inventoryStack) != igniteTime) return;
        DynamiteItem.clearIgnited(inventoryStack);
    }
}
