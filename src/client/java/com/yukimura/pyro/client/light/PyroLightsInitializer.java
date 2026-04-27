package com.yukimura.pyro.client.light;

import com.yukimura.pyro.entity.PyroEntities;
import com.yukimura.pyro.item.DynamiteItem;
import com.yukimura.pyro.item.MolotovItem;
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
        // Thrown ignited dynamite glows in flight and on the ground; suppressed when submerged.
        context.entityLightSourceManager().onRegisterEvent().register(
            Identifier.fromNamespaceAndPath("pyro", "dynamite_entity"),
            regContext -> regContext.register(PyroEntities.DYNAMITE_ENTITY, new EntityLuminance() {
                @Override
                public Type type() { return EntityLuminance.Type.VALUE; }
                @Override
                public int getLuminance(ItemLightSourceManager manager, Entity entity) {
                    return entity.isUnderWater() ? 0 : 15;
                }
            })
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

        // Thrown ignited molotov glows in flight and on the ground.
        context.entityLightSourceManager().onRegisterEvent().register(
            Identifier.fromNamespaceAndPath("pyro", "molotov_entity"),
            regContext -> regContext.register(PyroEntities.MOLOTOV_ENTITY, new EntityLuminance() {
                @Override
                public Type type() { return EntityLuminance.Type.VALUE; }
                @Override
                public int getLuminance(ItemLightSourceManager manager, Entity entity) {
                    return entity.isUnderWater() ? 0 : 15;
                }
            })
        );

        // Player holding ignited molotov in main hand glows.
        context.entityLightSourceManager().onRegisterEvent().register(
            Identifier.fromNamespaceAndPath("pyro", "player_holding_lit_molotov"),
            regContext -> regContext.register(
                EntityType.PLAYER,
                new IgnitedMolotovHolderLuminance()
            )
        );

        // Dropped ignited molotov (ItemEntity on the ground) glows.
        context.entityLightSourceManager().onRegisterEvent().register(
            Identifier.fromNamespaceAndPath("pyro", "dropped_ignited_molotov"),
            regContext -> regContext.register(
                EntityType.ITEM,
                new IgnitedDroppedMolotovLuminance()
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
            if (player.isUnderWater()) return 0;
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
            if (itemEntity.isUnderWater()) return 0;
            return 15;
        }
    }

    private static final class IgnitedMolotovHolderLuminance implements EntityLuminance {

        @Override
        public Type type() {
            return EntityLuminance.Type.VALUE;
        }

        @Override
        public int getLuminance(ItemLightSourceManager itemLightSourceManager, Entity entity) {
            if (!(entity instanceof Player player)) return 0;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof MolotovItem)) return 0;
            if (!MolotovItem.isIgnited(held)) return 0;
            if (player.isUnderWater()) return 0;
            return 15;
        }
    }

    private static final class IgnitedDroppedMolotovLuminance implements EntityLuminance {

        @Override
        public Type type() {
            return EntityLuminance.Type.VALUE;
        }

        @Override
        public int getLuminance(ItemLightSourceManager itemLightSourceManager, Entity entity) {
            if (!(entity instanceof ItemEntity itemEntity)) return 0;
            ItemStack stack = itemEntity.getItem();
            if (!(stack.getItem() instanceof MolotovItem)) return 0;
            if (!MolotovItem.isIgnited(stack)) return 0;
            if (itemEntity.isUnderWater()) return 0;
            return 15;
        }
    }
}
