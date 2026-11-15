package slimeknights.tconstruct.tables.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.block.RetexturedBlock;

import java.util.function.Consumer;

/**
 * Block item for retextured tables. Since 1.21.5 blocks no longer add tooltips to their items,
 * so the texture tooltip formerly added by {@link slimeknights.tconstruct.tables.block.RetexturedTableBlock} is added here.
 */
public class RetexturedTableBlockItem extends BlockItem {
  public RetexturedTableBlockItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, display, tooltip, flag);
    RetexturedBlock.appendTextureTooltip(stack, tooltip, flag);
  }
}
