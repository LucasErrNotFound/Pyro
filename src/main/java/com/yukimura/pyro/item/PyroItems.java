package com.yukimura.pyro.item;

import com.yukimura.pyro.Pyro;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class PyroItems {

    public static final Item DYNAMITE = registerItem(
        "dynamite",
        DynamiteItem::new,
        new Item.Properties()
            .stacksTo(16)
    );

    public static final Item CONTACT_DYNAMITE = registerItem(
        "contact_dynamite",
        ContactDynamiteItem::new,
        new Item.Properties()
            .stacksTo(16)
    );

    public static final Item MOLOTOV = registerItem(
        "molotov",
        MolotovItem::new,
        new Item.Properties()
            .stacksTo(1)
    );

    public static final Item FUSE = registerItem(
        "fuse",
        FuseItem::new,
        new Item.Properties()
            .stacksTo(64)
    );

    public static final Item FABRIC = registerItem(
        "fabric_white",
        FabricItem::new,
        new Item.Properties()
            .stacksTo(64)
    );

    public static final Item SPOOL_OF_WHITE_THREAD = registerItem(
        "spool_of_white_thread",
        WhiteThreadItem::new,
        new Item.Properties()
            .stacksTo(16)
    );

    public static <T extends Item> T registerItem(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Pyro.MOD_ID, name));
        T item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(itemGroup -> {
            itemGroup.accept(DYNAMITE);
            itemGroup.accept(CONTACT_DYNAMITE);
            itemGroup.accept(MOLOTOV);
            itemGroup.accept(FUSE);
            itemGroup.accept(FABRIC);
            itemGroup.accept(SPOOL_OF_WHITE_THREAD);
        });
    }
}