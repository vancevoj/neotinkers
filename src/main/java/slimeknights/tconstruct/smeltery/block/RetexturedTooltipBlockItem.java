package slimeknights.tconstruct.smeltery.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.item.BlockTooltipItem;

import java.util.function.Consumer;

/**
 * Block item for smeltery blocks that store a texture without extending {@link RetexturedBlock}, such as drains, ducts, chutes, and controllers.
 * Since 1.21.5 blocks no longer add item tooltips, so this replaces their old {@code appendHoverText} overrides.
 */
public class RetexturedTooltipBlockItem extends BlockTooltipItem {
  public RetexturedTooltipBlockItem(Block block, Properties props) {
    super(block, props);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    // block tooltips used to come first, then the optional tooltip from the block item
    // Mantle already adds the texture for retextured blocks, so skip those to prevent duplicates
    if (!(getBlock() instanceof RetexturedBlock)) {
      RetexturedBlock.appendTextureTooltip(stack, tooltip, flag);
    }
    super.appendHoverText(stack, context, display, tooltip, flag);
  }
}
