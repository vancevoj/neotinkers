package slimeknights.tconstruct.tools.modifiers.traits.general;

import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.slotless.OverslimeModifier;

public class OverlordModifier extends Modifier implements ToolStatsModifierHook, VolatileDataModifierHook {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.VOLATILE_DATA);
  }

  @Override
  public int getPriority() {
    return 80; // after overcast
  }

  /** Gets the durability boost per level */
  private int getBoost(IToolContext context, int level, float perLevel) {
    return (int)(context.getDefinitionData().getBaseStat(ToolStats.DURABILITY) * perLevel * level);
  }

  @Override
  public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
    OverslimeModifier overslime = TinkerModifiers.overslime.get();
    // gains +15% of the durability per level, note that base stats does not consider the durability modifier
    overslime.addCapacity(volatileData, getBoost(context, modifier.getLevel(), 0.10f * context.getDefinition().getData().getMultiplier(ToolStats.DURABILITY)));
  }

  @Override
  public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
    // at most subtract 90% durability, note this runs before the tool durability modifier
    ToolStats.DURABILITY.add(builder, -getBoost(context, Math.min(modifier.getLevel(), 6), 0.15f));
  }
}
