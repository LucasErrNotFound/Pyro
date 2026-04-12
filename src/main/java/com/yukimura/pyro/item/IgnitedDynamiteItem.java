package com.yukimura.pyro.item;

import com.yukimura.pyro.entity.DynamiteEntity;
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
import net.minecraft.world.level.Level;

public class IgnitedDynamiteItem extends Item {

    public static final int COOLDOWN_TICKS = 20;

    public IgnitedDynamiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack itemStack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
            0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            Projectile.spawnProjectileFromRotation(
                DynamiteEntity::new, (ServerLevel) level, itemStack, player, 0.0F, 1.5F, 1.0F);
            player.getCooldowns().addCooldown(itemStack, COOLDOWN_TICKS);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) itemStack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
