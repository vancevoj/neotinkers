package slimeknights.tconstruct.tables.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;

/** Dyeable chest block */
public class TinkersChestBlockItem extends BlockItem {
  public TinkersChestBlockItem(Block blockIn, Properties builder) {
    super(blockIn, builder);
  }

  /** Checks if the stack has a custom color set */
  public static boolean hasCustomColor(ItemStack stack) {
    return stack.has(DataComponents.DYED_COLOR);
  }

  /** Gets the color of the chest from the stack, or the default if unset */
  public static int getColor(ItemStack stack) {
    return DyedItemColor.getOrDefault(stack, TinkersChestBlockEntity.DEFAULT_COLOR);
  }

  /** Sets the color of the chest on the stack */
  public static void setColor(ItemStack stack, int color) {
    stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
  }
}
