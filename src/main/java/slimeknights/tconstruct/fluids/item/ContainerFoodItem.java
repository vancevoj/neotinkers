package slimeknights.tconstruct.fluids.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

/** @deprecated use {@link slimeknights.mantle.item.ContainerFoodItem} */
@Deprecated(forRemoval = true)
public class ContainerFoodItem extends slimeknights.mantle.item.ContainerFoodItem {
  public ContainerFoodItem(Properties props) {
    super(props);
  }

  /** Adds effects to the tooltip */
  public static void addEffectTooltip(FoodProperties food, List<Component> tooltip) {
    slimeknights.mantle.item.ContainerFoodItem.addEffectTooltip(food, tooltip);
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    FoodProperties food = stack.get(DataComponents.FOOD);
    if (food != null) {
      addEffectTooltip(food, tooltip);
    }
  }

  /** @deprecated use {@link slimeknights.mantle.item.ContainerFoodItem.FluidContainerFoodItem} */
  @Deprecated(forRemoval = true)
  public static class FluidContainerFoodItem extends slimeknights.mantle.item.ContainerFoodItem.FluidContainerFoodItem {
    public FluidContainerFoodItem(Properties props, Supplier<FluidStack> fluid) {
      super(props, fluid);
    }
  }
}
