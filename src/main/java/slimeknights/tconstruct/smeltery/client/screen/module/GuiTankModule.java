package slimeknights.tconstruct.smeltery.client.screen.module;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.smeltery.client.screen.IScreenWithFluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Module handling the melter tank UI display
 */
public class GuiTankModule implements IScreenWithFluidTank, ClickableTankModule {
  /** Tooltip for when the capacity is 0, it breaks some stuff */
  private static final Component NO_CAPACITY = Component.translatable(Mantle.makeDescriptionId("gui", "fluid.millibucket"), 0).withStyle(ChatFormatting.GRAY);

  private static final int TANK_INDEX = 0;
  private final AbstractContainerScreen<?> screen;
  private final ResourceHandler<FluidResource> tank;
  @Getter
  private final int x, y, width, height;
  private final boolean horizontal;
  private final Rect2i fluidLoc;
  private final BiConsumer<Integer,List<Component>> formatter;

  public GuiTankModule(AbstractContainerScreen<?> screen, ResourceHandler<FluidResource> tank, int x, int y, int width, int height, @Nullable Identifier tooltipId) {
    this(screen, tank, x, y, width, height, false, tooltipId);
  }

  public GuiTankModule(AbstractContainerScreen<?> screen, ResourceHandler<FluidResource> tank, int x, int y, int width, int height, boolean horizontal, @Nullable Identifier tooltipId) {
    this.screen = screen;
    this.tank = tank;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.horizontal = horizontal;
    this.fluidLoc = new Rect2i(x, y, width, height);
    this.formatter = tooltipId == null ? FluidTooltipHandler.BUCKET_FORMATTER : (amount, tooltip) -> FluidTooltipHandler.appendNamedList(tooltipId, amount, tooltip);
  }

  @Override
  public AbstractContainerMenu getMenu() {
    return screen.getMenu();
  }

  @Override
  public boolean isHovered(int checkX, int checkY) {
    return GuiUtil.isHovered(checkX, checkY, x - 1, y - 1, width + 2, height + 2);
  }

  @Override
  public boolean isFluidHovered(int check) {
    if (horizontal) {
      return check - x <= scaleFluid(width);
    }
    return check > (y + height) - scaleFluid(height);
  }

  /** Gets the fluid in the displayed tank, replaces {@code IFluidHandler#getFluidInTank} */
  private FluidStack getFluid() {
    // handlers with no tanks (such as an empty handler) have no index to read
    if (tank.size() <= TANK_INDEX) {
      return FluidStack.EMPTY;
    }
    FluidResource resource = tank.getResource(TANK_INDEX);
    if (resource.isEmpty()) {
      return FluidStack.EMPTY;
    }
    return resource.toStack(tank.getAmountAsInt(TANK_INDEX));
  }

  /** Gets the capacity of the displayed tank regardless of contents, replaces {@code IFluidHandler#getTankCapacity} */
  private int getCapacity() {
    if (tank.size() <= TANK_INDEX) {
      return 0;
    }
    return tank.getCapacityAsInt(TANK_INDEX, FluidResource.EMPTY);
  }

  /**
   * Gets the scaled amount of the fluid in pixels
   * @return  Scaled max value
   */
  private int scaleFluid(int max) {
    int capacity = getCapacity();
    if (capacity == 0) {
      return max;
    }
    return max * getFluid().getAmount() / capacity;
  }

  /**
   * Draws the tank
   * @param graphics  GuiGraphicsExtractor instance
   */
  public void draw(GuiGraphicsExtractor graphics) {
    FluidStack stack = getFluid();
    int capacity = getCapacity();
    if (horizontal) {
      if(!stack.isEmpty() && capacity > 0) {
        int fluidWidth = Math.min(width * stack.getAmount() / capacity, width);
        GuiUtil.renderTiledFluid(graphics, screen, stack, x, y, fluidWidth, height, 100);
      }
    } else {
      GuiUtil.renderFluidTank(graphics, screen, stack, capacity, x, y, width, height, 100);
    }
  }

  /**
   * Highlights the hovered fluid
   * @param graphics  GuiGraphicsExtractor instance
   * @param checkX    Mouse X position, screen relative
   * @param checkY    Mouse Y position, screen relative
   */
  public void highlightHoveredFluid(GuiGraphicsExtractor graphics, int checkX, int checkY) {
    // highlight hovered fluid
    if (isHovered(checkX, checkY)) {
      if (horizontal) {
        int fluidWidth = scaleFluid(width);
        int middle = x + fluidWidth;

        // highlight just fluid
        if (checkX <= middle) {
          GuiUtil.renderHighlight(graphics, x, y, fluidWidth, height);
        } else {
          // or highlight empty
          GuiUtil.renderHighlight(graphics, x + fluidWidth, y, width - fluidWidth, height);
        }
      } else {
        int fluidHeight = scaleFluid(height);
        int middle = y + height - fluidHeight;

        // highlight just fluid
        if (checkY > middle) {
          GuiUtil.renderHighlight(graphics, x, middle, width, fluidHeight);
        } else {
          // or highlight empty
          GuiUtil.renderHighlight(graphics, x, y, width, height - fluidHeight);
        }
      }
    }
  }

  /**
   * Renders the tooltip for hovering over the tank
   * @param graphics  GuiGraphicsExtractor instance
   * @param mouseX    Global mouse X position
   * @param mouseY    Global mouse Y position
   */
  public void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    int checkX = mouseX - screen.getLeftPos();
    int checkY = mouseY - screen.getTopPos();

    if (isHovered(checkX, checkY)) {
      FluidStack fluid = getFluid();
      int amount = fluid.getAmount();
      int capacity = getCapacity();

      // if hovering over the fluid, display with name
      final List<Component> tooltip;
      if (capacity > 0 && isFluidHovered(horizontal ? checkX : checkY)) {
        tooltip = FluidTooltipHandler.getFluidTooltip(fluid);
      } else {
        // function to call for amounts
        BiConsumer<Integer, List<Component>> formatter = Minecraft.getInstance().hasShiftDown()
                                                              ? FluidTooltipHandler.BUCKET_FORMATTER
                                                              : this.formatter;

        // add tooltips
        tooltip = new ArrayList<>();
        tooltip.add(GuiSmelteryTank.TOOLTIP_CAPACITY);
        if (capacity == 0) {
          tooltip.add(NO_CAPACITY);
        } else {
          formatter.accept(capacity, tooltip);
          if (capacity != amount) {
            tooltip.add(GuiSmelteryTank.TOOLTIP_AVAILABLE);
            formatter.accept(capacity - amount, tooltip);
          }
          // add shift message
          if (formatter != FluidTooltipHandler.BUCKET_FORMATTER) {
            FluidTooltipHandler.appendShift(tooltip);
          }
        }
      }

      // TODO: renderComponentTooltip->renderTooltip
      graphics.setComponentTooltipForNextFrame(screen.getFont(), tooltip, mouseX, mouseY);
    }
  }

  @Override
  public FluidLocation getFluidUnderMouse(int mouseX, int mouseY) {
    if (isHovered(mouseX, mouseY) && isFluidHovered(horizontal ? mouseX : mouseY)) {
      return new FluidLocation(getFluid(), fluidLoc);
    }
    return null;
  }
}
