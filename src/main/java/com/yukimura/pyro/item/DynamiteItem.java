package com.yukimura.pyro.item;

import com.yukimura.pyro.entity.DynamiteEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DynamiteItem extends Item {

    public DynamiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 1.0F, 1.0F);

        if (!level.isClientSide()) {
            DynamiteEntity dynamiteEntity = new DynamiteEntity(level, player);
            level.addFreshEntity(dynamiteEntity);
        }
        player.increaseScore(Stats.ITEM_USED.getRegistry().getId(this));

        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}