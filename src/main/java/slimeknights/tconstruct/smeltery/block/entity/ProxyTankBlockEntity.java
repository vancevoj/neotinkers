package slimeknights.tconstruct.smeltery.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.mantle.block.InventoryBlock;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.tank.ProxyItemTank;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/** Block entity with a tank that proxies to the nested item handler */
public class ProxyTankBlockEntity extends MantleBlockEntity implements IFluidTankUpdater {
  /** Direct access to the fluid handler and item handler */
  @Getter
  private final ProxyItemTank<ProxyTankBlockEntity> itemTank = new ProxyItemTank<>(this);
  /** Last comparator strength to reduce block updates */
  private int lastStrength = -1;
  protected ProxyTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public ProxyTankBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.proxyTank.get(), pos, state);
  }


  /* Comparators */

  /**
   * Gets the current comparator strength for the tank
   * @return  Tank comparator strength
   */
  private int calculateComparatorStrength() {
    int capacity = itemTank.getTankCapacity(0);
    if (capacity == 0) {
      return 0;
    }
    return 1 + 14 * itemTank.getFluidInTank(0).getAmount() / capacity;
  }

  /** Gets the current comparator strength */
  public int getComparatorStrength() {
    if (lastStrength == -1) {
      lastStrength = calculateComparatorStrength();
    }
    return lastStrength;
  }

  @Override
  public void onTankContentsChanged() {
    if (level != null && !level.isClientSide()) {
      setChangedFast();
      int newStrength = calculateComparatorStrength();
      if (newStrength != lastStrength) {
        lastStrength = newStrength;
        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
      }
    }
  }

  /* Removal */

  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    // drop the item tank, was in the block removal hook before 1.21.5
    if (level != null) {
      InventoryBlock.dropInventoryItems(level, pos, itemTank);
    }
    super.preRemoveSideEffects(pos, state);
  }

  /* Interaction */

  /** Called when a player interacts with the fluid cannon */
  public void interact(Player player, InteractionHand hand, boolean clickedTank) {
    // skip client side
    if (level == null || level.isClientSide()) {
      return;
    }

    // transfer fluid if clicked tank
    ItemStack held = player.getItemInHand(hand);
    ItemStack inventory = itemTank.getStack();
    // if we have an active tank, try interacting
    if (!inventory.isEmpty()) {
      // must have a held item to interact
      if (!held.isEmpty() && FluidTransferHelper.interactWithContainer(level, worldPosition, itemTank.getFluidHandler(), player, hand).didTransfer()
        || FluidTransferHelper.interactWithFilledBucket(level, worldPosition, itemTank.getFluidHandler(), player, hand, getBlockState().getValue(HORIZONTAL_FACING)).didTransfer()) {
        return;
      }
      // if we clicked the tank, don't try and swap items unless we have no tank
      // if we clicked the item, then we probably want it removed
      if (clickedTank) {
        return;
      }
    }
    // no fluid transfer? swap items around
    // inventory is empty means place item inside
    if (inventory.isEmpty()) {
      if (!held.isEmpty() && itemTank.isValid(0, ItemResource.of(held))) {
        // split the stack to place into inventory
        ItemStack stack = held.split(itemTank.getCapacityAsInt(0, ItemResource.of(held)));
        player.setItemInHand(hand, held.isEmpty() ? ItemStack.EMPTY : held);
        itemTank.setStack(stack);
      }
      // if not holding anything, pick up the stack
    } else if (held.isEmpty()) {
      player.setItemInHand(hand, inventory);
      itemTank.setStack(ItemStack.EMPTY);
    } else {
      // the proxy tank prefers you giving it the same instance it had before on no change
      player.addItem(inventory);
      itemTank.setStack(inventory);
    }
  }


  /* NBT */
  private static final String TAG_ITEM = "item";

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    readItem(input);
  }

  /** Reads the item from the input, used for both saving and syncing */
  private void readItem(ValueInput input) {
    // Mantle's handler reads the empty tag of an empty tank as no item
    input.child(TAG_ITEM).ifPresent(itemTank::readFromNBT);
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    itemTank.writeToNBT(output.child(TAG_ITEM));
  }
}
