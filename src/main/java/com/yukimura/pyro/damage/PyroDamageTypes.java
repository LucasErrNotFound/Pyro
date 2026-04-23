package com.yukimura.pyro.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public class PyroDamageTypes {

    public static final ResourceKey<DamageType> DYNAMITE                = key("dynamite");
    public static final ResourceKey<DamageType> DYNAMITE_SELF           = key("dynamite_self");
    public static final ResourceKey<DamageType> DYNAMITE_DIRECT         = key("dynamite_direct");
    public static final ResourceKey<DamageType> CONTACT_DYNAMITE        = key("contact_dynamite");
    public static final ResourceKey<DamageType> CONTACT_DYNAMITE_SELF   = key("contact_dynamite_self");
    public static final ResourceKey<DamageType> CONTACT_DYNAMITE_DIRECT = key("contact_dynamite_direct");

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("pyro", path));
    }

    /** Blast damage from a thrown dynamite entity — attributes the kill to the thrower. */
    public static DamageSource dynamite(RegistryAccess registryAccess, Entity directEntity, Entity causingEntity) {
        return new DamageSource(holder(registryAccess, DYNAMITE), directEntity, causingEntity);
    }

    /** Inventory explosion — the holder held the dynamite for too long. */
    public static DamageSource dynamiteSelf(RegistryAccess registryAccess) {
        return new DamageSource(holder(registryAccess, DYNAMITE_SELF));
    }

    /** Blast damage from an inventory explosion — attributes the kill to the holder. */
    public static DamageSource dynamiteHolder(RegistryAccess registryAccess, Entity causingEntity) {
        return new DamageSource(holder(registryAccess, DYNAMITE), null, causingEntity);
    }

    /** Direct projectile impact — the thrown dynamite physically struck the entity. */
    public static DamageSource dynamiteDirect(RegistryAccess registryAccess, Entity directEntity, Entity causingEntity) {
        return new DamageSource(holder(registryAccess, DYNAMITE_DIRECT), directEntity, causingEntity);
    }

    /** Blast damage from a thrown contact dynamite entity — attributes the kill to the thrower. */
    public static DamageSource contactDynamite(RegistryAccess registryAccess, Entity directEntity, Entity causingEntity) {
        return new DamageSource(holder(registryAccess, CONTACT_DYNAMITE), directEntity, causingEntity);
    }

    /** Self-triggered explosion — the thrower detonated contact dynamite at their own feet. */
    public static DamageSource contactDynamiteSelf(RegistryAccess registryAccess) {
        return new DamageSource(holder(registryAccess, CONTACT_DYNAMITE_SELF));
    }

    /** Direct projectile impact — the thrown contact dynamite physically struck the entity. */
    public static DamageSource contactDynamiteDirect(RegistryAccess registryAccess, Entity directEntity, Entity causingEntity) {
        return new DamageSource(holder(registryAccess, CONTACT_DYNAMITE_DIRECT), directEntity, causingEntity);
    }

    private static Holder<DamageType> holder(RegistryAccess registryAccess, ResourceKey<DamageType> key) {
        return registryAccess.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
    }
}
