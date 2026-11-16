package slimeknights.tconstruct.library.tools.capability;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

/**
 * Helper reading and writing a tool through an {@link ItemAccess}, shared by the tool item capabilities
 * ({@link slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability},
 * {@link slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability}, {@link ToolEnergyCapability}).
 * <p>
 * Item capabilities no longer receive a stack they may mutate. Instead, reads come from the current {@link ItemAccess#getResource()} and
 * changes are made to a private copy of the tool, which is then exchanged into the access as part of the transaction.
 * Simulating is done by not committing the transaction, so the tool data in the accessed stack (its {@link ToolDataComponents#TOOL_DATA}
 * component) only changes when the transaction commits.
 */
public class ToolItemAccess {
  /** Item access backing this tool */
  @Getter
  private final ItemAccess access;
  /** Item in the access when created. If the item changes, the tool capability no longer applies */
  @Getter
  private final Item item;

  /** Tool data compound the cached tool was parsed from, used to detect changes to the access */
  @Nullable
  private CompoundTag cachedTag;
  /** Cached read only tool */
  @Nullable
  private ToolStack cachedTool;

  public ToolItemAccess(ItemAccess access) {
    this.access = access;
    this.item = access.getResource().getItem();
  }

  /** Gets the current resource in the access, or empty if the item changed */
  public ItemResource getResource() {
    ItemResource resource = access.getResource();
    if (resource.isEmpty() || !resource.is(item)) {
      return ItemResource.EMPTY;
    }
    return resource;
  }

  /** Gets the number of tools in the access, 0 if the item changed. Amounts reported by tool capabilities scale with this count */
  public int getCount() {
    ItemResource resource = access.getResource();
    if (resource.isEmpty() || !resource.is(item)) {
      return 0;
    }
    return access.getAmount();
  }

  /**
   * Gets a view of the tool currently in the access, or null if the item changed.
   * The view is a copy that is reused while the tool data is unchanged, so changes made to it are never saved; use {@link #edit()} to change the tool.
   */
  @Nullable
  public IToolStackView getTool() {
    ItemResource resource = getResource();
    if (resource.isEmpty()) {
      cachedTool = null;
      cachedTag = null;
      return null;
    }
    // component values are not mutated after being set, so an identity check is enough to know the data is unchanged
    CompoundTag tag = ToolDataComponents.getTag(resource);
    if (cachedTool == null || tag != cachedTag) {
      cachedTool = ToolStack.copyFrom(resource.toStack());
      cachedTag = tag;
    }
    return cachedTool;
  }

  /**
   * Starts editing the tool in the access. Make changes to {@link Edit#tool()}, then call {@link Edit#commit(TransactionContext)}.
   * @return  Edit instance, or null if the access is empty or the item changed
   */
  @Nullable
  public Edit edit() {
    ItemResource original = getResource();
    int count = access.getAmount();
    if (original.isEmpty() || count <= 0) {
      return null;
    }
    ItemStack stack = original.toStack(count);
    return new Edit(original, count, stack, ToolStack.from(stack));
  }

  /** In progress change to a tool in the access */
  public class Edit {
    /** Resource in the access when the edit started */
    private final ItemResource original;
    /** Number of tools being edited */
    @Getter
    private final int count;
    /** Private copy of the accessed stack, receives all changes to {@link #tool} */
    private final ItemStack stack;
    /** Tool to modify, writes changes to {@link #stack} */
    private final ToolStack tool;

    private Edit(ItemResource original, int count, ItemStack stack, ToolStack tool) {
      this.original = original;
      this.count = count;
      this.stack = stack;
      this.tool = tool;
    }

    /** Gets the tool to modify */
    public IToolStackView tool() {
      return tool;
    }

    /**
     * Writes the changed tool back to the access in the given transaction
     * @param transaction  Transaction for the change, the access only changes if this commits
     * @return  True if the change was accepted, false if the access refused the changed tool
     */
    public boolean commit(TransactionContext transaction) {
      ItemResource updated = ItemResource.of(stack);
      // nothing changed? nothing to write
      if (updated.equals(original)) {
        return true;
      }
      return access.exchange(updated, count, transaction) == count;
    }
  }
}
