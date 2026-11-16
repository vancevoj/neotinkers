package slimeknights.tconstruct.fluids.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.component.Consumable;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * @deprecated use {@link slimeknights.mantle.item.ContainerFoodItem}.
 * Food effects are part of the {@link Consumable} component since 1.21.2, the inherited tooltip already lists them.
 */
@Deprecated(forRemoval = true)
public class ContainerFoodItem extends slimeknights.mantle.item.ContainerFoodItem {
  public ContainerFoodItem(Properties props) {
    super(props);
  }

  /** Adds the effects applied when consuming to the tooltip */
  public static void addEffectTooltip(Consumable consumable, List<Component> tooltip) {
    slimeknights.mantle.item.ContainerFoodItem.addEffectTooltip(consumable, tooltip);
  }

  /** Adds the given effects to the tooltip, for things without a consumable such as cake blocks */
  public static void addEffectTooltip(Collection<MobEffectInstance> effects, List<Component> tooltip) {
    slimeknights.mantle.item.ContainerFoodItem.addEffectTooltip(effects, tooltip::add);
  }

  /** @deprecated use {@link slimeknights.mantle.item.ContainerFoodItem.FluidContainerFoodItem} */
  @Deprecated(forRemoval = true)
  public static class FluidContainerFoodItem extends slimeknights.mantle.item.ContainerFoodItem.FluidContainerFoodItem {
    public FluidContainerFoodItem(Properties props, Supplier<FluidStack> fluid) {
      super(props, fluid);
    }
  }
}
