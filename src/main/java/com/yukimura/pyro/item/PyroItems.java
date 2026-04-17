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

    public static final Item FUSE = registerItem(
        "fuse",
        FuseItem::new,
        new Item.Properties()
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
            itemGroup.accept(FUSE);
        });
    }
}