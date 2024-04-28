package slimeknights.tconstruct.library.modifiers.modules.armor;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ProtectionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Module to increase protection against the given source
 * @param source    Source to protect against
 * @param entity    Conditions on the entity wearing the armor
 * @param amount    Amount of damage to block
 * @param condition Modifier module conditions
 */
public record ProtectionModule(IJsonPredicate<DamageSource> source, IJsonPredicate<LivingEntity> entity, LevelingValue amount, ModifierCondition<IToolStackView> condition) implements ProtectionModifierHook, TooltipModifierHook, ModifierModule, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ProtectionModule>defaultHooks(ModifierHooks.PROTECTION, ModifierHooks.TOOLTIP);
  public static final RecordLoadable<ProtectionModule> LOADER = RecordLoadable.create(
    DamageSourcePredicate.LOADER.defaultField("damage_source", ProtectionModule::source),
    LivingEntityPredicate.LOADER.defaultField("wearing_entity", ProtectionModule::entity),
    LevelingValue.LOADABLE.directField(ProtectionModule::amount),
    ModifierCondition.TOOL_FIELD,
    ProtectionModule::new);

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public float getProtectionModifier(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float modifierValue) {
    // apply the main protection bonus
    if (condition.matches(tool, modifier) && this.source.matches(source) && this.entity.matches(context.getEntity())) {
      modifierValue += amount.compute(modifier.getEffectiveLevel());
    }
    return modifierValue;
  }

  /** Adds the tooltip for the module */
  public static void addResistanceTooltip(IToolStackView tool, Modifier modifier, float amount, @Nullable Player player, List<Component> tooltip) {
    float cap;
    if (player != null) {
      cap = ProtectionModifierHook.getProtectionCap(player.getCapability(TinkerDataCapability.CAPABILITY));
    } else {
      cap = Math.min(20f + tool.getModifierLevel(TinkerModifiers.boundless.getId()) * 2.5f, 20 * 0.95f);
    }
    tooltip.add(modifier.applyStyle(
      Component.literal(Util.PERCENT_BOOST_FORMAT.format(Math.min(amount, cap) / 25f))
        .append(" ").append(Component.translatable(modifier.getTranslationKey() + ".resistance"))));
  }

  @Override
  public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    if (condition.matches(tool, modifier)) {
      addResistanceTooltip(tool, modifier.getModifier(), amount.compute(modifier.getEffectiveLevel()), player, tooltip);
    }
  }

  @Override
  public RecordLoadable<ProtectionModule> getLoader() {
    return LOADER;
  }


  /* Builder */

  /* Creates a new builder instance */
  public static Builder source(IJsonPredicate<DamageSource> source) {
    return new Builder(source);
  }

  /* Creates a new builder instance */
  @SafeVarargs
  public static Builder source(IJsonPredicate<DamageSource>... sources) {
    return source(DamageSourcePredicate.and(sources));
  }

  @Setter
  @Accessors(fluent = true)
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder extends ModuleBuilder.Stack<Builder> implements LevelingValue.Builder<ProtectionModule> {
    private final IJsonPredicate<DamageSource> source;
    private IJsonPredicate<LivingEntity> entity = LivingEntityPredicate.ANY;

    @Override
    public ProtectionModule amount(float flat, float eachLevel) {
      return new ProtectionModule(source, entity, new LevelingValue(flat, eachLevel), condition);
    }
  }
}
