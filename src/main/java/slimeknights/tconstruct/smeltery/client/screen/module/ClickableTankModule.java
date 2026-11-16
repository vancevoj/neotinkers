package slimeknights.tconstruct.smeltery.client.screen.module;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Helper to handle clicking the fluid within a tank */
public interface ClickableTankModule {
  /** Gets the menu for this tank */
  AbstractContainerMenu getMenu();

  /** If true, this tank is hovered */
  boolean isHovered(int checkX, int checkY);

  /** Checks if the fluid is hovered. Assumes {@link #isHovered(int, int)} is true. */
  boolean isFluidHovered(int checkY);

  /**
   * Called when the tank module is clicked to determine if an action happened
   * @param checkX       Mouse X relative to the screen corner
   * @param checkY       Mouse Y relative to the screen corner
   * @param button       Menu button index, 0 for left click and 1 for right click. Convert input event buttons with {@link #getMenuButton(int)}.
   * @param indexOffset  Offset added to the button to get the menu button ID
   * @return  True if an action happened
   */
  default boolean tryClick(int checkX, int checkY, int button, int indexOffset) {
    AbstractContainerMenu menu = getMenu();
    if (isHovered(checkX, checkY) && !menu.getCarried().isEmpty()) {
      int index = button + indexOffset;
      Minecraft minecraft = Minecraft.getInstance();
      assert minecraft.player != null && minecraft.gameMode != null;
      if (menu.clickMenuButton(minecraft.player, index)) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        return true;
      }
    }
    return false;
  }

  /**
   * Converts a mouse button from an input event to the button index used for menu button IDs.
   * 26.3 numbers mouse buttons from SDL ({@link InputConstants#MOUSE_BUTTON_LEFT} is 1, {@link InputConstants#MOUSE_BUTTON_RIGHT} is 3),
   * while the menus expect 0 for left and 1 for right, as the ID is the button plus an offset (right click would collide with the next offset).
   * @param button  Mouse button from the input event
   * @return  0 for left click, 1 for right click, -1 for any other button
   */
  static int getMenuButton(int button) {
    return switch (button) {
      case InputConstants.MOUSE_BUTTON_LEFT -> 0;
      case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
      default -> -1;
    };
  }
}
