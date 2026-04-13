package com.yukimura.pyro.entity;

import com.yukimura.pyro.Pyro;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class PyroEntities {

    public static final EntityType<DynamiteEntity> DYNAMITE_ENTITY = registerEntity(
        "dynamite",
        EntityType.Builder.<DynamiteEntity>of(DynamiteEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
    );

    private static <T extends Entity> EntityType<T> registerEntity(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Pyro.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        Pyro.LOGGER.info("Registering EntityTypes for " + Pyro.MOD_ID);
    }
}