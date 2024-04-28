package slimeknights.tconstruct.tools.modifiers.defense;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.data.ModifierMaxLevel;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;

public class ShulkingModifier extends AbstractProtectionModifier<ModifierMaxLevel> {
  private static final ComputableDataKey<ModifierMaxLevel> KEY = TConstruct.createKey("shulking", ModifierMaxLevel::new);
  public ShulkingModifier() {
    super(KEY);
    // TODO: move to data key registry and ModifierEvent
    MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingHurtEvent.class, ShulkingModifier::onAttack);
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(ProtectionModule.source(DamageSourcePredicate.CAN_PROTECT).entity(LivingEntityPredicate.CROUCHING).eachLevel(2.5f));
  }

  private static void onAttack(LivingHurtEvent event) {
    // if the attacker is crouching, deal less damage
    Entity attacker = event.getSource().getEntity();
    if (attacker != null && attacker.isCrouching()) {
      attacker.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
        ModifierMaxLevel max = data.get(KEY);
        if (max != null) {
          event.setAmount(event.getAmount() * (1 - (max.getMax() * 0.1f)));
        }
      });
    }
  }
}
