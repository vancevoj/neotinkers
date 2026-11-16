package slimeknights.tconstruct.library.modifiers.modules.interaction.edible;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EdibleEffectHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/** Module for making eating a tool remove a specific effect. */
public record EdibleRemoveEffectModule(Holder<MobEffect> effect, IJsonPredicate<LivingEntity> holder, ModifierCondition<IToolStackView> condition) implements ModifierModule, EdibleEffectHook, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<EdibleRemoveEffectModule>defaultHooks(ModifierHooks.EDIBLE_EFFECT);
  public static final RecordLoadable<EdibleRemoveEffectModule> LOADER = RecordLoadable.create(
    // 1.21: LivingEntity#removeEffect takes Holder<MobEffect>; loadable resolves raw MobEffect then wraps as holder
    Loadables.MOB_EFFECT.<Holder<MobEffect>>xmap((effect, error) -> BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), (holder, error) -> holder.value()).requiredField("effect", EdibleRemoveEffectModule::effect),
    LivingEntityPredicate.LOADER.defaultField("holder", EdibleRemoveEffectModule::holder),
    ModifierCondition.TOOL_FIELD, EdibleRemoveEffectModule::new);

  public EdibleRemoveEffectModule(Holder<MobEffect> effect) {
    this(effect, LivingEntityPredicate.ANY, ModifierCondition.ANY_TOOL);
  }

  @Override
  public RecordLoadable<EdibleRemoveEffectModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void onToolEaten(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot eatenSlot, int hunger, float saturation, List<ItemStack> representativeItems) {
    if (condition.matches(tool, modifier) && holder.matches(player)) {
      player.removeEffect(effect);
    }
  }
}
