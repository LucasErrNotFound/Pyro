package com.yukimura.pyro.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class FuseItem extends Item {

    public FuseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> add, TooltipFlag flag) {
        add.accept(Component.translatable("tooltip.pyro.fuse").withStyle(ChatFormatting.GOLD));
    }
}
