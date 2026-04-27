package com.yukimura.pyro.item;

import com.yukimura.pyro.damage.PyroDamageTypes;
import com.yukimura.pyro.entity.MolotovEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Consumer;

public class MolotovItem extends Item {

    public static final int FUSE_TICKS = 600;
    private static final int THROW_COOLDOWN = 40;
    static final String IGNITE_TIME_TAG = "igniteTime";

    public MolotovItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> add, TooltipFlag flag) {
        add.accept(Component.translatable("tooltip.pyro.molotov").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isIgnited(stack)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            if (!level.isClientSide()) {
                Projectile.spawnProjectileFromRotation(
                    MolotovEntity::new, (ServerLevel) level, stack, player, 0.0F, 1.0F, 1.0F);
                player.getCooldowns().addCooldown(stack, THROW_COOLDOWN);
            }

            stack.shrink(1);
            if (!stack.isEmpty()) {
                clearIgnited(stack);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherHandStack = player.getItemInHand(otherHand);
        boolean hasTorch      = otherHandStack.is(Items.TORCH) || otherHandStack.is(Items.REDSTONE_TORCH)
                || otherHandStack.is(Items.SOUL_TORCH) || otherHandStack.is(Items.COPPER_TORCH);
        boolean hasFireCharge = otherHandStack.is(Items.FIRE_CHARGE);
        boolean hasLavaBucket = otherHandStack.is(Items.LAVA_BUCKET);
        if (!hasTorch && !hasFireCharge && !hasLavaBucket) return InteractionResult.PASS;

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

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
        BlockPos clickedPosition = context.getClickedPos();
        BlockState blockState = level.getBlockState(clickedPosition);
        Player player = context.getPlayer();

        boolean isCampfire    = blockState.is(BlockTags.CAMPFIRES)
                && blockState.getValue(BlockStateProperties.LIT);
        boolean isHeatSource  = isHeatSourceAdjacentOrAt(level, blockState, clickedPosition, context.getClickedFace());
        boolean isFurnace     = isLitFurnace(blockState) && player != null && player.isShiftKeyDown();
        boolean isCandle      = blockState.is(BlockTags.CANDLES)
                && blockState.getValue(BlockStateProperties.LIT);
        boolean isPlacedTorch = isAnyPlacedTorch(blockState);
        boolean isMagmaBlock  = blockState.is(Blocks.MAGMA_BLOCK);
        if (!isCampfire && !isHeatSource && !isFurnace && !isCandle && !isPlacedTorch && !isMagmaBlock) return InteractionResult.PASS;

        level.playSound(null, clickedPosition.getX(), clickedPosition.getY(), clickedPosition.getZ(),
            SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, clickedPosition.getX(), clickedPosition.getY(), clickedPosition.getZ(),
            SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            setIgnited(stack, serverLevel);
        }

        if (player != null) player.awardStat(Stats.ITEM_USED.get(this));

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        long igniteTime = getIgniteTimeIfPresent(stack);
        if (igniteTime == Long.MIN_VALUE) return;

        long elapsed = level.getGameTime() - igniteTime;

        if (elapsed > 0 && elapsed % 80 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.0F);
        }

        if (elapsed < FUSE_TICKS) return;

        if (entity instanceof Player holder) {
            holder.hurt(PyroDamageTypes.molotovSelf(level.registryAccess()), 10.0f);
            holder.igniteForSeconds(5.0F);
        }

        MolotovEntity ghostEntity = new MolotovEntity(
            level, entity.getX(), entity.getY(), entity.getZ(), stack);
        level.addFreshEntity(ghostEntity);
        ghostEntity.triggerImpact();

        stack.setCount(0);
    }

    public static boolean isIgnited(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().contains(IGNITE_TIME_TAG);
    }

    public static long getIgniteTimeIfPresent(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(IGNITE_TIME_TAG)) return Long.MIN_VALUE;
        return tag.getLong(IGNITE_TIME_TAG).orElse(Long.MIN_VALUE);
    }

    public static long getIgniteTime(ItemStack stack) {
        long time = getIgniteTimeIfPresent(stack);
        return time == Long.MIN_VALUE ? 0L : time;
    }

    public static int getRemainingTicks(ItemStack stack, long currentGameTime) {
        return (int) Math.max(1, FUSE_TICKS - (currentGameTime - getIgniteTime(stack)));
    }

    public static void clearIgnited(ItemStack stack) {
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
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(java.util.List.of(1.0f), java.util.List.of(), java.util.List.of(), java.util.List.of()));
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

    private static boolean isHeatSourceAdjacentOrAt(Level level, BlockState clicked, BlockPos blockPosition, Direction clickedFace) {
        if (isFireOrLava(clicked)) return true;
        return level.getBlockState(blockPosition.relative(clickedFace)).getFluidState().is(FluidTags.LAVA);
    }

    private static boolean isFireOrLava(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock
                || state.getFluidState().is(FluidTags.LAVA);
    }
}
