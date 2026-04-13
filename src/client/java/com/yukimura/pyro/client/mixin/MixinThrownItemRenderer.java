package com.yukimura.pyro.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yukimura.pyro.client.IPyroThrownItemRenderState;
import com.yukimura.pyro.item.DynamiteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownItemRenderer.class)
public class MixinThrownItemRenderer {

    // Holds the igniteTime for the thrown entity currently being submitted.
    // Long.MIN_VALUE means "not ignited dynamite".
    // Safe as a static because entity rendering is single-threaded.
    @Unique
    private static long pyro_currentIgniteTime = Long.MIN_VALUE;

    // Called when the render state is being extracted from the entity.
    // The entity is available here, so we read igniteTime and store it on the
    // augmented render state for later use during submit().
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/ThrownItemRenderState;F)V",
        at = @At("TAIL")
    )
    private void pyro_storeIgniteTime(Entity entity, ThrownItemRenderState state, float partialTicks, CallbackInfo ci) {
        long igniteTime = Long.MIN_VALUE;
        if (entity instanceof ItemSupplier supplier) {
            ItemStack stack = supplier.getItem();
            if (DynamiteItem.isIgnited(stack)) {
                igniteTime = DynamiteItem.getIgniteTime(stack);
            }
        }
        ((IPyroThrownItemRenderState) state).pyro_setIgniteTime(igniteTime);
    }

    // Called at the start of submit() where the entity is no longer available,
    // but the render state (with our injected igniteTime) is. Copy it to the
    // static field so the @ModifyArg below can read it without @Local.
    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownItemRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void pyro_loadIgniteTime(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector snc, CameraRenderState camera, CallbackInfo ci) {
        pyro_currentIgniteTime = ((IPyroThrownItemRenderState) state).pyro_getIgniteTime();
    }

    // Intercepts the overlayCoords argument (index 3) of ItemStackRenderState.submit()
    // called inside ThrownItemRenderer.submit(), and replaces it with the white flash
    // overlay when the thrown item is ignited dynamite in the "flash on" phase.
    @ModifyArg(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownItemRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"),
        index = 3
    )
    private int pyro_flashThrownDynamiteOverlay(int overlay) {
        long igniteTime = pyro_currentIgniteTime;
        if (igniteTime == Long.MIN_VALUE) return overlay;
        var level = Minecraft.getInstance().level;
        if (level == null) return overlay;
        long elapsed = level.getGameTime() - igniteTime;
        return (elapsed / 4) % 2 == 0
                ? OverlayTexture.pack(15, OverlayTexture.WHITE_OVERLAY_V)
                : overlay;
    }
}
