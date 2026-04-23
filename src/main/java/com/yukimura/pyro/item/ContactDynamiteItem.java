package com.yukimura.pyro.item;

import com.yukimura.pyro.entity.ContactDynamiteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class ContactDynamiteItem extends Item {

    private static final int THROW_COOLDOWN = 40;

    public ContactDynamiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> add, TooltipFlag flag) {
        add.accept(Component.translatable("tooltip.pyro.contact_dynamite").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
            0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            Projectile.spawnProjectileFromRotation(
                ContactDynamiteEntity::new, (ServerLevel) level, stack, player, 0.0F, 1.5F, 1.0F);
            player.getCooldowns().addCooldown(stack, THROW_COOLDOWN);
        }

        stack.shrink(1);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
