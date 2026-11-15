package slimeknights.tconstruct.tools.modifiers.traits.skull;

import java.util.Set;
import java.util.List;
import slimeknights.tconstruct.library.utils.TinkerEffectCures;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.DamageDealtModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class PlagueModifier extends NoLevelsModifier implements DamageDealtModifierHook {
  /** Cures of an effect without custom cures, see {@link TinkerEffectCures#getCures(MobEffectInstance)} */
  private static final Set<Item> DEFAULT_CURES = Set.of(Items.MILK_BUCKET);

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, ModifierHooks.DAMAGE_DEALT);
  }

  @Override
  public void onDamageDealt(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, LivingEntity target, DamageSource source, float amount, boolean isDirectDamage) {
    if (isDirectDamage && !source.is(DamageTypeTags.IS_PROJECTILE)) {
      // copy all negative effects to target
      LivingEntity attacker = context.getEntity();
      for (MobEffectInstance effect : context.getEntity().getActiveEffects()) {
        // 1.21: getEffect() returns Holder<MobEffect>, unwrap via value()
        // 26.3 removed effect cures, Tinkers tracks them per instance (empty cures are internal effects)
        Set<Item> cures = TinkerEffectCures.getCures(effect);
        if (!effect.getEffect().value().isBeneficial() && !cures.isEmpty()) {
          MobEffectInstance copy = new MobEffectInstance(effect);
          // the copy constructor used to copy the cures, keep custom cures on the copy
          if (!cures.equals(DEFAULT_CURES)) {
            TinkerEffectCures.setCures(copy, List.copyOf(cures));
          }
          target.addEffect(copy, attacker);
        }
      }
    }
  }
}
