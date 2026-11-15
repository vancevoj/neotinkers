package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.library.utils.TinkerEffectCures;

import java.util.List;

/**
 * Effect that cannot be cured with milk
 * TODO 1.21: move to {@link slimeknights.tconstruct.shared.effect}
 */
public class NoMilkEffect extends TinkerEffect {
  public NoMilkEffect(MobEffectCategory typeIn, int color, boolean show) {
    super(typeIn, color, show);
  }

  /**
   * NeoForge 26.3 removed effect cures, so cures are now tracked per instance by {@link TinkerEffectCures}.
   * Gives every new instance of a no milk effect an empty cure list, so it cannot be cured by milk or anything else,
   * restoring the original behavior of clearing the cures.
   */
  @EventBusSubscriber(modid = TConstruct.MOD_ID)
  static class Events {
    private Events() {}

    /** Runs early so cure tracking and other listeners see the empty cures */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onEffectAdded(MobEffectEvent.Added event) {
      MobEffectInstance instance = event.getEffectInstance();
      // when an old instance exists it is updated in place, keeping its own cures
      if (event.getOldEffectInstance() == null && instance.getEffect().value() instanceof NoMilkEffect) {
        TinkerEffectCures.setCures(instance, List.of());
      }
    }
  }
}
