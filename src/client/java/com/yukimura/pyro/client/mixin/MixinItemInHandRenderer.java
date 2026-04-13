package com.yukimura.pyro.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yukimura.pyro.item.DynamiteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    // Captures the item currently being rendered by renderArmWithItem so that
    // the @ModifyArg inside renderItem can read it without needing @Local.
    @Unique
    private static ItemStack pyro_currentHeldItem = null;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void pyro_captureHeldItem(
            AbstractClientPlayer player, float frameInterp, float xRot,
            InteractionHand hand, float attack, ItemStack itemStack,
            float inverseArmHeight, PoseStack poseStack,
            SubmitNodeCollector snc, int lightCoords, CallbackInfo ci) {
        pyro_currentHeldItem = itemStack;
    }

    @Inject(method = "renderArmWithItem", at = @At("TAIL"))
    private void pyro_clearHeldItem(
            AbstractClientPlayer player, float frameInterp, float xRot,
            InteractionHand hand, float attack, ItemStack itemStack,
            float inverseArmHeight, PoseStack poseStack,
            SubmitNodeCollector snc, int lightCoords, CallbackInfo ci) {
        pyro_currentHeldItem = null;
    }

    // Intercepts the overlayCoords argument (index 3) of ItemStackRenderState.submit()
    // called inside ItemInHandRenderer.renderItem(), and replaces it with the white
    // flash overlay when the held item is ignited dynamite in the "flash on" phase.
    @ModifyArg(
        method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"),
        index = 3
    )
    private int pyro_flashDynamiteOverlay(int overlay) {
        ItemStack stack = pyro_currentHeldItem;
        if (stack == null || !DynamiteItem.isIgnited(stack)) return overlay;
        var level = Minecraft.getInstance().level;
        if (level == null) return overlay;
        long elapsed = level.getGameTime() - DynamiteItem.getIgniteTime(stack);
        return (elapsed / 4) % 2 == 0
                ? OverlayTexture.pack(15, OverlayTexture.WHITE_OVERLAY_V)
                : overlay;
    }
}
