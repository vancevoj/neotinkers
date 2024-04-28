package slimeknights.tconstruct.tools.modifiers.defense;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.data.ModifierMaxLevel;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;

import java.util.UUID;

public class MeleeProtectionModifier extends AbstractProtectionModifier<ModifierMaxLevel> {
  private static final UUID ATTRIBUTE_UUID = UUID.fromString("6f030b1e-e9e1-11ec-8fea-0242ac120002");
  private static final ComputableDataKey<ModifierMaxLevel> KEY = TConstruct.createKey("melee_protection", ModifierMaxLevel::new);

  public MeleeProtectionModifier() {
    super(KEY);
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(ProtectionModule.source(DamageSourcePredicate.CAN_PROTECT, DamageSourcePredicate.MELEE).eachLevel(2.5f));
  }

  @Override
  protected void set(ModifierMaxLevel data, EquipmentSlot slot, float scaledLevel, EquipmentChangeContext context) {
    float oldMax = data.getMax();
    super.set(data, slot, scaledLevel, context);
    float newMax = data.getMax();
    // 5% bonus attack speed for the largest level
    if (oldMax != newMax) {
      AttributeInstance instance = context.getEntity().getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (instance != null) {
        instance.removeModifier(ATTRIBUTE_UUID);
        if (newMax != 0) {
          instance.addTransientModifier(new AttributeModifier(ATTRIBUTE_UUID, "tconstruct.melee_protection", 0.05 * newMax, Operation.ADDITION));
        }
      }
    }
  }
}
