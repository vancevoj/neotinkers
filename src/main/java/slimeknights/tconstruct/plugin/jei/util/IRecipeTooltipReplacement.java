package slimeknights.tconstruct.plugin.jei.util;

import com.mojang.datafixers.util.Either;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated use {@link FluidTooltipCallback} for better handling of advanced tooltip information.
 * JEI 31 removed the list based slot tooltip callback, so this is now a rich tooltip callback.
 */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface IRecipeTooltipReplacement extends IRecipeSlotRichTooltipCallback {
  /** Tooltip replacement that keeps just the name and mod ID */
  IRecipeTooltipReplacement EMPTY = (slot, tooltip) -> {};

  @Override
  default void onRichTooltip(IRecipeSlotView recipeSlotView, ITooltipBuilder tooltip) {
    List<Either<FormattedText,TooltipComponent>> lines = tooltip.getLines();
    // keep the name, replace everything after it
    if (!lines.isEmpty()) {
      Either<FormattedText,TooltipComponent> name = lines.getFirst();
      lines.clear();
      lines.add(name);
    }
    List<Component> middle = new ArrayList<>();
    addMiddleLines(recipeSlotView, middle);
    tooltip.addAll(middle);
  }

  /** Adds the lines between the name and mod ID */
  void addMiddleLines(IRecipeSlotView recipeSlotView, List<Component> tooltip);
}
