package slimeknights.tconstruct.gadgets.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class FancyItemFrameItem extends Item {

  private final TriFunction<? extends HangingEntity, Level, BlockPos, Direction> entityProvider;

  public FancyItemFrameItem(Properties props, TriFunction<? extends HangingEntity, Level, BlockPos, Direction> entityProvider) {
    super(props);
    this.entityProvider = entityProvider;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag pIsAdvanced) {
    tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
  }

  @Override
  @Nonnull
  public InteractionResult useOn(UseOnContext context) {
    BlockPos pos = context.getClickedPos();
    Direction facing = context.getClickedFace();
    BlockPos placeLocation = pos.relative(facing);
    Player player = context.getPlayer();
    ItemStack stack = context.getItemInHand();
    if (player != null && !this.canPlace(player, facing, stack, placeLocation)) {
      return InteractionResult.FAIL;
    }

    Level world = context.getLevel();
    HangingEntity frame = this.entityProvider.apply(world, placeLocation, facing);
    // apply components and the entity data component from the stack, matching vanilla HangingEntityItem
    EntityType.<HangingEntity>createDefaultStackConfig(world, stack, player).apply(frame);

    if (frame.survives()) {
      if (!world.isClientSide()) {
        frame.playPlacementSound();
        world.addFreshEntity(frame);
      }
      stack.shrink(1);
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.CONSUME;
  }

  private boolean canPlace(Player player, Direction facing, ItemStack stack, BlockPos pos) {
    return !player.level().isOutsideBuildHeight(pos) && player.mayUseItemAt(pos, facing, stack);
  }

  @FunctionalInterface
  public interface TriFunction<R, T, U, V> {
    R apply(T t, U u, V v);
  }
}
