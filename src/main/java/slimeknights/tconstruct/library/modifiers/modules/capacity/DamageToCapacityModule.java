package slimeknights.tconstruct.library.modifiers.modules.capacity;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.CapacityBarHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/** Module that restores capacity when you take damage */
public record DamageToCapacityModule(IJsonPredicate<DamageSource> source, LevelingValue multiplier, boolean reduceDamage, @Nullable ModifierId owner) implements ModifierModule, ModifyDamageModifierHook, CapacitySourceModule {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<DamageToCapacityModule>defaultHooks(ModifierHooks.MODIFY_DAMAGE);
  public static final RecordLoadable<DamageToCapacityModule> LOADER = RecordLoadable.create(
    DamageSourcePredicate.LOADER.requiredField("source", DamageToCapacityModule::source),
    LevelingValue.LOADABLE.requiredField("multiplier", DamageToCapacityModule::multiplier),
    BooleanLoadable.INSTANCE.requiredField("reduce_damage", DamageToCapacityModule::reduceDamage),
    OWNER_FIELD,
    DamageToCapacityModule::new);

  @Override
  public RecordLoadable<? extends IHaveLoader> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
    //  if it matches, absorb it
    if (this.source.matches(source)) {
      ModifierEntry barModifier = barModifier(tool, modifier);
      CapacityBarHook bar = barModifier.getHook(ModifierHooks.CAPACITY_BAR);
      // absorbed damage becomes capacity
      int capacity = bar.getCapacity(tool, barModifier);
      int current = bar.getAmount(tool);
      // once it fills though, you take the damage directly
      if (current < capacity) {
        int added = Math.min(capacity - current, Mth.ceil(amount * multiplier.compute(modifier.getEffectiveLevel())));
        bar.setAmount(tool, barModifier, current + added);
        if (reduceDamage) {
          amount -= added;
        }
      }
    }
    return amount;
  }
}
