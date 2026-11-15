package slimeknights.tconstruct.gadgets.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.tconstruct.gadgets.block.FoodCakeBlock;

import java.util.function.Consumer;

/** Block item for food cakes, shows the effects of a slice. Blocks no longer add item tooltips, so the cake tooltip moved here. */
public class FoodCakeItem extends BlockTooltipItem {
  public FoodCakeItem(Block block, Properties properties) {
    super(block, properties);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, display, tooltip, flag);
    if (getBlock() instanceof FoodCakeBlock cake) {
      cake.appendHoverText(tooltip);
    }
  }
}
