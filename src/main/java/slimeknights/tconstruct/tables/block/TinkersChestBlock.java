package slimeknights.tconstruct.tables.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;
import slimeknights.tconstruct.tables.item.TinkersChestBlockItem;

public class TinkersChestBlock extends ChestBlock {
  public TinkersChestBlock(Properties builder, BlockEntitySupplier<? extends BlockEntity> be, boolean dropsItems) {
    super(builder, be, dropsItems);
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
    ItemStack stack = new ItemStack(this);
    if (world.getBlockEntity(pos) instanceof TinkersChestBlockEntity te && te.hasColor()) {
      TinkersChestBlockItem.setColor(stack, te.getColor());
    }
    return stack;
  }
}
