package slimeknights.tconstruct.tables.client.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import slimeknights.tconstruct.tables.block.entity.chest.AbstractChestBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.module.ScalingChestScreen;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;
import slimeknights.tconstruct.tables.menu.TinkerChestContainerMenu;

public class TinkerChestScreen extends BaseTabbedScreen<AbstractChestBlockEntity,TabbedContainerMenu<AbstractChestBlockEntity>> {
  // TODO: can this safely be removed? its unused
  //protected static final ScalableElementScreen BACKGROUND = new ScalableElementScreen(7 + 18, 7, 18, 18);
  public ScalingChestScreen<AbstractChestBlockEntity> scalingChestScreen;

  public TinkerChestScreen(TabbedContainerMenu<AbstractChestBlockEntity> container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title);

    this.imageHeight = 184;
    TinkerChestContainerMenu.DynamicChestInventory chestContainer = container.getSubContainer(TinkerChestContainerMenu.DynamicChestInventory.class);
    if (chestContainer != null) {
      this.scalingChestScreen = new ScalingChestScreen<>(this, chestContainer, playerInventory, title);
      // add one extra row to the height
      this.scalingChestScreen.imageHeight += 18;
      this.addModule(scalingChestScreen);
    }
  }

  @Override
  protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
    this.drawBackground(graphics, BLANK_BACK_PLUS_1);

    if (this.scalingChestScreen != null) {
      this.scalingChestScreen.update(mouseX, mouseY);
    }

    super.renderBg(graphics, partialTicks, mouseX, mouseY);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (this.scalingChestScreen == null) {
      return false;
    }

    // module hooks take the 26.3 button numbering, see InputConstants.MOUSE_BUTTON_LEFT
    if (this.scalingChestScreen.handleMouseClicked(event.x(), event.y(), event.button())) {
      return false;
    }

    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
    if (this.scalingChestScreen == null) {
      return false;
    }

    if (this.scalingChestScreen.handleMouseClickMove(event.x(), event.y(), event.button(), dragX)) {
      return false;
    }

    return super.mouseDragged(event, dragX, dragY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (this.scalingChestScreen == null) {
      return false;
    }

    if (this.scalingChestScreen.handleMouseScrolled(mouseX, mouseY, scrollY)) {
      return false;
    }

    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    if (this.scalingChestScreen == null) {
      return false;
    }

    if (this.scalingChestScreen.handleMouseReleased(event.x(), event.y(), event.button())) {
      return false;
    }

    return super.mouseReleased(event);
  }
}
