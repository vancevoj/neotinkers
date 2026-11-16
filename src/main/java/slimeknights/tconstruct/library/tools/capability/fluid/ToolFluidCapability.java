package slimeknights.tconstruct.library.tools.capability.fluid;

import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider.IToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.ToolItemAccess;
import slimeknights.tconstruct.library.tools.capability.ToolItemAccess.Edit;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Logic to make a tool a fluid handler.
 * <p>
 * In 26.3 this is a {@link ResourceHandler} of {@link FluidResource} built from the {@link ItemAccess} of the {@link Capabilities.Fluid#ITEM} query.
 * Each change runs the {@link FluidModifierHook}s on a private copy of the tool and exchanges the changed tool into the access in the transaction,
 * so the tool data of the accessed stack only changes when the transaction commits. Amounts scale with the number of tools in the access,
 * like other item access handlers.
 */
public class ToolFluidCapability extends FluidModifierHookIterator<ModifierEntry> implements ResourceHandler<FluidResource> {
  /** Boolean key to set in volatile mod data to enable the fluid capability */
  public static final Identifier TOTAL_TANKS = TConstruct.getResource("total_tanks");

  /** Modifier hook instance to make an inventory modifier */
  public static final ModuleHook<FluidModifierHook> HOOK = ModifierHooks.register(TConstruct.getResource("fluid"), FluidModifierHook.class, FluidModifierHookMerger::new, new FluidModifierHook() {
    @Override
    public int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      return false;
    }

    @Override
    public int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction) {
      return 0;
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction) {
      return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, TransactionContext transaction) {
      return FluidStack.EMPTY;
    }
  });

  /** Access to the tool */
  private final ToolItemAccess access;

  public ToolFluidCapability(ItemAccess access) {
    this.access = new ToolItemAccess(access);
  }

  /* Basic inventory */

  @Override
  public int size() {
    IToolStackView tool = access.getTool();
    if (tool == null) {
      return 0;
    }
    return tool.getVolatileData().getInt(TOTAL_TANKS);
  }

  @Override
  protected Iterator<ModifierEntry> getIterator(IToolStackView tool) {
    return tool.getModifierList().iterator();
  }

  @Override
  protected FluidModifierHook getHook(ModifierEntry entry) {
    indexEntry = entry;
    return entry.getHook(HOOK);
  }

  /** Gets the fluid in the given tank for a single tool */
  private FluidStack getFluidInTank(int tank) {
    IToolStackView tool = access.getTool();
    if (tool != null && tank >= 0) {
      FluidModifierHook hook = findHook(tool, tank);
      if (hook != null) {
        return hook.getFluidInTank(tool, indexEntry, tank - startIndex);
      }
    }
    return FluidStack.EMPTY;
  }

  @Override
  public FluidResource getResource(int index) {
    return FluidResource.of(getFluidInTank(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    // Cast as the product of two ints might overflow an int, but fits into a long
    return (long) access.getCount() * getFluidInTank(index).getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    if (!resource.isEmpty() && !isValid(index, resource)) {
      return 0;
    }
    IToolStackView tool = access.getTool();
    if (tool != null && index >= 0) {
      FluidModifierHook hook = findHook(tool, index);
      if (hook != null) {
        return (long) access.getCount() * hook.getTankCapacity(tool, indexEntry, index - startIndex);
      }
    }
    return 0;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    IToolStackView tool = access.getTool();
    if (tool != null && index >= 0 && !resource.isEmpty()) {
      FluidModifierHook hook = findHook(tool, index);
      if (hook != null) {
        // hooks take a stack, the amount does not matter for validity
        return hook.isFluidValid(tool, indexEntry, index - startIndex, resource.toStack(FluidType.BUCKET_VOLUME));
      }
    }
    return false;
  }


  /* Filling */

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    Edit edit = access.edit();
    if (edit == null || index < 0) {
      return 0;
    }
    // fill each tool in the stack with the same amount
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    IToolStackView tool = edit.tool();
    FluidModifierHook hook = findHook(tool, index);
    if (hook != null) {
      int filled = hook.fill(tool, indexEntry, resource.toStack(amountPerTool), transaction);
      if (filled > 0 && edit.commit(transaction)) {
        return filled * count;
      }
    }
    return 0;
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // index-less insert keeps the old fill distribution across modifiers
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    int filled = fill(edit.tool(), resource.toStack(amountPerTool), transaction);
    if (filled > 0 && edit.commit(transaction)) {
      return filled * count;
    }
    return 0;
  }


  /* Draining */

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    Edit edit = access.edit();
    if (edit == null || index < 0) {
      return 0;
    }
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    IToolStackView tool = edit.tool();
    FluidModifierHook hook = findHook(tool, index);
    if (hook != null) {
      FluidStack drained = hook.drain(tool, indexEntry, resource.toStack(amountPerTool), transaction);
      // hooks only drain a matching fluid, but be safe as the result must be the requested resource
      if (!drained.isEmpty() && resource.matches(drained) && edit.commit(transaction)) {
        return drained.getAmount() * count;
      }
    }
    return 0;
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // index-less extract keeps the old drain distribution across modifiers
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    int count = edit.getCount();
    int amountPerTool = amount / count;
    if (amountPerTool <= 0) {
      return 0;
    }
    FluidStack drained = drain(edit.tool(), resource.toStack(amountPerTool), transaction);
    if (!drained.isEmpty() && resource.matches(drained) && edit.commit(transaction)) {
      return drained.getAmount() * count;
    }
    return 0;
  }

  /**
   * Drains any fluid from the tool, replacement for the old {@code drain(int, FluidAction)}. Generic code should use
   * {@link net.neoforged.neoforge.transfer.ResourceHandlerUtil#extractFirst} on the capability, this method keeps the type insensitive
   * {@link FluidModifierHook#drain(IToolStackView, ModifierEntry, int, TransactionContext)} hook.
   * @param maxDrain     Maximum amount to drain across all tools in the access
   * @param transaction  Transaction for the change
   * @return  Fluid drained across all tools in the access
   */
  public FluidStack drain(int maxDrain, TransactionContext transaction) {
    TransferPreconditions.checkNonNegative(maxDrain);
    Edit edit = access.edit();
    if (edit == null) {
      return FluidStack.EMPTY;
    }
    int count = edit.getCount();
    int amountPerTool = maxDrain / count;
    if (amountPerTool <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack drained = drain(edit.tool(), amountPerTool, transaction);
    if (!drained.isEmpty() && edit.commit(transaction)) {
      return drained.copyWithAmount(drained.getAmount() * count);
    }
    return FluidStack.EMPTY;
  }

  /** Adds the tanks from the fluid modifier to the tool */
  public static void addTanks(ModifierEntry modifier, ModDataNBT volatileData, FluidModifierHook hook) {
    volatileData.putInt(TOTAL_TANKS, hook.getTanks(volatileData, modifier) + volatileData.getInt(TOTAL_TANKS));
  }

  /**
   * Interface for modifiers with fluid capabilities to return.
   * <p>
   * In 26.3 the tool passed to {@link #fill(IToolStackView, ModifierEntry, FluidStack, TransactionContext)} and the drain methods is a private copy
   * of the tool in the item access, which is written back as part of the transaction. Hooks should therefore always apply their changes to the tool,
   * simulating is done by the caller not committing the transaction. The transaction is passed along for hooks that interact with other handlers.
   * @deprecated We are considering removing this interface in favor of a much simpler tool tank implementation.
   * For most use-cases {@link ToolTankHelper} is sufficient. If it does not cover your usecase, please leave a comment on <a href="https://github.com/SlimeKnights/TinkersConstruct/issues/5353">GitHub #5353</a>.
   */
  @SuppressWarnings("unused")
  @Deprecated
  public interface FluidModifierHook {
    /**
     * Determines how many fluid tanks are used by this modifier
     * @param volatileData  Tool data to check
     * @param modifier      Modifier to consider
     * @return  Number of tanks used
     */
    default int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      return 1;
    }

    /**
     * Gets the fluid in the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      return FluidStack.EMPTY;
    }

    /**
     * Gets the max capacity for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      return 0;
    }

    /**
     * Checks if the fluid is valid for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @param fluid  Fluid to insert
     * @return  True if the fluid is valid
     */
    default boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      return true;
    }

    /**
     * Fills fluid into tanks
     * @param tool         Tool instance, a private copy written back when the transaction commits, so always apply the change
     * @param modifier     Entry instance
     * @param resource     FluidStack representing the Fluid and maximum amount of fluid to be filled. If you want to store this stack, make a copy
     * @param transaction  Transaction this fill is part of. Simulating is done by not committing it.
     * @return Amount of resource that was filled.
     */
    int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction);

    /**
     * Drains fluid out of tanks, distribution is left entirely to the hook.
     * @param tool         Tool instance, a private copy written back when the transaction commits, so always apply the change
     * @param modifier     Entry instance
     * @param resource     FluidStack representing the Fluid and maximum amount of fluid to be drained.
     * @param transaction  Transaction this drain is part of. Simulating is done by not committing it.
     * @return FluidStack representing the Fluid and amount that was drained.
     */
    FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction);

    /**
     * Drains fluid out of internal tanks, distribution is left entirely to the hook.
     * @param tool         Tool instance, a private copy written back when the transaction commits, so always apply the change
     * @param modifier     Entry instance
     * @param maxDrain     Maximum amount of fluid to drain.
     * @param transaction  Transaction this drain is part of. Simulating is done by not committing it.
     * @return FluidStack representing the Fluid and amount that was drained.
     */
    FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, TransactionContext transaction);
  }

  /** Logic to merge multiple fluid hooks */
  @RequiredArgsConstructor
  private static class FluidModifierHookMerger extends FluidModifierHookIterator<FluidModifierHook> implements FluidModifierHook {
    private final Collection<FluidModifierHook> modules;

    @Override
    protected Iterator<FluidModifierHook> getIterator(IToolStackView tool) {
      return modules.iterator();
    }

    @Override
    protected FluidModifierHook getHook(FluidModifierHook entry) {
      return entry;
    }

    /** Gets the given hook */
    @Nullable
    private FluidModifierHook findHook(IToolStackView tool, ModifierEntry modifier, int tank) {
      indexEntry = modifier;
      return this.findHook(tool, tank);
    }

    @Override
    public int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      int sum = 0;
      for (FluidModifierHook module : modules) {
        sum += module.getTanks(volatileData, modifier);
      }
      return sum;
    }

    @Override
    public FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getFluidInTank(tool, modifier, tank - startIndex);
      }
      return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getTankCapacity(tool, modifier, tank - startIndex);
      }
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.isFluidValid(tool, modifier, tank - startIndex, fluid);
      }
      return false;
    }

    @Override
    public int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction) {
      indexEntry = modifier;
      return fill(tool, resource, transaction);
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction) {
      indexEntry = modifier;
      return drain(tool, resource, transaction);
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, TransactionContext transaction) {
      indexEntry = modifier;
      return drain(tool, maxDrain, transaction);
    }
  }

  /** Provider instance for a fluid cap */
  public static class Provider implements IToolCapabilityProvider {
    @SuppressWarnings("unused")
    public Provider(ItemStack stack, Supplier<? extends IToolStackView> toolStack) {}

    @Nullable
    @Override
    public <T> T getCapability(IToolStackView tool, ItemCapability<T, ?> cap) {
      // the fluid capability needs the item access, see the context version
      return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T, C> T getCapability(IToolStackView tool, ItemCapability<T, C> cap, @Nullable C context) {
      if (cap == Capabilities.Fluid.ITEM && context instanceof ItemAccess access && tool.getVolatileData().getInt(TOTAL_TANKS) > 0) {
        return (T) new ToolFluidCapability(access);
      }
      return null;
    }
  }
}
