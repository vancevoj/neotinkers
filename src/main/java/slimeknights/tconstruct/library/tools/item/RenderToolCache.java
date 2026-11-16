package slimeknights.tconstruct.library.tools.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.DefaultDataComponentsBoundEvent;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Cache for the stack returned by {@link IModifiableDisplay#getRenderTool()}.
 * Since 26.3 the default item components are bound again on every datapack load and when joining a world, while a stack keeps the component
 * prototype it was created with. The cached stack is rebuilt after each binding so it never holds stale defaults.
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID)
public final class RenderToolCache {
  /** Bumped each time default components are bound, invalidates all caches */
  private static volatile int generation = 0;

  @Nullable
  private ItemStack stack = null;
  private int stackGeneration = -1;

  /**
   * Gets the cached stack, building it if missing or stale
   * @param builder  Logic to build the render tool
   * @return  Render tool stack
   */
  public ItemStack get(Supplier<ItemStack> builder) {
    int current = generation;
    if (stack == null || stackGeneration != current) {
      stack = builder.get();
      stackGeneration = current;
    }
    return stack;
  }

  @SubscribeEvent
  static void onComponentsBound(DefaultDataComponentsBoundEvent event) {
    generation++;
  }
}
