package slimeknights.tconstruct.tools.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.fluid.SimpleFluidTank;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.InventoryModifierHook;
import slimeknights.tconstruct.library.tools.layout.Patterns;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.smeltery.client.screen.IScreenWithFluidTank;
import slimeknights.tconstruct.smeltery.client.screen.module.ClickableTankModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiTankModule;
import slimeknights.tconstruct.tools.menu.ToolContainerMenu;

import javax.annotation.Nullable;
import java.util.List;

import static slimeknights.tconstruct.tools.menu.ToolContainerMenu.REPEAT_BACKGROUND_START;
import static slimeknights.tconstruct.tools.menu.ToolContainerMenu.SLOT_SIZE;
import static slimeknights.tconstruct.tools.menu.ToolContainerMenu.TITLE_SIZE;
import static slimeknights.tconstruct.tools.menu.ToolContainerMenu.UI_START;

/** Screen for a tool inventory */
public class ToolContainerScreen extends AbstractContainerScreen<ToolContainerMenu> implements IScreenWithFluidTank {
  /** The Identifier containing the chest GUI texture. */
  private static final Identifier TEXTURE = TConstruct.getResource("textures/gui/tool_inventory.png");

  /** Slot background for 3x3 crafting grid */
  private static final ElementScreen CRAFTING_SLOTS = new ElementScreen(TEXTURE, 176, 74, 54, 54, 256, 256);
  /** Result slot for 3x3 crafting grid */
  private static final ElementScreen CRAFTING_RESULT = CRAFTING_SLOTS.move(176, 20, 62, 54);
  /** Full 2x2 crafting grid */
  private static final ElementScreen INVENTORY_CRAFTING = CRAFTING_SLOTS.move(176, 128, 74, 36);
  /** Fluid bar to draw at the bottom of the UI */
  private static final ElementScreen FLUID_TANK = CRAFTING_SLOTS.move(0, 224, 176, 14);

  /** Max number of rows in the repeat slots background */
  private static final int REPEAT_BACKGROUND_SIZE = 6 * SLOT_SIZE;
  /** Start location of the player inventory */
  private static final int PLAYER_INVENTORY_START = REPEAT_BACKGROUND_START + REPEAT_BACKGROUND_SIZE;
  /** Height of the player inventory texture */
  private static final int PLAYER_INVENTORY_HEIGHT = 96;
  /** Start Y location of the slot start element */
  private static final int SLOTS_START = 256 - SLOT_SIZE;
  /** Selected slot texture X position */
  private static final int SELECTED_X = 176;

  /** Total number of slots in the inventory */
  private final int slots;
  /** Number of rows in this inventory */
  private final int inventoryRows;
  /** Number of slots in the final row */
  private final int slotsInLastRow;
  /** Tool tank rendering logic */
  @Nullable
  private final GuiTankModule tank;
  public ToolContainerScreen(ToolContainerMenu menu, Inventory inv, Component title) {
    // 26.3: image size is final, so compute it before calling super
    super(menu, inv, title, 176, computeImageHeight(menu));
    int slots = getSlotCount(menu);
    int inventoryRows = slots / 9;
    int slotsInLastRow = slots % 9;
    if (slotsInLastRow == 0) {
      slotsInLastRow = 9;
    } else {
      inventoryRows++;
    }
    this.slots = slots;
    this.inventoryRows = inventoryRows;
    this.slotsInLastRow = slotsInLastRow;
    int craftingHeight = menu.getCraftingHeight() * SLOT_SIZE;
    SimpleFluidTank tank = menu.getTank();
    if (tank.getCapacity() > 0) {
      this.tank = new GuiTankModule(this, tank, 8, this.imageHeight - PLAYER_INVENTORY_HEIGHT - 9, 160, 8, true, null);
    } else {
      this.tank = null;
    }
    if (slots > 0) {
      this.titleLabelY += craftingHeight;
    }
    this.inventoryLabelY = this.imageHeight - 93;
  }

  /** Gets the number of slots displayed for the tool inventory */
  private static int getSlotCount(ToolContainerMenu menu) {
    int slots = menu.getItemHandler().size();
    if (menu.isShowOffhand()) {
      slots++;
    }
    return slots;
  }

  /** Computes the height of the UI for the given menu */
  private static int computeImageHeight(ToolContainerMenu menu) {
    int slots = getSlotCount(menu);
    int inventoryRows = slots / 9;
    if (slots % 9 != 0) {
      inventoryRows++;
    }
    int craftingHeight = menu.getCraftingHeight() * SLOT_SIZE;
    int imageHeight = UI_START + TITLE_SIZE + PLAYER_INVENTORY_HEIGHT + inventoryRows * SLOT_SIZE + craftingHeight;
    if (menu.getTank().getCapacity() > 0) {
      imageHeight += FLUID_TANK.h;
    }
    return imageHeight;
  }

  @Override
  protected void slotClicked(Slot slot, int slotId, int index, ContainerInput type) {
    // disallow swapping the tool slot
    if (type == ContainerInput.SWAP && slot.container == menu.getPlayer().getInventory() && slot.getSlotIndex() == menu.getSlotIndex()) {
      return;
    }
    super.slotClicked(slot, slotId, index, type);
  }

  /** Draws part of the UI texture */
  private static void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
    graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, width, height, 256, 256);
  }

  /** Draws a pattern from the block atlas */
  private static void drawPattern(GuiGraphicsExtractor graphics, Pattern pattern, int x, int y) {
    TextureAtlasSprite sprite = graphics.getSprite(new SpriteId(TextureAtlas.LOCATION_BLOCKS, pattern.getTexture()));
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16);
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partialTicks) {
    // dims the screen, the background is drawn once per frame on its own layer
    super.extractBackground(graphics, x, y, partialTicks);
    int xStart = (this.width - this.imageWidth) / 2;
    int yStart = (this.height - this.imageHeight) / 2;

    // draw the background, repeated if we have too much content for the default size, or shrunk otherwise
    int craftingHeight = menu.getCraftingHeight();
    int slotBackground = REPEAT_BACKGROUND_START + (inventoryRows + craftingHeight) * SLOT_SIZE;
    if (slotBackground < PLAYER_INVENTORY_START) {
      // small background? draw a single segment up to the size
      blit(graphics, xStart, yStart, 0, 0, this.imageWidth, slotBackground);
    } else {
      // large background? repeat as needed
      // start with the top bar + roughly 6 slots
      blit(graphics, xStart, yStart, 0, 0, this.imageWidth, PLAYER_INVENTORY_START);
      int yOffset = PLAYER_INVENTORY_START;
      int remainingBackground = slotBackground - yOffset;
      // add chunks of about 6 until we run out
      for (; remainingBackground > REPEAT_BACKGROUND_SIZE; remainingBackground -= REPEAT_BACKGROUND_SIZE) {
        blit(graphics, xStart, yStart + yOffset, 0, REPEAT_BACKGROUND_START, this.imageWidth, REPEAT_BACKGROUND_SIZE);
        yOffset += REPEAT_BACKGROUND_SIZE;
      }
      // draw last partial chunk
      blit(graphics, xStart, yStart + yOffset, 0, REPEAT_BACKGROUND_START, this.imageWidth, remainingBackground);
    }
    // draw tank if we have capacity
    if (tank != null) {
      FLUID_TANK.draw(graphics, xStart, yStart + slotBackground);
      slotBackground += FLUID_TANK.h;
    }
    // draw the player inventory background
    blit(graphics, xStart, yStart + slotBackground, 0, PLAYER_INVENTORY_START, this.imageWidth, PLAYER_INVENTORY_HEIGHT);

    // add crafting table slots
    // if we have no slots, push them below the title, otherwise above the title
    int craftingOffset = yStart + (slots == 0 ? REPEAT_BACKGROUND_START : UI_START);
    if (craftingHeight == 3) {
      CRAFTING_SLOTS.draw(graphics, xStart + 29, craftingOffset);
      CRAFTING_RESULT.draw(graphics, xStart + 83, craftingOffset);
    } else if (craftingHeight == 2) {
      INVENTORY_CRAFTING.draw(graphics, xStart + 51, craftingOffset);
    }

    // draw slot background
    if (slots > 0) {
      int rowLeft = xStart + 7;
      int rowStart = yStart + REPEAT_BACKGROUND_START - SLOT_SIZE + (craftingHeight * SLOT_SIZE);
      for (int i = 1; i < inventoryRows; i++) {
        blit(graphics, rowLeft, rowStart + i * SLOT_SIZE, 0, SLOTS_START, 9 * SLOT_SIZE, SLOT_SIZE);
      }
      // last row may not have all slots
      blit(graphics, rowLeft, rowStart + inventoryRows * SLOT_SIZE, 0, SLOTS_START, slotsInLastRow * SLOT_SIZE, SLOT_SIZE);
    }

    // draw a background on the selected slot index
    int slotIndex = menu.getSlotIndex();
    int playerStart = menu.getPlayerInventoryStart();
    int highlightIndex = -1;
    if (slotIndex < 9) {
      // hotbar slots are after all our slots, and after the main inventory 27
      highlightIndex = playerStart + slotIndex + 27;
    } else if (slotIndex < Inventory.INVENTORY_SIZE) {
      // main inventory 27 is after our slots, but the index is 9 too high (hotbar)
      highlightIndex = playerStart + slotIndex - 9;
    } else if (slotIndex == Inventory.SLOT_OFFHAND && menu.isShowOffhand()) {
      // offhand is the last slot, but only if the offhand is shown in the inveotry
      highlightIndex = playerStart - 1;
    }
    // armor is not shown, so that will be -1
    if (highlightIndex != -1 && highlightIndex < menu.slots.size()) {
      Slot slot = menu.getSlot(highlightIndex);
      blit(graphics, xStart + slot.x - 2, yStart + slot.y - 2, SELECTED_X, 0, SLOT_SIZE + 2, SLOT_SIZE + 2);
    }

    // draw slot patterns for all empty slots
    int start = menu.getToolInventoryStart();
    int maxSlots = menu.slots.size();

    IToolStackView tool = menu.getTool();
    List<ModifierEntry> modifiers = tool.getModifierList();
    modifiers:
    for (int modIndex = modifiers.size() - 1; modIndex >= 0; modIndex--) {
      ModifierEntry entry = modifiers.get(modIndex);
      InventoryModifierHook inventory = entry.getHook(ToolInventoryCapability.HOOK);
      int size = inventory.getSlots(tool, entry);
      for (int i = 0; i < size; i++) {
        if (start + i >= maxSlots) {
          break modifiers;
        }
        Slot slot = menu.getSlot(start + i);
        Pattern pattern = inventory.getPattern(tool, entry, i, slot.hasItem());
        if (pattern != null) {
          drawPattern(graphics, pattern, xStart + slot.x, yStart + slot.y);
        }
      }
      start += size;
    }

    // offhand icon
    if (menu.isShowOffhand()) {
      Slot slot = menu.getSlot(menu.getPlayerInventoryStart() - 1);
      if (!slot.hasItem()) {
        drawPattern(graphics, Patterns.SHIELD, xStart + slot.x, yStart + slot.y);
      }
    }

    if (tank != null) {
      tank.draw(graphics);
    }
  }

  @Override
  protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    super.extractLabels(graphics, mouseX, mouseY);
    if (tank != null) {
      tank.highlightHoveredFluid(graphics, mouseX - this.leftPos, mouseY - this.topPos);
    }
  }

  @Override
  protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    super.extractTooltip(graphics, mouseX, mouseY);

    if (tank != null) {
      tank.renderTooltip(graphics, mouseX, mouseY);
    }
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    assert minecraft.player != null && minecraft.gameMode != null;
    // menu button IDs are still 0 for left and 1 for right
    int button = ClickableTankModule.getMenuButton(event.button());
    if (tank != null && button != -1 && !menu.getCarried().isEmpty() && !minecraft.player.isSpectator()) {
      if (tank.tryClick((int)event.x() - leftPos, (int)event.y() - topPos, button, 0)) {
        return true;
      }
    }
    return super.mouseClicked(event, doubleClick);
  }

  @Nullable
  @Override
  public FluidLocation getFluidUnderMouse(int mouseX, int mouseY) {
    if (tank != null) {
      return tank.getFluidUnderMouse(mouseX - leftPos, mouseY - topPos);
    }
    return null;
  }
}
