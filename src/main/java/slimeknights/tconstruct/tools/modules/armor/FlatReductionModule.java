package slimeknights.tconstruct.tools.modules.armor;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.json.predicate.TinkerPredicate;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Module preventing some damage when at full health.
 * Note this module has no default hooks. You will want to register it with {@link ModifierHooks#TOOLTIP} plus one of {@link ModifierHooks#MODIFY_HURT} or {@link ModifierHooks#MODIFY_DAMAGE}.
 * @param reduction    Flat reduction to apply when above the threshold.
 * @param minimum      Smallest amount of damage allowed after reduction.
 * @param holder       Condition on the entity wearing the armor.
 * @param damageSource Condition on the damage source.
 * @param condition    Condition on the tool and modifier entry.
 */
public record FlatReductionModule(LevelingValue reduction, LevelingValue minimum, IJsonPredicate<LivingEntity> holder, IJsonPredicate<DamageSource> damageSource, ModifierCondition<IToolStackView> condition) implements ModifierModule, ModifyDamageModifierHook, TooltipModifierHook, ConditionalModule<IToolStackView> {
  public static final RecordLoadable<FlatReductionModule> LOADER = RecordLoadable.create(
    LevelingValue.LOADABLE.requiredField("reduction", FlatReductionModule::reduction),
    LevelingValue.LOADABLE.requiredField("minimum", FlatReductionModule::minimum),
    LivingEntityPredicate.LOADER.defaultField("holder", FlatReductionModule::holder),
    DamageSourcePredicate.LOADER.defaultField("damage_source", FlatReductionModule::damageSource),
    ModifierCondition.TOOL_FIELD,
    FlatReductionModule::new);

  /** @apiNote use {@link #builder()} */
  @Internal
  public FlatReductionModule {}

  @Override
  public RecordLoadable<? extends ModifierModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return List.of();
  }

  @Override
  public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
    if (condition.matches(tool, modifier) && this.holder.matches(context.getEntity()) && this.damageSource.matches(source)) {
      float level = modifier.getEffectiveLevel();
      // don't reduce below the minumum, but make sure if it was already below we leave it there
      float minimum = this.minimum.compute(level);
      if (amount > minimum) {
        amount = Math.max(minimum, amount - reduction.compute(level));
      }
    }
    return amount;
  }

  @Override
  public void addTooltip(IToolStackView tool, ModifierEntry entry, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    if (condition.matches(tool, entry) && TinkerPredicate.matchesInTooltip(holder, player, tooltipKey)) {
      float reduction = this.reduction.compute(entry.getEffectiveLevel());
      if (reduction != 0) {
        Modifier modifier = entry.getModifier();
        TooltipModifierHook.addFlatBoost(modifier, Component.translatable(modifier.getTranslationKey() + ".reduction"), reduction, tooltip);
      }
    }
  }


  /* Builder */

  /** Creates a new builder instance */
  public static Builder builder() {
    return new Builder();
  }

  @Setter
  @Accessors(fluent = true)
  public static class Builder extends ModuleBuilder.Stack<Builder> implements LevelingValue.Builder<FlatReductionModule> {
    private LevelingValue minimum = LevelingValue.ZERO;
    private IJsonPredicate<LivingEntity> holder = LivingEntityPredicate.ANY;
    private IJsonPredicate<DamageSource> damageSource = DamageSourcePredicate.CAN_PROTECT;

    @Override
    public FlatReductionModule amount(float flat, float eachLevel) {
      return new FlatReductionModule(new LevelingValue(flat, eachLevel), minimum, holder, damageSource, condition);
    }
  }
}
