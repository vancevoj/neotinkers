package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierTraitModule;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider.IToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.ToolItemAccess.Edit;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.CapacityStat;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Standard implementation of energy capability on a tool. Not currently used in the mod directly, but should help addons have more unity.
 * <p>
 * In 26.3 this is an {@link EnergyHandler} built from the {@link ItemAccess} of the {@link Capabilities.Energy#ITEM} query. Changes are written
 * back to the tool data of the accessed stack through the access as part of the transaction, and amounts scale with the number of tools in the access.
 */
public class ToolEnergyCapability implements EnergyHandler {
  /** Format string to display energy amounts, used internally by the stat */
  public static final String ENERGY_FORMAT = TConstruct.makeDescriptionId("tool_stat", "energy");
  /** Stat marking the max capacity */
  public static final CapacityStat MAX_STAT = new CapacityStat(new ToolStatId(TConstruct.MOD_ID, "max_energy"), 0xa00000, ENERGY_FORMAT);
  /** Persistent data key for fetching the current energy */
  public static final Identifier ENERGY_KEY = TConstruct.getResource("energy");
  /** Include this module in a modifier adding energy capacity or functionality to ensure capacity changes are properly cleaned up */
  public static final ModifierModule ENERGY_HANDLER = new ModifierTraitModule(TinkerModifiers.energyHandler.getId(), 1, true);

  /** Access to the tool */
  private final ToolItemAccess access;

  public ToolEnergyCapability(ItemAccess access) {
    this.access = new ToolItemAccess(access);
  }

  /** Gets the energy capacity for the given tool */
  public static int getMaxEnergy(IToolStackView tool) {
    return tool.getStats().getInt(MAX_STAT);
  }

  /** Gets the current energy on a tool */
  public static int getEnergy(IToolStackView tool) {
    return tool.getPersistentData().getInt(ENERGY_KEY);
  }

  /** Internal method to set energy, skips a capacity check */
  private static void setEnergyRaw(IToolStackView tool, int energy) {
    if (energy == 0) {
      tool.getPersistentData().remove(ENERGY_KEY);
    } else {
      tool.getPersistentData().putInt(ENERGY_KEY, energy);
    }
  }

  /** Sets the energy on the tool */
  public static void setEnergy(IToolStackView tool, int energy) {
    setEnergyRaw(tool, Mth.clamp(energy, 0, getMaxEnergy(tool)));
  }

  /** Adds the given amount of energy to the tool. Can use negative to subtract energy. */
  public static void addEnergy(IToolStackView tool, int energy) {
    if (energy != 0) {
      setEnergy(tool, getEnergy(tool) + energy);
    }
  }

  /** Ensures the tool's energy is within the cap. Generally not necessary to call this directly as it's called by {@link #ENERGY_HANDLER} on tool change. */
  public static void checkEnergy(IToolStackView tool) {
    int energy = ToolEnergyCapability.getEnergy(tool);
    if (energy < 0) {
      ToolEnergyCapability.setEnergyRaw(tool, 0);
    } else {
      int capacity = ToolEnergyCapability.getMaxEnergy(tool);
      if (energy > capacity) {
        ToolEnergyCapability.setEnergyRaw(tool, capacity);
      }
    }
  }

  @Override
  public int insert(int maxReceive, TransactionContext transaction) {
    TransferPreconditions.checkNonNegative(maxReceive);
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    // fill each tool in the stack with the same amount
    int count = edit.getCount();
    int receivePerTool = maxReceive / count;
    if (receivePerTool <= 0) {
      return 0;
    }
    IToolStackView tool = edit.tool();
    int current = getEnergy(tool);
    int filled = Math.min(getMaxEnergy(tool) - current, receivePerTool);
    if (filled > 0) {
      setEnergyRaw(tool, current + filled);
      if (edit.commit(transaction)) {
        return filled * count;
      }
    }
    return 0;
  }

  @Override
  public int extract(int maxExtract, TransactionContext transaction) {
    TransferPreconditions.checkNonNegative(maxExtract);
    Edit edit = access.edit();
    if (edit == null) {
      return 0;
    }
    int count = edit.getCount();
    int extractPerTool = maxExtract / count;
    if (extractPerTool <= 0) {
      return 0;
    }
    IToolStackView tool = edit.tool();
    int current = getEnergy(tool);
    if (current <= 0) {
      return 0;
    }
    int drained = extractPerTool;
    if (current < drained) {
      drained = current;
    }
    setEnergyRaw(tool, current - drained);
    if (edit.commit(transaction)) {
      return drained * count;
    }
    return 0;
  }

  @Override
  public long getAmountAsLong() {
    IToolStackView tool = access.getTool();
    if (tool == null) {
      return 0;
    }
    // Cast as the product of two ints might overflow an int, but fits into a long
    return (long) access.getCount() * getEnergy(tool);
  }

  @Override
  public long getCapacityAsLong() {
    IToolStackView tool = access.getTool();
    if (tool == null) {
      return 0;
    }
    return (long) access.getCount() * getMaxEnergy(tool);
  }

  /** Provider instance for an energy cap */
  public static class Provider implements IToolCapabilityProvider {
    @SuppressWarnings("unused")
    public Provider(Supplier<? extends IToolStackView> toolStack) {}

    @Nullable
    @Override
    public <T> T getCapability(IToolStackView tool, ItemCapability<T, ?> cap) {
      // the energy capability needs the item access, see the context version
      return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T, C> T getCapability(IToolStackView tool, ItemCapability<T, C> cap, @Nullable C context) {
      if (cap == Capabilities.Energy.ITEM && context instanceof ItemAccess access && tool.getStats().getInt(MAX_STAT) > 0) {
        return (T) new ToolEnergyCapability(access);
      }
      return null;
    }
  }
}
