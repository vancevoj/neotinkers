package slimeknights.tconstruct.library.tools.capability.inventory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.config.Config.ToolSyncType;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider.IToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.ToolItemAccess;
import slimeknights.tconstruct.library.tools.capability.ToolItemAccess.Edit;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.display.ToolNameHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.tools.menu.ToolContainerMenu;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Capability for a tool with an inventory.
 * <p>
 * In 26.3 this is a {@link ResourceHandler} of {@link ItemResource} built from the {@link ItemAccess} of the {@link Capabilities.Item#ITEM} query.
 * Each change runs the {@link InventoryModifierHook}s on a private copy of the tool and exchanges the changed tool into the access in the transaction,
 * so the tool data of the accessed stack only changes when the transaction commits. Amounts scale with the number of tools in the access.
 * Also an {@link IndexModifier} so menu slots can set stacks directly.
 */
public class ToolInventoryCapability extends InventoryModifierHookIterator<ModifierEntry> implements ResourceHandler<ItemResource>, IndexModifier<ItemResource> {
  /** Boolean key to set in volatile mod data for the total slot count across all modifiers */
  public static final Identifier TOTAL_SLOTS = TConstruct.getResource("total_item_slots");
  /** Boolean key to set in volatile mod data to show the offand in the inventory menu */
  public static final Identifier INCLUDE_OFFHAND = TConstruct.getResource("inventory_show_offhand");
  /** Boolean key to set to enable the 3x3 crafting table in the tool inventory */
  public static final Identifier CRAFTING_TABLE = TConstruct.getResource("crafting_table");
  /** Boolean key to set to enable the 2x2 crafting table in the tool inventory */
  public static final Identifier INVENTORY_CRAFTING = TConstruct.getResource("inventory_crafting");

  /** Modifier hook instance to make an inventory modifier */
  public static final ModuleHook<InventoryModifierHook> HOOK = ModifierHooks.register(TConstruct.getResource("inventory"), InventoryModifierHook.class, InventoryModifierHookMerger::new, new InventoryModifierHook() {
    @Override
    public int getSlots(IToolStackView tool, ModifierEntry modifier) {
      return 0;
    }

    @Override
    public ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot) {
      return ItemStack.EMPTY;
    }

    @Override
    public void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {}

    @Override
    public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
      return 0;
    }

    @Override
    public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      return false;
    }

    @Override
    public StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
      return StackMatch.EMPTY;
    }

    @Override
    public List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry modifier, List<ItemStack> stackList) {
      return List.of();
    }
  });

  /** Access to the tool */
  private final ToolItemAccess access;
  /** Tool view the cached stacks were parsed from */
  @Nullable
  private IToolStackView cachedTool;
  /** Cache of all stacks that have been parsed thus far, never handed out directly */
  @Nullable
  private ItemStack[] cachedStacks;

  public ToolInventoryCapability(ItemAccess access) {
    this.access = new ToolItemAccess(access);
  }

  @Override
  public int size() {
    IToolStackView tool = access.getTool();
    if (tool == null) {
      return 0;
    }
    return tool.getVolatileData().getInt(TOTAL_SLOTS);
  }


  /* Basic inventory */

  @Override
  protected Iterator<ModifierEntry> getIterator(IToolStackView tool) {
    // iterate in reverse order, as that allows us to put shield strap/tool belt later in the UI without breaking the keybind
    return new ReversedListIterator<>(tool.getModifierList());
  }

  @Override
  protected InventoryModifierHook getHook(ModifierEntry entry) {
    indexEntry = entry;
    return entry.getHook(HOOK);
  }

  /** If true, the given stack is blacklisted from being stored in a tool */
  public static boolean isBlacklisted(ItemStack stack) {
    // item capabilities need an item access in 26.3; the access is only used to look up the capability, nothing is changed through it
    return !stack.getItem().canFitInsideContainerItems() || stack.is(TinkerTags.Items.TOOL_INVENTORY_BLACKLIST)
      || (!stack.isEmpty() && stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack)) != null);
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    if (resource.isEmpty()) {
      return false;
    }
    // no nesting item handlers
    ItemStack stack = resource.toStack();
    if (isBlacklisted(stack)) {
      return false;
    }
    IToolStackView tool = access.getTool();
    if (tool != null && index >= 0) {
      InventoryModifierHook inventory = findHook(tool, index);
      if (inventory != null) {
        return inventory.isItemValid(tool, indexEntry, index - startIndex, stack);
      }
    }
    return false;
  }

  @Override
  public long getCapacityAsLong(int index, ItemResource resource) {
    if (!resource.isEmpty() && !isValid(index, resource)) {
      return 0;
    }
    IToolStackView tool = access.getTool();
    if (tool != null && index >= 0) {
      InventoryModifierHook inventory = findHook(tool, index);
      if (inventory != null) {
        int limit = inventory.getSlotLimit(tool, indexEntry, index - startIndex);
        // stacks never exceed their max size in a tool inventory
        if (!resource.isEmpty()) {
          limit = Math.min(limit, resource.getMaxStackSize());
        }
        return (long) access.getCount() * limit;
      }
    }
    return 0;
  }


  /* Item stack cache */

  /** Gets the stack in the given slot of a single tool, parsing it if not yet cached. The result must not be modified. */
  private ItemStack getCachedStack(int slot) {
    IToolStackView tool = access.getTool();
    if (tool == null || slot < 0) {
      return ItemStack.EMPTY;
    }
    // the view changes whenever the tool data changes, so the cache is only valid for the view it was parsed from
    if (tool != cachedTool) {
      cachedTool = tool;
      cachedStacks = null;
    }
    int slots = tool.getVolatileData().getInt(TOTAL_SLOTS);
    if (slot >= slots) {
      return ItemStack.EMPTY;
    }
    if (cachedStacks == null || cachedStacks.length != slots) {
      cachedStacks = new ItemStack[slots];
    }
    ItemStack stack = cachedStacks[slot];
    if (stack == null) {
      InventoryModifierHook inventory = findHook(tool, slot);
      stack = inventory != null ? inventory.getStack(tool, indexEntry, slot - startIndex) : ItemStack.EMPTY;
      cachedStacks[slot] = stack;
    }
    return stack;
  }


  /* Get and set */

  @Override
  public ItemResource getResource(int index) {
    return ItemResource.of(getCachedStack(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    // Cast as the product of two ints might overflow an int, but fits into a long
    return (long) access.getCount() * getCachedStack(index).getCount();
  }

  /**
   * Directly sets the contents of a slot, replacement for the old {@code setStackInSlot}. Used by menu slots, must not be called inside a transaction.
   * @param index     Slot index
   * @param resource  New item, or empty to clear the slot
   * @param amount    New amount across all tools in the access
   */
  @Override
  public void set(int index, ItemResource resource, int amount) {
    TransferPreconditions.checkNonNegative(amount);
    try (Transaction transaction = Transaction.openRoot()) {
      Edit edit = access.edit();
      if (edit == null || index < 0) {
        return;
      }
      IToolStackView tool = edit.tool();
      InventoryModifierHook inventory = findHook(tool, index);
      if (inventory != null) {
        ItemStack stack = resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(amount / edit.getCount());
        inventory.setStack(tool, indexEntry, index - startIndex, stack);
        if (edit.commit(transaction)) {
          transaction.commit();
        }
      }
    }
  }

  /**
   * Inserts the item into the given slot of the tool being edited
   * @param tool          Tool being edited
   * @param index         Slot index
   * @param resource      Item to insert
   * @param amountPerTool Amount to insert into each tool
   * @return  Amount inserted into each tool
   */
  private int insertInto(IToolStackView tool, int index, ItemResource resource, int amountPerTool) {
    InventoryModifierHook inventory = findHook(tool, index);
    if (inventory == null) {
      return 0;
    }
    // next, is the item valid for the slot?
    int localSlot = index - startIndex;
    if (!inventory.isItemValid(tool, indexEntry, localSlot, resource.toStack())) {
      return 0;
    }
    // do we have a stack?
    ItemStack current = inventory.getStack(tool, indexEntry, localSlot);
    int slotLimit = inventory.getSlotLimit(tool, indexEntry, localSlot);
    // nothing currently? place the item in
    if (current.isEmpty()) {
      int inserted = Math.min(amountPerTool, Math.min(resource.getMaxStackSize(), slotLimit));
      if (inserted > 0) {
        inventory.setStack(tool, indexEntry, localSlot, resource.toStack(inserted));
      }
      return Math.max(inserted, 0);
    }
    // space leftover? does it match?
    // the resource handler contract needs the slot to hold the inserted resource, so components must match
    int limit = Math.min(current.getMaxStackSize(), slotLimit);
    if (current.getCount() >= limit || !resource.matches(current)) {
      return 0;
    }
    int inserted = Math.min(amountPerTool, limit - current.getCount());
    // store new stack, current is a freshly parsed stack so its safe to modify
    current.grow(inserted);
    inventory.setStack(tool, indexEntry, localSlot, current); // update stack in NBT
    return inserted;
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // no nesting item handlers
    if (index < 0 || isBlacklisted(resource.toStack())) {
      return 0;
    }
    // first, do we have an inventory?
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    int inserted = insertInto(edit.tool(), index, resource, amountPerTool);
    if (inserted > 0 && edit.commit(transaction)) {
      return inserted * count;
    }
    return 0;
  }

  @Override
  public int insert(ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // no nesting item handlers
    if (isBlacklisted(resource.toStack())) {
      return 0;
    }
    // same as the default slot by slot insert, but changes the tool once instead of once per slot
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    IToolStackView tool = edit.tool();
    int slots = tool.getVolatileData().getInt(TOTAL_SLOTS);
    int inserted = 0;
    for (int index = 0; index < slots && inserted < amountPerTool; index++) {
      inserted += insertInto(tool, index, resource, amountPerTool - inserted);
    }
    if (inserted > 0 && edit.commit(transaction)) {
      return inserted * count;
    }
    return 0;
  }

  /**
   * Extracts the item from the given slot of the tool being edited
   * @param tool          Tool being edited
   * @param index         Slot index
   * @param resource      Item to extract
   * @param amountPerTool Amount to extract from each tool
   * @return  Amount extracted from each tool
   */
  private int extractFrom(IToolStackView tool, int index, ItemResource resource, int amountPerTool) {
    InventoryModifierHook inventory = findHook(tool, index);
    if (inventory == null) {
      return 0;
    }
    int localSlot = index - startIndex;
    // do we have the item in the slot?
    ItemStack current = inventory.getStack(tool, indexEntry, localSlot);
    if (current.isEmpty() || !resource.matches(current)) {
      return 0;
    }
    // they want more than we can give? just say no
    int extracted = Math.min(amountPerTool, current.getCount());
    if (extracted == current.getCount()) {
      inventory.setStack(tool, indexEntry, localSlot, ItemStack.EMPTY);
    } else {
      // current is a freshly parsed stack so its safe to modify
      current.shrink(extracted);
      inventory.setStack(tool, indexEntry, localSlot, current); // update in NBT
    }
    return extracted;
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // first, are you wasting our time?
    if (index < 0) {
      return 0;
    }
    // next, do we have an inventory?
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    int extracted = extractFrom(edit.tool(), index, resource, amountPerTool);
    if (extracted > 0 && edit.commit(transaction)) {
      return extracted * count;
    }
    return 0;
  }

  @Override
  public int extract(ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // same as the default slot by slot extract, but changes the tool once instead of once per slot
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    IToolStackView tool = edit.tool();
    int slots = tool.getVolatileData().getInt(TOTAL_SLOTS);
    int extracted = 0;
    for (int index = 0; index < slots && extracted < amountPerTool; index++) {
      extracted += extractFrom(tool, index, resource, amountPerTool - extracted);
    }
    if (extracted > 0 && edit.commit(transaction)) {
      return extracted * count;
    }
    return 0;
  }

  /** Interface for an inventory modifier to use */
  @SuppressWarnings("unused")
  public interface InventoryModifierHook {
    /** Gets the number of item slots used by the given tool. The number returned here must also be added into volatile data under {@link #TOTAL_SLOTS} */
    int getSlots(IToolStackView tool, ModifierEntry modifier);

    /** Sets the stack in the given slot */
    ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot);

    /** Sets the stack in the given slot */
    void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack);

    /** Gets the max stack size for the given slot */
    default int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
      return 64;
    }

    /** Checks if the item is valid for the given slot */
    default boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      return true;
    }

    /** Gets the pattern to render when the given slot is empty */
    @Nullable
    default Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
      return null;
    }

    /**
     * Finds a matching stack in the inventory
     * @param predicate  Predicate to test
     * @return Match containing slot and stack, or {@link StackMatch#EMPTY} if not found.
     */
    default StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
      for (int i = 0; i < getSlots(tool, modifier); i++) {
        ItemStack stack = getStack(tool, modifier, i);
        if (!stack.isEmpty() && predicate.test(stack)) {
          return new StackMatch(stack, i);
        }
      }
      return StackMatch.EMPTY;
    }

    /** Parses all stacks in NBT into the passed list */
    @CanIgnoreReturnValue
    default List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry modifier, List<ItemStack> stackList) {
      for (int i = 0; i < getSlots(tool, modifier); i++) {
        ItemStack stack = getStack(tool, modifier, i);
        if (!stack.isEmpty()) {
          stackList.add(stack);
        }
      }
      return stackList;
    }
  }

  /** Result of searching the inventory */
  public record StackMatch(ItemStack stack, int slot) {
    public static final StackMatch EMPTY = new StackMatch(ItemStack.EMPTY, -1);

    /** Checks if this match is empty */
    public boolean isEmpty() {
      return slot == -1;
    }
  }

  /** Merger for inventory modifier hooks */
  @RequiredArgsConstructor
  private static class InventoryModifierHookMerger extends InventoryModifierHookIterator<InventoryModifierHook> implements InventoryModifierHook {
    private final Collection<InventoryModifierHook> modules;

    @Override
    protected Iterator<InventoryModifierHook> getIterator(IToolStackView tool) {
      return modules.iterator();
    }

    @Override
    protected InventoryModifierHook getHook(InventoryModifierHook hook) {
      return hook;
    }

    /** Gets the inventory instance for the given slot index */
    @Nullable
    private InventoryModifierHook findHook(IToolStackView tool, ModifierEntry modifier, int slot) {
      indexEntry = modifier;
      return this.findHook(tool, slot);
    }

    @Override
    public int getSlots(IToolStackView tool, ModifierEntry modifier) {
      int sum = 0;
      for (InventoryModifierHook module : modules) {
        sum += module.getSlots(tool, modifier);
      }
      return sum;
    }

    @Override
    public ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.getStack(tool, modifier, slot - startIndex);
      }
      return ItemStack.EMPTY;
    }

    @Override
    public void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        module.setStack(tool, modifier, slot - startIndex, stack);
      }
    }

    @Override
    public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.getSlotLimit(tool, modifier, slot - startIndex);
      }
      return 0;
    }

    @Override
    public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.isItemValid(tool, modifier, slot - startIndex, stack);
      }
      return false;
    }

    @Nullable
    @Override
    public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.getPattern(tool, modifier, slot - startIndex, hasStack);
      }
      return null;
    }

    @Override
    public StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
      int start = 0;
      for (InventoryModifierHook module : modules) {
        StackMatch match = module.findStack(tool, modifier, predicate);
        if (!match.isEmpty()) {
          return start == 0 ? match : new StackMatch(match.stack, match.slot + start);
        }
        start += module.getSlots(tool, modifier);
      }
      return StackMatch.EMPTY;
    }

    @Override
    public List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry modifier, List<ItemStack> stackList) {
      for (InventoryModifierHook module : modules) {
        module.getAllStacks(tool, modifier, stackList);
      }
      return stackList;
    }
  }

  /** Provider for an inventory tool capability */
  public static class Provider implements IToolCapabilityProvider {
    @SuppressWarnings("unused")
    public Provider(ItemStack stack, Supplier<? extends IToolStackView> tool) {}

    @Nullable
    @Override
    public <T> T getCapability(IToolStackView tool, ItemCapability<T, ?> cap) {
      // the item capability needs the item access, see the context version
      return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T, C> T getCapability(IToolStackView tool, ItemCapability<T, C> cap, @Nullable C context) {
      if (cap == Capabilities.Item.ITEM && context instanceof ItemAccess access && tool.getVolatileData().getInt(TOTAL_SLOTS) > 0) {
        return (T) new ToolInventoryCapability(access);
      }
      return null;
    }
  }


  /* Helpers */

  /** Adds the given number of slots to the data */
  public static void addSlots(ModDataNBT volatileData, int count) {
    volatileData.putInt(TOTAL_SLOTS, volatileData.getInt(TOTAL_SLOTS) + count);
  }

  /**
   * Gets the item handler for the given tool stack. The handler changes the passed stack in place when a transaction commits,
   * keeping the stack instance, which the tool container relies on.
   * @param stack  Tool stack
   * @return  Item handler, or null if the stack has no tool inventory
   */
  @Nullable
  public static ResourceHandler<ItemResource> getHandler(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }
    return stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack));
  }


  /** Opens the tool inventory container if an inventory is present on the given tool */
  public static InteractionResult tryOpenContainer(ItemStack stack, IToolStackView tool, Player player, EquipmentSlot slotType) {
    return tryOpenContainer(stack, tool, tool.getDefinition(), player, slotType);
  }

  /** Opens the tool inventory container if an inventory is present on the given tool */
  public static InteractionResult tryOpenContainer(ItemStack stack, @Nullable IToolStackView tool, ToolDefinition definition, Player player, EquipmentSlot slotType) {
    return tryOpenContainer(stack, tool, definition, player, switch (slotType) {
      // mainhand is the hotbar selected slot
      case MAINHAND -> player.getInventory().getSelectedSlot();
      // offhand is its own slot
      case OFFHAND -> Inventory.SLOT_OFFHAND;
      // armor starts from the end of inventory
      default -> Inventory.INVENTORY_SIZE + slotType.getIndex();
    });
  }

  /** Opens the tool inventory container if an inventory is present on the given tool */
  public static InteractionResult tryOpenContainer(ItemStack stack, @Nullable IToolStackView tool, ToolDefinition definition, Player player, int slotIndex) {
    // the handler writes to the stack in place, so the menu can keep checking the stack instance is still in the slot
    ResourceHandler<ItemResource> rawHandler = getHandler(stack);
    ResourceHandler<ItemResource> handler = rawHandler != null ? rawHandler : EmptyResourceHandler.<ItemResource>instance();
    // open if we have any slots or we have a crafting table
    if (handler.size() > 0 || ModifierUtil.checkVolatileFlag(stack, CRAFTING_TABLE) || ModifierUtil.checkVolatileFlag(stack, INVENTORY_CRAFTING)) {
      if (player instanceof ServerPlayer serverPlayer) {
        serverPlayer.openMenu(new SimpleMenuProvider(
          (id, inventory, p) -> new ToolContainerMenu(id, inventory, stack, handler, slotIndex),
          ToolNameHook.getName(definition, stack, tool)
        ), buf -> {
          buf.writeVarInt(slotIndex);
          ToolSyncType syncType = Config.COMMON.toolInventorySync.get();
          buf.writeEnum(syncType);
          if (syncType == ToolSyncType.FULL_STACK) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
          } else if (syncType == ToolSyncType.MINIMAL) {
            buf.writeVarInt(ModifierUtil.getVolatileInt(stack, TOTAL_SLOTS));
            buf.writeEnum(CraftingType.fromStack(stack));
            buf.writeBoolean(ModifierUtil.checkVolatileFlag(stack, INCLUDE_OFFHAND));
          }
        });
      }
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }

  /** Called when a tool item entity is destroyed to drop its inventory items. */
  public static void onDestroyed(ItemEntity entity) {
    if (!entity.level().isClientSide()) {
      ResourceHandler<ItemResource> handler = getHandler(entity.getItem());
      if (handler != null && handler.size() > 0) {
        ItemUtils.onContainerDestroyed(entity, IntStream.range(0, handler.size()).mapToObj(slot -> ItemUtil.getStack(handler, slot)).filter(stack -> !stack.isEmpty()));
      }
    }
  }

  /** Iterator that goes through a list in reverse order */
  private static class ReversedListIterator<T> implements Iterator<T> {
    /** List to iterate */
    private final List<T> list;
    /** Index of the next element */
    private int index;

    public ReversedListIterator(List<T> list) {
      this.list = list;
      this.index = list.size() - 1;
    }

    @Override
    public boolean hasNext() {
      return index >= 0;
    }

    @Override
    public T next() {
      T element = list.get(index);
      index--;
      return element;
    }
  }


  /** Crafting table type for the UI */
  public enum CraftingType {
    FULL, INVENTORY, NONE;

    /** Gets the crafting type for the given stack */
    public static CraftingType fromStack(ItemStack stack) {
      if (ModifierUtil.checkVolatileFlag(stack, CRAFTING_TABLE)) {
        return FULL;
      }
      if (ModifierUtil.checkVolatileFlag(stack, INVENTORY_CRAFTING)) {
        return INVENTORY;
      }
      return NONE;
    }
  }
}
