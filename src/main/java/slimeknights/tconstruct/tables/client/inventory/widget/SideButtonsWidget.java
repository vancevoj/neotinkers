package slimeknights.tconstruct.tables.client.inventory.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.Rect2i;
import slimeknights.mantle.client.screen.MultiModuleScreen;

import java.util.List;

public class SideButtonsWidget<T extends Button> implements Renderable, GuiEventListener {

  private static final int SPACING = 4;

  protected final MultiModuleScreen<?> parent;

  @Getter
  private final int leftPos;
  private final int topPos;
  @Getter
  private final int imageWidth;
  private final int imageHeight;

  private final int columns;
  protected final List<T> buttons = Lists.newArrayList();
  private Button clickedButton;

  public SideButtonsWidget(MultiModuleScreen<?> parent, int leftPos, int topPos, int columns, int rows, int buttonWidth, int buttonHeight) {
    this.parent = parent;

    this.leftPos = leftPos;
    this.topPos = topPos;
    this.columns = columns;

    this.imageWidth = size(columns, buttonWidth);
    this.imageHeight = size(rows, buttonHeight);
  }

  public int guiRight() {
    return this.leftPos + this.imageWidth;
  }

  public int guiBottom() {
    return this.topPos + this.imageHeight;
  }

  public Rect2i getArea() {
    return new Rect2i(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
  }

  public void setButtonPositions() {
    for (int i = 0; i < this.buttons.size(); i++) {
      T button = this.buttons.get(i);
      int x = (i % columns) * (button.getWidth() + SPACING);
      int y = (i / columns) * (button.getHeight() + SPACING);
      button.setX(leftPos + x);
      button.setY(topPos + y);
    }
  }

  @Override
  public boolean isMouseOver(double mouseX, double mouseY) {
    return this.leftPos <= mouseX && mouseX < this.guiRight() && this.topPos <= mouseY && mouseY < this.guiBottom();
  }

  /**
   * Creates a mouse event to pass to vanilla widgets from a module style click
   * @param mouseX       Mouse X
   * @param mouseY       Mouse Y
   * @param mouseButton  Mouse button using the 26.3 numbering ({@link InputConstants#MOUSE_BUTTON_LEFT} is 1)
   * @return  Mouse event
   */
  protected static MouseButtonEvent toEvent(double mouseX, double mouseY, int mouseButton) {
    // modifiers are not used by buttons
    return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(mouseButton, 0));
  }

  /**
   * Handles a mouse click on the buttons
   * @param mouseButton  Mouse button using the 26.3 numbering ({@link InputConstants#MOUSE_BUTTON_LEFT} is 1)
   * @return  True if a button was clicked
   */
  public boolean handleMouseClicked(double mouseX, double mouseY, int mouseButton) {
    if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
      MouseButtonEvent event = toEvent(mouseX, mouseY, mouseButton);
      for (T button : this.buttons) {
        if (button.mouseClicked(event, false)) {
          this.clickedButton = button;
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Handles a mouse release on the buttons
   * @param state  Mouse button using the 26.3 numbering ({@link InputConstants#MOUSE_BUTTON_LEFT} is 1)
   * @return  True if a button was released
   */
  public boolean handleMouseReleased(double mouseX, double mouseY, int state) {
    if (clickedButton != null) {
      clickedButton.mouseReleased(toEvent(mouseX, mouseY, state));
      clickedButton = null;
      return true;
    }

    return false;
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
    for (T button : this.buttons) {
      button.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }
  }

  /**
   * Calculates the number of rows of buttons when fitting the given number of buttons within the given number of columns.
   */
  public static int rowsForCount(int columns, int count) {
    return (count - 1) / columns + 1;
  }

  /**
   * Calculates the width or height of this widget given the width or height of a button and the number of buttons along the same axis.
   */
  public static int size(int buttonCount, int buttonSize) {
    return buttonSize * buttonCount + SPACING * (buttonCount - 1);
  }

  // TODO: do I need to use these?
  @Override
  public void setFocused(boolean pFocused) {}

  @Override
  public boolean isFocused() {
    return false;
  }
}
