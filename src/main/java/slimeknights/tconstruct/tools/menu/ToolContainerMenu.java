package slimeknights.tconstruct.tools.menu;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.inventory.EmptyItemHandler;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config.ToolSyncType;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.SimpleFluidTank;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.CraftingType;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.network.ToolContainerFluidUpdatePacket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Container for a tool inventory */
public class ToolContainerMenu extends AbstractContainerMenu {
  /** Size of a single slot */
  public static final int SLOT_SIZE = 18;
  /** Y start of the repeat slots background */
  public static final int TITLE_SIZE = 13;
  /** Y start of the repeat slots background */
  public static final int UI_START = 4;
  /** Max number of rows in the repeat slots background */
  public static final int REPEAT_BACKGROUND_START = UI_START + TITLE_SIZE;

  /** Stack containing the tool being rendered */
  @Getter
  private final ItemStack stack;
  /**
   * Tool hosting this tank. The inventory handler commits into the same stack, so call {@link ToolStack#refreshFromStack()}
   * before using this view, otherwise a stale copy of the tool data could be written back
   */
  @Getter
  private final ToolStack tool;
  /** Item handler being rendered */
  @Getter
  private final ResourceHandler<ItemResource> itemHandler;
  /** Tank in the tool */
  @Getter
  private final SimpleFluidTank tank;
  @Getter
  private final Player player;
  @Getter
  private final int slotIndex;
  @Getter
  private final boolean showOffhand;
  @Nullable
  private final CraftingContainer craftingContainer;
  @Nullable
  private final ResultContainer resultContainer;
  /** Start index of the tool slots */
  @Getter
  private final int toolInventoryStart;
  /** Index of the first player inventory slot */
  @Getter
  private final int playerInventoryStart;

  public ToolContainerMenu(int id, Inventory playerInventory, ItemStack stack, ResourceHandler<ItemResource> itemHandler, int slotIndex) {
    this(TinkerTools.toolContainer.get(), id, playerInventory, stack, itemHandler, slotIndex);
  }

  // TODO 1.21: probably ditch this constructor
  protected ToolContainerMenu(@Nullable MenuType<?> type, int id, Inventory playerInventory, ItemStack stack, ResourceHandler<ItemResource> handler, int slotIndex) {
    this(type, id, playerInventory, stack, handler, slotIndex, CraftingType.fromStack(stack), ModifierUtil.checkVolatileFlag(stack, ToolInventoryCapability.INCLUDE_OFFHAND));
  }

  /** Creates a new instance of this container on the client side */
  public static ToolContainerMenu forClient(int id, Inventory inventory, FriendlyByteBuf buffer) {
    int slotIndex = buffer.readVarInt();
    ToolSyncType syncType = buffer.readEnum(ToolSyncType.class);
    // when syncing the full stack, overwrite the spot in the inventory
    ItemStack stack;
    if (syncType == ToolSyncType.FULL_STACK) {
      stack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
      inventory.setItem(slotIndex, stack);
    } else {
      stack = inventory.getItem(slotIndex);
    }

    // with minimal syncing, key details will be in the packet
    CraftingType craftingType;
    boolean includeOffhand;
    int size;
    if (syncType == ToolSyncType.MINIMAL) {
      size = buffer.readVarInt();
      craftingType = buffer.readEnum(CraftingType.class);
      includeOffhand = buffer.readBoolean();
    } else {
      size = ModifierUtil.getVolatileInt(stack, ToolInventoryCapability.TOTAL_SLOTS);
      craftingType = CraftingType.fromStack(stack);
      includeOffhand = ModifierUtil.checkVolatileFlag(stack, ToolInventoryCapability.INCLUDE_OFFHAND);
    }
    // if the stack looks like it could be our tool, fetch the handler from it
    ResourceHandler<ItemResource> handler;
    if (stack.has(ToolDataComponents.TOOL_DATA.get()) && stack.is(TinkerTags.Items.MODIFIABLE)) {
      // item capabilities need an item access, tool inventories only change the components of the stack in place
      ResourceHandler<ItemResource> cap = ToolInventoryCapability.getHandler(stack);
      handler = cap != null ? cap : EmptyItemHandler.INSTANCE;
      // wrong number of slots means something went wrong, use a dummy
      if (handler.size() != size) {
        handler = new ItemStacksResourceHandler(size);
      }
    } else {
      // stack doesn't look right? use a dummy
      handler = new ItemStacksResourceHandler(size);
    }
    return new ToolContainerMenu(TinkerTools.toolContainer.get(), id, inventory, stack, handler, slotIndex, craftingType, includeOffhand);
  }

  protected ToolContainerMenu(@Nullable MenuType<?> type, int id, Inventory playerInventory, ItemStack stack, ResourceHandler<ItemResource> handler, int slotIndex, CraftingType craftingType, boolean includeOffhand) {
    super(type, id);
    this.stack = stack;
    this.tool = ToolStack.from(stack);
    this.itemHandler = handler;
    this.player = playerInventory.player;
    this.tank = new ToolFluidHandler(tool, player.level().isClientSide() ? null : player);
    this.slotIndex = slotIndex;

    // if requested, add 3x3 crafting area
    int slots = itemHandler.size();
    int craftingOffset = (slots == 0 ? REPEAT_BACKGROUND_START : UI_START) + 1;
    if (craftingType == CraftingType.FULL) {
      this.craftingContainer = new TransientCraftingContainer(this, 3, 3);
      this.resultContainer = new ResultContainer();
      this.addSlot(new ResultSlot(this.player, this.craftingContainer, resultContainer, 0, 124, craftingOffset + 18));
      for (int r = 0; r < 3; ++r) {
        for (int c = 0; c < 3; ++c) {
          this.addSlot(new Slot(this.craftingContainer, c + r * 3, 30 + c * 18, craftingOffset + r * 18));
        }
      }
      // if no 3x3, check if 2x2 was requested
    } else if (craftingType == CraftingType.INVENTORY) {
      this.craftingContainer = new TransientCraftingContainer(this, 2, 2);
      this.resultContainer = new ResultContainer();
      this.addSlot(new ResultSlot(this.player, this.craftingContainer, resultContainer, 0, 108, craftingOffset + 10));
      for(int r = 0; r < 2; ++r) {
        for(int c = 0; c < 2; ++c) {
          this.addSlot(new Slot(this.craftingContainer, c + r * 2, 52 + c * 18, craftingOffset + r * 18));
        }
      }
    } else {
      this.craftingContainer = null;
      this.resultContainer = null;
    }
    this.toolInventoryStart = this.slots.size();

    // add tool slots
    int yOffset = REPEAT_BACKGROUND_START + getCraftingHeight() * SLOT_SIZE + 1;
    for (int i = 0; i < slots; i++) {
      this.addSlot(new ToolContainerSlot(itemHandler, i, 8 + (i % 9) * SLOT_SIZE, yOffset + (i / 9) * SLOT_SIZE));
    }
    // add offhand if requested
    this.showOffhand = includeOffhand;
    if (this.showOffhand) {
      int x = 8 + (slots % 9) * SLOT_SIZE;
      int y = yOffset + (slots / 9) * SLOT_SIZE;
      if (slotIndex == Inventory.SLOT_OFFHAND) {
        this.addSlot(new ReadOnlySlot(playerInventory, 40, x, y));
      } else {
        this.addSlot(new Slot(playerInventory, 40, x, y));
      }
      slots++;
    }

    this.playerInventoryStart = this.slots.size();

    // add player slots
    yOffset += TITLE_SIZE + ((slots + 8) / 9) * SLOT_SIZE;
    if (tank.getCapacity() > 0) {
      yOffset += 14;
    }
    for(int r = 0; r < 3; ++r) {
      for(int c = 0; c < 9; ++c) {
        int index = c + r * 9 + 9;
        if (index == slotIndex) {
          this.addSlot(new ReadOnlySlot(playerInventory, index, 8 + c * 18, yOffset + r * 18));
        } else {
          this.addSlot(new Slot(        playerInventory, index, 8 + c * 18, yOffset + r * 18));
        }
      }
    }
    yOffset += 58; // 3 slots + 4 pixel divider
    for(int c = 0; c < 9; ++c) {
      if (c == slotIndex) {
        this.addSlot(new ReadOnlySlot(playerInventory, c, 8 + c * 18, yOffset));
      } else {
        this.addSlot(new Slot(        playerInventory, c, 8 + c * 18, yOffset));
      }
    }
  }

  /** Gets the height of the crafting area in slots */
  public int getCraftingHeight() {
    return craftingContainer != null ? craftingContainer.getHeight() : 0;
  }

  @Override
  public boolean stillValid(Player playerIn) {
    // if the stack ever leaves the slot, close the menu, as we have no way to recover then and dupes are likely
    return player == playerIn && !stack.isEmpty() && player.getInventory().getItem(slotIndex) == stack;
  }

  @Override
  public ItemStack quickMoveStack(Player playerIn, int index) {
    ItemStack result = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot.hasItem()) {
      ItemStack slotStack = slot.getItem();
      result = slotStack.copy();
      int end = this.slots.size();
      // if its in a crafting slot, move it anywhere
      if (index < toolInventoryStart) {
        if (!this.moveItemStackTo(slotStack, toolInventoryStart, end, true)) {
          return ItemStack.EMPTY;
        }
        if (index == 0) {
          slot.onQuickCraft(slotStack, result);
        }
        // if its in the tool inventory, move to player inventory
      } else if (index < playerInventoryStart) {
        if (!this.moveItemStackTo(slotStack, playerInventoryStart, end, true)) {
          return ItemStack.EMPTY;
        }
        // if its in the player inventory, move to the tool inventory
      } else if (!this.moveItemStackTo(slotStack, toolInventoryStart, playerInventoryStart, false)) {
        return ItemStack.EMPTY;
      }
      // if we moved the whole stack, clear the slot
      if (slotStack.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
      // if we moved nothing, give up
      if (slotStack.getCount() == result.getCount()) {
        return ItemStack.EMPTY;
      }
      // drop the crafted result if it didn't entirely move
      slot.onTake(player, slotStack);
      if (toolInventoryStart > 0 && index == 0) {
        player.drop(slotStack, false, Prediction.PREDICTED);
      }

    }
    return result;
  }

  @Override
  public void slotsChanged(Container pContainer) {
    super.slotsChanged(pContainer);
    // crafting results are only computed server side, the server syncs the result slot
    if (craftingContainer != null && resultContainer != null && player.level() instanceof ServerLevel serverLevel) {
      CraftingMenu.slotChangedCraftingGrid(this, serverLevel, player, craftingContainer, resultContainer, (RecipeHolder<CraftingRecipe>) null);
    }
  }

  @Override
  public void removed(Player pPlayer) {
    super.removed(pPlayer);
    if (resultContainer != null) {
      resultContainer.clearContent();
    }
    if (craftingContainer != null) {
      clearContainer(pPlayer, craftingContainer);
    }
  }

  @Override
  public boolean clickMenuButton(Player player, int id) {
    ItemStack held = getCarried();
    if ((id == 0 || id == 1) && !held.isEmpty() && !player.isSpectator()) {
      if (!player.level().isClientSide()) {
        TransferResult result = FluidTransferHelper.interactWithStack(tank, held, id == 0 ? TransferDirection.FILL_ITEM : TransferDirection.EMPTY_ITEM);
        setCarried(FluidTransferHelper.handleUIResult(player, held, result));
      }
      return true;
    }
    return false;
  }

  /**
   * Slot for the tool inventory.
   * Setting the stack goes through the handler's index modifier (see {@link SmartItemHandlerSlot}), and the resource handler slot base
   * already writes the cached stack back on {@link #setChanged()}, so only pickup needs to differ.
   */
  private static class ToolContainerSlot extends SmartItemHandlerSlot {
    public ToolContainerSlot(ResourceHandler<ItemResource> itemHandler, int index, int xPosition, int yPosition) {
      super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPickup(Player playerIn) {
      return true;
    }
  }

  /** Logic handling the fluid tank in the UI */
  private static class ToolFluidHandler extends SimpleFluidTank {
    private final ToolStack tool;
    @Nullable
    private final Player player;

    private ToolFluidHandler(ToolStack tool, @Nullable Player player) {
      this.tool = tool;
      this.player = player;
    }

    @Nonnull
    @Override
    public FluidStack getFluid() {
      // the inventory may have changed the stack since we last read it, as tool data is a component each view holds a copy
      tool.refreshFromStack();
      return ToolTankHelper.TANK_HELPER.getFluid(tool);
    }

    @Override
    public void setFluid(FluidStack fluid) {
      // refresh first so we do not write back stale inventory data
      tool.refreshFromStack();
      ToolTankHelper.TANK_HELPER.setFluid(tool, fluid);
    }

    @Override
    public void updateFluid(FluidStack updated, int change) {
      // only called once per committed transaction when the fluid actually changed, change may be 0 if the fluid was swapped
      setFluid(updated);
      if (player != null) {
        TinkerNetwork.getInstance().sendTo(new ToolContainerFluidUpdatePacket(updated), player);
      }
    }

    @Override
    public int getCapacity() {
      tool.refreshFromStack();
      return ToolTankHelper.TANK_HELPER.getCapacity(tool);
    }
  }
}
