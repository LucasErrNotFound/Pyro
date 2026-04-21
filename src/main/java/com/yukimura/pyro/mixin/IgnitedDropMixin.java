package com.yukimura.pyro.mixin;

import com.yukimura.pyro.damage.PyroDamageTypes;
import com.yukimura.pyro.item.DynamiteItem;
import com.yukimura.pyro.item.PyroItems;
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
        if (self.level().isClientSide()) return;

        ItemStack stack = self.getItem();
        if (!stack.is(PyroItems.DYNAMITE) || !DynamiteItem.isIgnited(stack)) return;

        ServerLevel serverLevel = (ServerLevel) self.level();

        if (!pyro_clearedRemainingFromDropper) {
            pyro_clearedRemainingFromDropper = true;
            long droppedIgniteTime = DynamiteItem.getIgniteTime(stack);
            serverLevel.getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(4.0))
                .forEach(player -> {
                    int inventorySize = player.getInventory().getContainerSize();
                    for (int slotIndex = 0; slotIndex < inventorySize; slotIndex++) {
                        clearIfMatchingIgnited(player.getInventory().getItem(slotIndex), droppedIgniteTime);
                    }
                    clearIfMatchingIgnited(player.getOffhandItem(), droppedIgniteTime);
                });
        }

        if (!DynamiteItem.isFuseExpired(stack, serverLevel.getGameTime())) return;

        int stackCount = stack.getCount();
        float damageRadius = 5.0f * stackCount;
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
    private static void clearIfMatchingIgnited(ItemStack inventoryStack, long igniteTime) {
        if (inventoryStack.is(PyroItems.DYNAMITE)
                && DynamiteItem.isIgnited(inventoryStack)
                && DynamiteItem.getIgniteTime(inventoryStack) == igniteTime) {
            DynamiteItem.clearIgnited(inventoryStack);
        }
    }
}
