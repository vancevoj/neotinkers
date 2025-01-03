package slimeknights.tconstruct.plugin.jsonthings.item;

import dev.gigaherz.jsonthings.things.builders.ItemBuilder;
import dev.gigaherz.jsonthings.things.serializers.IItemFactory;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;

/** Extension of {@link IItemFactory} for tool items */
public interface IToolItemFactory<T extends Item & IModifiable> extends IItemFactory<T> {
  @SuppressWarnings("removal")  // no other API exists
  @Override
  default void provideVariants(BuildCreativeModeTabContentsEvent event, ItemBuilder context) {
    if (context.get().self() instanceof IModifiable modifiable) {
      ToolBuildHandler.addVariants(event::accept, modifiable, "");
    }
  }
}
