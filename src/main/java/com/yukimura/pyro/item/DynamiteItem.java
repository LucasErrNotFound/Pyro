package com.yukimura.pyro.item;

import com.yukimura.pyro.damage.PyroDamageTypes;
import com.yukimura.pyro.entity.DynamiteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Consumer;

public class DynamiteItem extends Item {

    private static final int FUSE_TICKS = 80;
    private static final int THROW_COOLDOWN = 40;
    static final String IGNITE_TIME_TAG = "igniteTime";

    public DynamiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> add, TooltipFlag flag) {
        add.accept(Component.translatable("tooltip.pyro.dynamite").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
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

            stack.shrink(1);
            if (!stack.isEmpty()) {
                clearIgnited(stack);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        // ── IGNITE ────────────────────────────────────────────────────────────
        InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherHandStack = player.getItemInHand(otherHand);
        boolean hasTorch     = otherHandStack.is(Items.TORCH) || otherHandStack.is(Items.REDSTONE_TORCH)
                || otherHandStack.is(Items.SOUL_TORCH) || otherHandStack.is(Items.COPPER_TORCH);
        boolean hasFireCharge = otherHandStack.is(Items.FIRE_CHARGE);
        if (!hasTorch && !hasFireCharge) return InteractionResult.PASS;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            setIgnited(stack, serverLevel);
        }

        if (hasFireCharge && !player.getAbilities().instabuild) {
            otherHandStack.shrink(1);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (isIgnited(stack)) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState blockState = level.getBlockState(pos);
        Player player = context.getPlayer();

        boolean isCampfire   = blockState.is(BlockTags.CAMPFIRES)
                && blockState.getValue(BlockStateProperties.LIT);
        boolean isHeatSource = isHeatSourceAdjacentOrAt(level, blockState, pos, context.getClickedFace());
        boolean isFurnace    = isLitFurnace(blockState) && player != null && player.isShiftKeyDown();
        boolean isCandle      = blockState.is(BlockTags.CANDLES)
                && blockState.getValue(BlockStateProperties.LIT);
        boolean isPlacedTorch = isAnyPlacedTorch(blockState);
        if (!isCampfire && !isHeatSource && !isFurnace && !isCandle && !isPlacedTorch) return InteractionResult.PASS;

        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
            SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
            SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            setIgnited(stack, serverLevel);
        }

        if (player != null) player.awardStat(Stats.ITEM_USED.get(this));

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!isIgnited(stack)) return;

        long elapsed = level.getGameTime() - getIgniteTime(stack);
        if (elapsed < FUSE_TICKS) return;

        // ── FUSE EXPIRED: explode at the holder's position ────────────────────
        int stackCount = stack.getCount();
        float damageRadius = 5.0f * stackCount;
        float maxDamage = 15.0f * stackCount;

        if (entity instanceof Player holder) {
            holder.hurt(PyroDamageTypes.dynamiteSelf(level.registryAccess()), Float.MAX_VALUE);
        }

        DamageSource nearbySource = PyroDamageTypes.dynamiteHolder(level.registryAccess(),
                entity instanceof LivingEntity living ? living : null);
        level.getEntitiesOfClass(LivingEntity.class,
            entity.getBoundingBox().inflate(damageRadius)
        ).forEach(e -> {
            if (e == entity) return; // holder already killed above
            double distance = e.distanceTo(entity);
            if (distance < damageRadius) {
                float scaled = (float) (1.0 - distance / damageRadius) * maxDamage;
                e.hurt(nearbySource, scaled);
            }
        });

        stack.setCount(0);

        double spread = (stackCount - 1) * 0.75;
        for (int i = 0; i < stackCount; i++) {
            double offsetX = i == 0 ? 0.0 : (level.getRandom().nextDouble() * 2.0 - 1.0) * spread;
            double offsetY = i == 0 ? 0.0 : (level.getRandom().nextDouble() * 2.0 - 1.0) * spread * 0.4;
            double offsetZ = i == 0 ? 0.0 : (level.getRandom().nextDouble() * 2.0 - 1.0) * spread;
            level.explode(entity,
                entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                4.0f, Level.ExplosionInteraction.BLOCK);
        }
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

    private static boolean isAnyPlacedTorch(BlockState state) {
        if (state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)) {
            return state.getValue(BlockStateProperties.LIT);
        }
        return state.is(Blocks.TORCH)        || state.is(Blocks.WALL_TORCH)
            || state.is(Blocks.SOUL_TORCH)   || state.is(Blocks.SOUL_WALL_TORCH)
            || state.is(Blocks.COPPER_TORCH) || state.is(Blocks.COPPER_WALL_TORCH);
    }

    private static boolean isLitFurnace(BlockState state) {
        return (state.is(Blocks.FURNACE) || state.is(Blocks.SMOKER) || state.is(Blocks.BLAST_FURNACE))
                && state.getValue(BlockStateProperties.LIT);
    }

    private static boolean isHeatSourceAdjacentOrAt(Level level, BlockState clicked, BlockPos pos, Direction clickedFace) {
        if (isFireOrLava(clicked)) return true;
        return level.getBlockState(pos.relative(clickedFace)).getFluidState().is(FluidTags.LAVA);
    }

    private static boolean isFireOrLava(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock
                || state.getFluidState().is(FluidTags.LAVA);
    }

    static void clearIgnited(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(IGNITE_TIME_TAG);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    static void setIgnited(ItemStack stack, ServerLevel level) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(IGNITE_TIME_TAG, level.getGameTime());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        // Signal the item model to switch to the "lit" variant (dynamite_lit.json)
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(java.util.List.of(1.0f), java.util.List.of(), java.util.List.of(), java.util.List.of()));
    }
}
