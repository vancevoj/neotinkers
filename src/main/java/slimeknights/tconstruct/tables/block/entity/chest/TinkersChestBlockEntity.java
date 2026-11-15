package slimeknights.tconstruct.tables.block.entity.chest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;

import javax.annotation.Nullable;

/**
 * Chest holding 64 slots of 16 items each
 */
public class TinkersChestBlockEntity extends AbstractChestBlockEntity {
  /** NBT tag for colors of the chest */
  public static final String TAG_CHEST_COLOR = "color";
  /** Default color for a chest */
  public static final int DEFAULT_COLOR = 0x407686;
  public static final Component NAME = TConstruct.makeTranslation("gui", "tinkers_chest");

  /** Current display color for the chest */
  @Getter
  private int color = DEFAULT_COLOR;
  /** If true, a custom color was set */
  @Getter @Accessors(fluent = true)
  private boolean hasColor = false;

  public TinkersChestBlockEntity(BlockPos pos, BlockState state) {
    super(TinkerTables.tinkersChestTile.get(), pos, state, NAME, new TinkersChestItemHandler());
  }

  /** Sets the color of the chest */
  public void setColor(int color) {
    this.color = color;
    this.hasColor = true;
  }

  @Override
  public boolean canInsert(Player player, ItemStack heldItem) {
    return false;
  }

  @Override
  public void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    if (hasColor) {
      output.putInt(TAG_CHEST_COLOR, color);
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    input.getInt(TAG_CHEST_COLOR).ifPresent(this::setColor);
  }

  /** Item handler for tinkers chests */
  public static class TinkersChestItemHandler extends ItemStacksResourceHandler implements IChestItemHandler {
    @Setter @Nullable
    private MantleBlockEntity parent;
    public TinkersChestItemHandler() {
      super(64);
    }

    /** Gets the maximum number of items in each slot */
    public int getSlotLimit(int slot) {
      return 16;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
      // matches the old item stack handler: slot limit, but never more than the item stacks to
      int limit = getSlotLimit(index);
      return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
      return stacks.get(slot);
    }

    @Override
    public int getVisualSize() {
      return size();
    }

    @Override
    protected void onContentsChanged(int slot, ItemStack previous) {
      if (parent != null) {
        parent.setChangedFast();
      }
    }
  }
}
