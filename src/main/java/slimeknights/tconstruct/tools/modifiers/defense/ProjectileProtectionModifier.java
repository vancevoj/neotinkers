package slimeknights.tconstruct.tools.modifiers.defense;

import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.data.ModifierMaxLevel;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;

public class ProjectileProtectionModifier extends AbstractProtectionModifier<ModifierMaxLevel> {
  /** Entity data key for the data associated with this modifier */
  private static final ComputableDataKey<ModifierMaxLevel> PROJECTILE_DATA = TConstruct.createKey("projectile_protection", ModifierMaxLevel::new);
  public ProjectileProtectionModifier() {
    super(PROJECTILE_DATA, true);
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(ProtectionModule.source(DamageSourcePredicate.CAN_PROTECT, DamageSourcePredicate.PROJECTILE).eachLevel(2.5f));
  }

  @Override
  protected void set(ModifierMaxLevel data, EquipmentSlot slot, float scaledLevel, EquipmentChangeContext context) {
    float oldMax = data.getMax();
    super.set(data, slot, scaledLevel, context);
    float newMax = data.getMax();
    if (oldMax != newMax) {
      context.getTinkerData().ifPresent(d -> d.add(TinkerDataKeys.USE_SPEED_BONUS, (newMax - oldMax) * 0.05f));
    }
  }
}
