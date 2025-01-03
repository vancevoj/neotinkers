package slimeknights.tconstruct.plugin.jsonthings.item;

import dev.gigaherz.jsonthings.things.builders.ItemBuilder;
import dev.gigaherz.jsonthings.things.serializers.IItemFactory;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

/** Extension of {@link IItemFactory} for material items */
public interface IMaterialItemFactory<T extends Item & IMaterialItem> extends IItemFactory<T> {
  @SuppressWarnings("removal")  // no other API exists
  @Override
  default void provideVariants(BuildCreativeModeTabContentsEvent event, ItemBuilder context) {
    if (context.get().self() instanceof IMaterialItem materialItem) {
      materialItem.addVariants(event::accept, "");
    }
  }
}
