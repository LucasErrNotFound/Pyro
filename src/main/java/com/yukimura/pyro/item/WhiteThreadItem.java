package com.yukimura.pyro.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class WhiteThreadItem extends Item {

    public WhiteThreadItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> add, TooltipFlag flag) {
        add.accept(Component.translatable("tooltip.pyro.spool_of_white_thread").withStyle(ChatFormatting.GOLD));
    }
}
