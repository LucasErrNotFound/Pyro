package com.yukimura.pyro.client.light;

import com.yukimura.pyro.entity.PyroEntities;
import com.yukimura.pyro.item.DynamiteItem;
import net.minecraft.world.entity.item.ItemEntity;
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

        // Dropped ignited dynamite (ItemEntity on the ground) glows.
        context.entityLightSourceManager().onRegisterEvent().register(
            Identifier.fromNamespaceAndPath("pyro", "dropped_ignited_dynamite"),
            regContext -> regContext.register(
                EntityType.ITEM,
                new IgnitedDroppedDynamiteLuminance()
            )
        );
    }

    private static final class IgnitedDynamiteHolderLuminance implements EntityLuminance {

        @Override
        public Type type() {
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

    private static final class IgnitedDroppedDynamiteLuminance implements EntityLuminance {

        @Override
        public Type type() {
            return EntityLuminance.Type.VALUE;
        }

        @Override
        public int getLuminance(ItemLightSourceManager itemLightSourceManager, Entity entity) {
            if (!(entity instanceof ItemEntity itemEntity)) return 0;
            ItemStack stack = itemEntity.getItem();
            if (!(stack.getItem() instanceof DynamiteItem)) return 0;
            if (!DynamiteItem.isIgnited(stack)) return 0;
            return 15;
        }
    }
}
