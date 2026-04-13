package com.yukimura.pyro.item;

import com.yukimura.pyro.entity.DynamiteEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class DynamiteItem extends Item {

    private static final int FUSE_TICKS = 80;
    private static final int THROW_COOLDOWN = 40;
    static final String IGNITE_TIME_TAG = "igniteTime";

    public DynamiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);

        if (isIgnited(stack)) {
            // ── THROW ─────────────────────────────────────────────────────────
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            if (!level.isClientSide()) {
                Projectile.spawnProjectileFromRotation(
                    DynamiteEntity::new, (ServerLevel) level, stack, player, 0.0F, 1.5F, 1.0F);
                player.getCooldowns().addCooldown(stack, THROW_COOLDOWN);
            }
            // Always consume on throw — in creative the slot is restored from the menu.
            // Without this, the item stays in hand with fuseTicks set and explodes there too.
            stack.shrink(1);
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        // ── IGNITE ────────────────────────────────────────────────────────────
        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!offhand.is(Items.TORCH) && !offhand.is(Items.REDSTONE_TORCH)) {
            return InteractionResult.PASS;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            setIgnited(stack, serverLevel);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!isIgnited(stack)) return;

        long elapsed = level.getGameTime() - getIgniteTime(stack);
        if (elapsed < FUSE_TICKS) return;

        // ── FUSE EXPIRED: explode at the holder's position ────────────────────
        float blastRadius = 2.3f;
        float damageRadius = 4.0f;
        float maxDamage = 6.0f;

        if (entity instanceof Player holder) {
            holder.hurt(level.damageSources().explosion(null, null), Float.MAX_VALUE);
        }

        level.getEntitiesOfClass(LivingEntity.class,
            entity.getBoundingBox().inflate(damageRadius)
        ).forEach(e -> {
            if (e == entity) return; // owner already instakilled above
            double distance = e.distanceTo(entity);
            if (distance < damageRadius) {
                float scaled = (float) (1.0 - distance / damageRadius) * maxDamage;
                e.hurt(level.damageSources().explosion(null, null), scaled);
            }
        });

        // Consume the entire remaining stack — prevents repeat explosions next tick.
        stack.setCount(0);
        level.explode(entity, entity.getX(), entity.getY(), entity.getZ(),
            blastRadius, Level.ExplosionInteraction.BLOCK);
    }

    public static boolean isIgnited(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().contains(IGNITE_TIME_TAG);
    }

    public static long getIgniteTime(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getLong(IGNITE_TIME_TAG).orElse(0L);
    }

    public static int getRemainingTicks(ItemStack stack, long currentGameTime) {
        return (int) Math.max(1, FUSE_TICKS - (currentGameTime - getIgniteTime(stack)));
    }

    static void setIgnited(ItemStack stack, ServerLevel level) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(IGNITE_TIME_TAG, level.getGameTime());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
