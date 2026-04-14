package com.yukimura.pyro.client.light;

import com.yukimura.pyro.entity.PyroEntities;
import com.yukimura.pyro.item.DynamiteItem;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PyroLightsInitializer implements DynamicLightsInitializer {

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        // Thrown ignited dynamite glows in flight and on the ground.
        // DynamiteEntity is only ever spawned from an already-ignited item, so always return 15.
        context.entityLightSourceManager().onRegisterEvent().register(
            Identifier.fromNamespaceAndPath("pyro", "dynamite_entity"),
            regContext -> regContext.register(PyroEntities.DYNAMITE_ENTITY, 15)
        );

        // Player holding ignited dynamite in main hand glows.
        // The custom EntityLuminance checks the held item each tick for the ignited state.
        context.entityLightSourceManager().onRegisterEvent().register(
            Identifier.fromNamespaceAndPath("pyro", "player_holding_lit_dynamite"),
            regContext -> regContext.register(
                EntityType.PLAYER,
                new IgnitedDynamiteHolderLuminance()
            )
        );
    }

    /**
     * Emits light level 15 when a Player holds an ignited dynamite in their main hand.
     * Returns 0 for all other entities and for unignited dynamite.
     */
    private static final class IgnitedDynamiteHolderLuminance implements EntityLuminance {

        @Override
        public Type type() {
            // type() is only used for JSON (de)serialization of data-pack light sources.
            // For programmatic event-based registration, this is never called at runtime.
            return EntityLuminance.Type.VALUE;
        }

        @Override
        public int getLuminance(ItemLightSourceManager itemLightSourceManager, Entity entity) {
            if (!(entity instanceof Player player)) return 0;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof DynamiteItem)) return 0;
            if (!DynamiteItem.isIgnited(held)) return 0;
            return 15;
        }
    }
}
