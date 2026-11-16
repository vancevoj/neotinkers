package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

import javax.annotation.Nullable;

/**
 * Orientable smeltery block with a texture from its item.
 * Blocks no longer add item tooltips, so the texture tooltip comes from {@link slimeknights.tconstruct.smeltery.block.RetexturedTooltipBlockItem}.
 */
public class RetexturedOrientableSmelteryBlock extends OrientableSmelteryBlock {
  public RetexturedOrientableSmelteryBlock(Properties properties, BlockEntitySupplier<? extends SmelteryComponentBlockEntity> blockEntity) {
    super(properties, true, blockEntity);
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(world, pos, state, placer, stack);
    RetexturedBlock.updateTextureBlock(world, pos, stack);
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
    return RetexturedBlock.getPickBlock(world, pos, state);
  }
}
