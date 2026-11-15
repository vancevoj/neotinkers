package slimeknights.tconstruct.tables.block.entity.chest;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;
import slimeknights.tconstruct.tables.menu.TinkerChestContainerMenu;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Shared base logic for all Tinkers' chest tile entities */
public abstract class AbstractChestBlockEntity extends NameableBlockEntity {
  private static final String KEY_ITEMS = "Items";

  @Getter
  private final IChestItemHandler itemHandler;
  protected AbstractChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, IChestItemHandler itemHandler) {
    super(type, pos, state, name);
    itemHandler.setParent(this);
    this.itemHandler = itemHandler;
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player playerEntity) {
    return new TinkerChestContainerMenu(menuId, playerInventory, this);
  }

  /**
   * Checks if the given item should be inserted into the chest on interact
   * @param player    Player inserting
   * @param heldItem  Stack to insert
   * @return  Return true
   */
  public boolean canInsert(Player player, ItemStack heldItem) {
    return true;
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    // items are saved in the vanilla slot list format, we don't care about the size and need this format for compat with old worlds
    ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list(KEY_ITEMS, ItemStackWithSlot.CODEC);
    for (int i = 0; i < itemHandler.size(); i++) {
      ItemStack stack = itemHandler.getStackInSlot(i);
      if (!stack.isEmpty()) {
        items.add(new ItemStackWithSlot(i, stack));
      }
    }
  }

  /**
   * Reads the inventory from NBT, resiliently. Each stack is parsed independently so a single corrupt or
   * unparseable item can never take down the whole load. Previously any exception here propagated out of
   * {@link #loadAdditional}, and vanilla's {@code BlockEntity.loadStatic} discards the entire block entity when
   * loading throws, so the chest reloaded completely empty (every stored item lost). Bad slots are now logged
   * and skipped, keeping every item that still parses.
   */
  public void readInventory(CompoundTag tags, HolderLookup.Provider registries) {
    ListTag items = tags.getListOrEmpty(KEY_ITEMS);
    List<CompoundTag> list = new ArrayList<>(items.size());
    for (int i = 0; i < items.size(); i++) {
      items.getCompound(i).ifPresent(list::add);
    }
    readInventory(list, registries);
  }

  /** Reads the inventory from the block entity data, see {@link #readInventory(CompoundTag, HolderLookup.Provider)} */
  public void readInventory(ValueInput input) {
    // read the raw compounds so each stack can be parsed independently below
    readInventory(input.listOrEmpty(KEY_ITEMS, CompoundTag.CODEC), input.lookup());
  }

  /** Parses each stack from the list of slot compounds, skipping any slot that fails to parse */
  private void readInventory(Iterable<CompoundTag> items, HolderLookup.Provider registries) {
    RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
    int slots = itemHandler.size();
    for (CompoundTag itemTags : items) {
      try {
        ItemStackWithSlot.CODEC.parse(ops, itemTags)
          .resultOrPartial(error -> TConstruct.LOG.error("Skipping unloadable item in Tinkers' chest at {}: {}", getBlockPos(), error))
          .filter(entry -> entry.isValidInContainer(slots) && !entry.stack().isEmpty())
          .ifPresent(entry -> {
            ItemStack stack = entry.stack();
            // tools in chests never tick, so rebuild tools saved by an older version as they load
            // done before setting as the handler stores a copy of the stack
            ToolStack.updateDataVersion(stack);
            itemHandler.setStackInSlot(entry.slot(), stack);
          });
      } catch (Exception e) {
        TConstruct.LOG.error("Skipping unloadable item in Tinkers' chest at {}: {}", getBlockPos(), e.toString());
      }
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    readInventory(input);
  }
}
