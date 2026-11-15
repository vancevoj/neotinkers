package slimeknights.tconstruct.smeltery.block.entity.module;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Inventory composite made of a set of melting module inventories.
 * <p>
 * Changes through the transfer API are kept pending while the transaction is open and applied to the modules when the root transaction commits,
 * so the slot sync packets and recipe updates from {@link MeltingModule#setStack(ItemStack)} never run for simulated transfers.
 * Also implements {@link IndexModifier} to set slots directly from menus and sync packets.
 */
public class MeltingModuleInventory extends SnapshotJournal<Int2ObjectMap<ItemStack>> implements ResourceHandler<ItemResource>, IndexModifier<ItemResource> {
  /**
   * There are {@link Short#MAX_VALUE} data slots in a standard container instance.
   * The smeltery requires 6 slots for fuel syncing. It also requires 3 slots per inventory slot.
   * Thus, the largest number of slots we can have without breaking syncing is the below equation.
   * This does leave us with 1 extra unused slot. If we add anything that uses more slots we will want to adjust this number.
   */
  private static final int MAX_SIZE = (Short.MAX_VALUE - 7) / 3;
  private static final String TAG_SLOT = "slot";
  private static final String TAG_ITEMS = "items";
  private static final String TAG_SIZE = "size";

  /** Parent tile entity */
  private final MantleBlockEntity parent;
  /** Fluid handler for outputs */
  protected final ResourceHandler<FluidResource> fluidHandler;
  /** Array of modules containing each slot */
  private MeltingModule[] modules;
  /** If true, module cannot be resized */
  private final boolean strictSize;
  /** Number of nuggets to produce when melting an ore */
  private final IOreRate oreRate;
  /** Stacks changed in the current transaction by slot, empty outside of transactions. Stacks in this map are never modified, only replaced */
  private Int2ObjectMap<ItemStack> pending = new Int2ObjectOpenHashMap<>();

  /**
   * Creates a new inventory with a fixed size
   * @param parent         Parent tile
   * @param fluidHandler   Tank for output
   * @param oreRate        Ore rate
   * @param size           Size
   */
  public MeltingModuleInventory(MantleBlockEntity parent, ResourceHandler<FluidResource> fluidHandler, IOreRate oreRate, int size) {
    this.parent = parent;
    this.fluidHandler = fluidHandler;
    this.modules = new MeltingModule[size];
    this.oreRate = oreRate;
    this.strictSize = size != 0;
  }

  /**
   * Creates a new inventory with a variable size
   * @param parent         Parent tile
   * @param fluidHandler   Tank for output
   * @param oreRate        Ore rate
   */
  public MeltingModuleInventory(MantleBlockEntity parent, ResourceHandler<FluidResource> fluidHandler, IOreRate oreRate) {
    this(parent, fluidHandler, oreRate, 0);
  }

  /* Properties */

  @Override
  public int size() {
    return modules.length;
  }

  /** @deprecated use {@link #size()} */
  @Deprecated
  public int getSlots() {
    return size();
  }

  /**
   * Checks if the given slot index is valid
   * @param slot  Slot index to check
   * @return  True if valid
   */
  public boolean validSlot(int slot) {
    return slot >= 0 && slot < size();
  }

  @Override
  public long getCapacityAsLong(int index, ItemResource resource) {
    return validSlot(index) ? 1 : 0;
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    // a menu opened before the structure shrank still has slots past the end, setting those ignores the stack so placing there deletes the item
    return validSlot(index);
  }

  /** Returns true if a slot is defined in the array */
  private boolean hasModule(int slot) {
    return validSlot(slot) && modules[slot] != null;
  }

  /**
   * Gets the current time of a slot
   * @param slot  Slot index
   * @return  Slot temperature
   */
  public int getCurrentTime(int slot) {
    return hasModule(slot) ? modules[slot].getCurrentTime() : 0;
  }

  /**
   * Gets the required time for a slot
   * @param slot  Slot index
   * @return  Required time
   */
  public int getRequiredTime(int slot) {
    return hasModule(slot) ? modules[slot].getRequiredTime() : 0;
  }

  /**
   * Gets the required temperature for a slot
   * @param slot  Slot index
   * @return  Required temperature
   */
  public int getRequiredTemp(int slot) {
    return hasModule(slot) ? modules[slot].getRequiredTemp() : 0;
  }


  /* Sub modules */

  /**
   * Gets the module for the given index
   * @param slot  Index
   * @return  Module for index
   * @throws IndexOutOfBoundsException  index is invalid
   */
  public MeltingModule getModule(int slot) {
    if (!validSlot(slot)) {
      throw new IndexOutOfBoundsException();
    }
    if (modules[slot] == null) {
      modules[slot] = new MeltingModule(parent, recipe -> tryFillTank(slot, recipe), oreRate, slot);
    }
    return modules[slot];
  }

  /**
   * Resizes the module to a new size
   * @param newSize        New size
   * @param stackConsumer  Consumer for any stacks that no longer fit
   * @throws IllegalStateException  If this inventory cannot be resized
   */
  public void resize(int newSize, Consumer<ItemStack> stackConsumer) {
    if (strictSize) {
      throw new IllegalStateException("Cannot resize this melting module inventory");
    }
    if (newSize > MAX_SIZE) {
      newSize = MAX_SIZE;
    }
    // nothing to do
    if (newSize == modules.length) {
      return;
    }
    // if shrinking, drop extra items
    if (newSize < modules.length) {
      for (int i = newSize; i < modules.length; i++) {
        if (modules[i] != null && !modules[i].getStack().isEmpty()) {
          stackConsumer.accept(modules[i].getStack());
        }
      }
    }

    // resize the module array
    modules = Arrays.copyOf(modules, newSize);
    parent.setChangedFast();
  }


  /* Item handling */

  /**
   * Gets the stack in the given slot, ignoring changes in transactions that are not yet committed
   * @param slot  Slot index
   * @return  Stack in the slot, must not be modified
   */
  public ItemStack getStackInSlot(int slot) {
    if (validSlot(slot)) {
      // don't create the slot, just reading
      if (modules[slot] != null) {
        return modules[slot].getStack();
      }
    }
    return ItemStack.EMPTY;
  }

  /**
   * Sets the stack in the given slot directly, should not be called during a transaction
   * @param slot   Slot index
   * @param stack  New stack
   */
  public void setStackInSlot(int slot, ItemStack stack) {
    // actually set the stack
    if (validSlot(slot)) {
      if (stack.isEmpty()) {
        if (modules[slot] != null) {
          modules[slot].setStack(ItemStack.EMPTY);
        }
      } else {
        // validate size
        if (stack.getCount() > 1) {
          stack.setCount(1);
        }
        getModule(slot).setStack(stack);
      }
    }
  }

  /** Gets the stack including changes from the current transaction. Must not be modified */
  private ItemStack getCurrent(int slot) {
    ItemStack stack = pending.get(slot);
    return stack != null ? stack : getStackInSlot(slot);
  }

  @Override
  public ItemResource getResource(int index) {
    return ItemResource.of(getCurrent(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    return getCurrent(index).getCount();
  }

  @Override
  public void set(int index, ItemResource resource, int amount) {
    setStackInSlot(index, resource.isEmpty() || amount <= 0 ? ItemStack.EMPTY : resource.toStack(amount));
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || !validSlot(index)) {
      return 0;
    }

    // if the slot is empty, we can insert. Ignores stack sizes at this time, assuming always 1
    if (getCurrent(index).isEmpty()) {
      updateSnapshots(transaction);
      pending.put(index, resource.toStack(1));
      return 1;
    }
    return 0;
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || !validSlot(index)) {
      return 0;
    }

    ItemStack existing = getCurrent(index);
    if (existing.isEmpty() || !resource.matches(existing)) {
      return 0;
    }
    int extracted = Math.min(amount, existing.getCount());
    updateSnapshots(transaction);
    pending.put(index, extracted < existing.getCount() ? existing.copyWithCount(existing.getCount() - extracted) : ItemStack.EMPTY);
    return extracted;
  }


  /* Transactions */

  @Override
  protected Int2ObjectMap<ItemStack> createSnapshot() {
    // stacks are replaced rather than modified, so a shallow copy is enough
    return new Int2ObjectOpenHashMap<>(pending);
  }

  @Override
  protected void revertToSnapshot(Int2ObjectMap<ItemStack> snapshot) {
    pending = snapshot;
  }

  @Override
  protected void onRootCommit(Int2ObjectMap<ItemStack> originalState) {
    if (!pending.isEmpty()) {
      Int2ObjectMap<ItemStack> changes = pending;
      pending = new Int2ObjectOpenHashMap<>();
      for (Int2ObjectMap.Entry<ItemStack> entry : changes.int2ObjectEntrySet()) {
        // setStackInSlot ignores slots past the end, in case we got resized
        setStackInSlot(entry.getIntKey(), entry.getValue());
      }
    }
  }


  /* Heating */

  /**
   * Checks if any slot can heat
   * @param temperature  Temperature to try
   * @return  True if a slot can heat
   */
  public boolean canHeat(int temperature) {
    for (MeltingModule module : modules) {
      if (module != null && module.canHeatItem(temperature)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tries to fill the fluid handler with the given fluid
   * @param index   Index of the module being filled
   * @param recipe  Recipe to add
   * @return  True if filled, false if not enough space for the whole fluid
   */
  protected boolean tryFillTank(int index, IMeltingRecipe recipe) {
    FluidStack fluid = recipe.getOutput(getModule(index));
    // nothing to fill means we trivially filled everything
    if (fluid.isEmpty()) {
      return true;
    }
    try (Transaction transaction = Transaction.openRoot()) {
      if (fluidHandler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction) == fluid.getAmount()) {
        transaction.commit();
        return true;
      }
    }
    return false;
  }

  /**
   * Heats all items in the inventory
   * @param temperature  Heating structure temperature
   */
  public void heatItems(int temperature, int rate) {
    for (MeltingModule module : modules) {
      if (module != null) {
        module.heatItem(temperature, rate);
      }
    }
  }

  /**
   * Cools down all items in the inventory, used when there is no fuel
   */
  public void coolItems() {
    for (MeltingModule module : modules) {
      if (module != null) {
        module.coolItem();
      }
    }
  }

  /**
   * Writes this inventory to the given output, typically a child output
   * @param output  Output
   */
  public void writeToTag(ValueOutput output) {
    ValueOutput.ValueOutputList list = output.childrenList(TAG_ITEMS);
    for (int i = 0; i < modules.length; i++) {
      if (modules[i] != null && !modules[i].getStack().isEmpty()) {
        ValueOutput moduleTag = list.addChild();
        modules[i].writeToTag(moduleTag);
        writeIndex(moduleTag, TAG_SLOT, i);
      }
    }
    writeIndex(output, TAG_SIZE, modules.length);
  }

  /**
   * Writes a slot index or size. Smelteries easily exceed 255 slots, so large values need an int, but older builds only read a byte.
   * Writing a byte whenever the value fits keeps smelteries that fit in a byte loading after a downgrade.
   * @param output  Output
   * @param key     Key to write
   * @param value   Index or size
   */
  private static void writeIndex(ValueOutput output, String key, int value) {
    if (value < 256) {
      output.putByte(key, (byte)value);
    } else {
      output.putInt(key, value);
    }
  }

  /**
   * Reads a slot index or size, the inverse of {@link #writeIndex(ValueOutput, String, int)}. Bytes must be read unsigned
   * @param input  Input
   * @param key    Key to read
   * @return  Index or size, or -1 if missing
   */
  private static int readIndex(ValueInput input, String key) {
    // read the raw tag, as the value input cannot tell a byte apart from an int
    return switch (input.read(key, ExtraCodecs.NBT).orElse(null)) {
      case ByteTag(byte value) -> value & 255;
      case NumericTag numeric -> numeric.intValue();
      case null, default -> -1;
    };
  }

  /**
   * Reads this inventory from the given input, typically a child input
   * @param input  Input
   */
  public void readFromTag(ValueInput input) {
    if (!strictSize) {
      int newSize = Math.min(Math.max(readIndex(input, TAG_SIZE), 0), MAX_SIZE);
      if (newSize != modules.length) {
        modules = Arrays.copyOf(modules, newSize);
      }
    }
    // remove old data
    for (MeltingModule module : modules) {
      if (module != null) {
        module.setStack(ItemStack.EMPTY);
      }
    }

    for (ValueInput item : input.childrenListOrEmpty(TAG_ITEMS)) {
      int slot = readIndex(item, TAG_SLOT);
      if (validSlot(slot)) {
        getModule(slot).readFromTag(item);
      }
    }
  }


  /* Container sync */

  /**
   * Sets up all sub slots for tracking
   * @param consumer  IIntArray consumer
   */
  public void trackInts(Consumer<ContainerData> consumer) {
    for (int i = 0; i < size(); i++) {
      consumer.accept(getModule(i));
    }
  }

  @Override
  public String toString() {
    return "MeltingModuleInventory[" + modules.length + "]";
  }
}
