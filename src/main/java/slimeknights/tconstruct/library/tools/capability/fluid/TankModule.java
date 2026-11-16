package slimeknights.tconstruct.library.tools.capability.fluid;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability.FluidModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Standard implementation of a tank module using the {@link ToolTankHelper}.
 * Feel free to request abstracting out an aspect of it if you wish to have less duplication in a non-standard implementation.
 * Unregistered as modifiers have no way to register new tool stats.
 */
@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public class TankModule implements HookProvider, FluidModifierHook, VolatileDataModifierHook, ValidateModifierHook, ModifierRemovalHook, SlotStackModifierHook, DisplayNameModifierHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<TankModule>defaultHooks(ToolFluidCapability.HOOK, ModifierHooks.VOLATILE_DATA, ModifierHooks.VALIDATE, ModifierHooks.REMOVE, ModifierHooks.SLOT_STACK, ModifierHooks.DISPLAY_NAME);


  /** Helper handling updating fluids */
  private final ToolTankHelper helper;


  /* Module logic */

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
    ToolFluidCapability.addTanks(modifier, volatileData, this);
  }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, Component name, @Nullable RegistryAccess access) {
    FluidStack fluid = helper.getFluid(tool);
    int capacity = helper.getCapacity(tool);
    if (fluid.isEmpty()) {
      // no fluid, display as: Tank Capacity: #,### mb
      return ToolTankHelper.CAPACITY_STAT.formatValue(capacity);
    } else {
      // fluid, display as: Fluid Name: #,### / #,### mb
      return fluid.getHoverName().copy()
                  .append(": ")
                  .append(ToolTankHelper.CAPACITY_STAT.formatContents(fluid.getAmount(), capacity));
    }
  }

  @Override
  public int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
    return helper.getCapacity(tool);
  }

  @Override
  public FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
    return helper.getFluid(tool);
  }


  /* Cleanup */

  @Nullable
  @Override
  public Component validate(IToolStackView tool, ModifierEntry modifier) {
    FluidStack fluid = helper.getFluid(tool);
    int capacity = helper.getCapacity(tool);
    if (fluid.getAmount() > capacity) {
      fluid.setAmount(capacity);
      helper.setFluid(tool, fluid);
    }
    return null;
  }

  @Nullable
  @Override
  public Component onRemoved(IToolStackView tool, Modifier modifier) {
    helper.setFluid(tool, FluidStack.EMPTY);
    return null;
  }


  /* Filling and draining */
  // the tool is a private copy written back by the capability when the transaction commits, so changes are always applied

  @Override
  public int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction) {
    // make sure this modifier is in charge of the tank, that is first come first serve
    if (!resource.isEmpty()) {
      // if empty, just directly fill, setFluid will check capacity
      FluidStack current = helper.getFluid(tool);
      int capacity = helper.getCapacity(tool);
      if (current.isEmpty()) {
        int filled = Math.min(resource.getAmount(), capacity);
        if (filled > 0) {
          helper.setFluid(tool, resource);
        }
        return filled;
      }
      // if the fluid matches and we have space, update
      if (current.getAmount() < capacity && FluidStack.isSameFluidSameComponents(current, resource)) {
        int filled = Math.min(resource.getAmount(), capacity - current.getAmount());
        if (filled > 0) {
          current.grow(filled);
          helper.setFluid(tool, current);
        }
        return filled;
      }
    }
    return 0;
  }

  @Override
  public FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, TransactionContext transaction) {
    modifier.getId();
    if (!resource.isEmpty()) {
      // ensure we have something and it matches the request
      FluidStack current = helper.getFluid(tool);
      if (!current.isEmpty() && FluidStack.isSameFluidSameComponents(current, resource)) {
        // create the drained stack
        FluidStack drained = current.copyWithAmount(Math.min(current.getAmount(), resource.getAmount()));
        // remove it from the tank
        if (drained.getAmount() == current.getAmount()) {
          helper.setFluid(tool, FluidStack.EMPTY);
        } else {
          current.shrink(drained.getAmount());
          helper.setFluid(tool, current);
        }
        return drained;
      }
    }
    return FluidStack.EMPTY;
  }

  @Override
  public FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, TransactionContext transaction) {
    modifier.getId();
    if (maxDrain > 0) {
      // ensure we have something and it matches the request
      FluidStack current = helper.getFluid(tool);
      if (!current.isEmpty()) {
        // create the drained stack
        FluidStack drained = current.copyWithAmount(Math.min(current.getAmount(), maxDrain));
        // remove it from the tank
        if (drained.getAmount() == current.getAmount()) {
          helper.setFluid(tool, FluidStack.EMPTY);
        } else {
          current.shrink(drained.getAmount());
          helper.setFluid(tool, current);
        }
        return drained;
      }
    }
    return FluidStack.EMPTY;
  }


  /* Inventory slot stacking */

  /** Gets a single tank handler holding the fluid of the given tool, read back after the transfer using {@link FluidUtil#getStack} */
  private FluidStacksResourceHandler getTank(IToolStackView tool) {
    FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, helper.getCapacity(tool));
    FluidStack fluid = helper.getFluid(tool);
    if (!fluid.isEmpty()) {
      tank.set(0, FluidResource.of(fluid), fluid.getAmount());
    }
    return tank;
  }

  @Override
  public boolean overrideStackedOnOther(IToolStackView heldTool, ModifierEntry modifier, Slot slot, Player player) {
    ItemStack slotStack = slot.getItem();
    // must have something with possible fluid in the slot
    if (!slotStack.isEmpty() && TankItem.mayHaveFluid(slotStack)) {
      // target must be stack size 1, if not then its not safe to modify it
      if (slotStack.getCount() == 1) {
        FluidStacksResourceHandler tank = getTank(heldTool);
        TransferResult result = FluidTransferHelper.interactWithStack(tank, slotStack, TransferDirection.REVERSE);
        // update held tank and slot item if something changed (either we have a result or the stack in the slot was shrunk)
        if (result != null) {
          if (player.level().isClientSide()) {
            player.playSound(result.getSound());
          }
          helper.setFluid(heldTool, FluidUtil.getStack(tank, 0));
          slot.set(FluidTransferHelper.getOrTransferFilled(player, slotStack, result.stack()));
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean overrideOtherStackedOnMe(IToolStackView slotTool, ModifierEntry modifier, ItemStack held, Slot slot, Player player, SlotAccess access) {
    // must have something with possible fluid held
    if (!held.isEmpty() && TankItem.mayHaveFluid(held)) {
      FluidStacksResourceHandler tank = getTank(slotTool);
      TransferResult result = FluidTransferHelper.interactWithStack(tank, held, TransferDirection.AUTO);
      // update tank if something happened
      if (result != null) {
        if (player.level().isClientSide()) {
          player.playSound(result.getSound());
        }
        helper.setFluid(slotTool, FluidUtil.getStack(tank, 0));
        // update held item, assuming its actually held
        TankItem.updateHeldItem(player, held, result.stack());
      }

      return true;
    }
    return false;
  }
}
