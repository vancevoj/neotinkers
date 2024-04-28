package slimeknights.tconstruct.tools.logic;

import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.tools.modifiers.traits.ranged.CrystalboundModifier;

/** Events to implement modifier specific behaviors, such as those defined by {@link TinkerDataKeys}. General hooks will typically be in {@link ToolEvents} */
@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.FORGE)
public class ModifierEvents {
  @SubscribeEvent
  static void onKnockback(LivingKnockBackEvent event) {
    event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
      float knockback = data.get(TinkerDataKeys.KNOCKBACK, 0f);
      if (knockback != 0) {
        // adds +20% knockback per level
        event.setStrength(event.getStrength() * (1 + knockback));
      }
      // apply crystalbound bonus
      int crystalbound = data.get(CrystalboundModifier.LEVEL, 0);
      if (crystalbound > 0) {
        CrystalboundModifier.onKnockback(event, crystalbound);
      }
    });
  }
}
