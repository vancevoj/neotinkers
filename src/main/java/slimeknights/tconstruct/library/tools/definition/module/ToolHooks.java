package slimeknights.tconstruct.library.tools.definition.module;

import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionToolHook;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitHook;
import slimeknights.tconstruct.library.tools.definition.module.build.VolatileDataToolHook;
import slimeknights.tconstruct.library.tools.definition.module.interaction.InteractionToolModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedToolHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningTierToolHook;
import slimeknights.tconstruct.library.tools.definition.module.weapon.MeleeHitToolHook;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT.Builder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Modules for tool definition data */
public class ToolHooks {
  private ToolHooks() {}

  public static void init() {}


  /* Build */
  /** Hook for checking if a tool can perform a given action. TODO: rename to {@code volatile_data} */
  public static final ModifierHook<VolatileDataToolHook> VOLATILE_DATA = register("tool_volatile_data", VolatileDataToolHook.class, VolatileDataToolHook.AllMerger::new, (context, data) -> {});
  /** Hook for fetching tool traits */
  public static final ModifierHook<ToolTraitHook> TOOL_TRAITS = register("tool_traits", ToolTraitHook.class, ToolTraitHook.AllMerger::new, new ToolTraitHook() {
    @Override
    public List<ModifierEntry> getTraits(ToolDefinition definition) {
      return Collections.emptyList();
    }

    @Override
    public void addTraits(ToolDefinition definition, Builder builder) {}
  });
  /** Hook for checking if a tool can perform a given action. TODO: rename to {@code tool_action} */
  public static final ModifierHook<ToolActionToolHook> TOOL_ACTION = register("tool_tool_actions", ToolActionToolHook.class, ToolActionToolHook.AnyMerger::new, (tool, action) -> false);


  /* Mining */
  /** Hook for checking if a tool is effective against the given block */
  public static final ModifierHook<IsEffectiveToolHook> IS_EFFECTIVE = register("is_effective", IsEffectiveToolHook.class, (tool, state) -> false);
  /** Hook for modifying the tier from the stat */
  public static final ModifierHook<MiningTierToolHook> MINING_TIER = register("mining_tier", MiningTierToolHook.class, MiningTierToolHook.ComposeMerger::new, (tool, tier) -> tier);
  /** Hook for modifying the mining speed from the stat/effectiveness */
  public static final ModifierHook<MiningSpeedToolHook> MINING_SPEED = register("mining_speed_modifier", MiningSpeedToolHook.class, MiningSpeedToolHook.ComposeMerger::new, (tool, state, speed) -> speed);
  /** Logic for finding AOE blocks */
  public static final ModifierHook<AreaOfEffectIterator> AOE_ITERATOR = register("aoe_iterator", AreaOfEffectIterator.class, (tool, stack, player, state, world, origin, sideHit, match) -> Collections.emptyList());


  /* Weapon */
  /** Hook that runs after a melee hit to apply extra effects. TODO: rename to {@code after_melee_hit} */
  public static final ModifierHook<MeleeHitToolHook> MELEE_HIT = register("after_melee_hit", MeleeHitToolHook.class, MeleeHitToolHook.AllMerger::new, (tool, context, damage) -> {});


  /** Hook for configuring interaction behaviors on the tool */
  public static final ModifierHook<InteractionToolModule> INTERACTION = register("tool_interaction", InteractionToolModule.class, (t, m, s) -> true);





  /** Registers a new modifier hook under {@code tconstruct} that cannot merge */
  @SuppressWarnings("SameParameterValue")
  private static <T> ModifierHook<T> register(String name, Class<T> filter, T defaultInstance) {
    return ModifierHooks.register(TConstruct.getResource(name), filter, defaultInstance);
  }

  /** Registers a new modifier hook under {@code tconstruct} that merges */
  @SuppressWarnings("SameParameterValue")
  private static <T> ModifierHook<T> register(String name, Class<T> filter, @Nullable Function<Collection<T>,T> merger, T defaultInstance) {
    return ModifierHooks.register(TConstruct.getResource(name), filter, defaultInstance, merger);
  }
}
