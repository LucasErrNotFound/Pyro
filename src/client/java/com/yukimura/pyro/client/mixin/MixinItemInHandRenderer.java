package com.yukimura.pyro.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yukimura.pyro.item.DynamiteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void pyro_blinkIgnitedDynamite(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci) {
        if (!DynamiteItem.isIgnited(itemStack)) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        long elapsed = level.getGameTime() - DynamiteItem.getIgniteTime(itemStack);
        // Blink: hidden for 4 ticks, visible for 4 ticks (TNT-style flash)
        if ((elapsed / 4) % 2 == 0) {
            ci.cancel();
        }
    }
}
