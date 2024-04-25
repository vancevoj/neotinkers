package slimeknights.tconstruct.tools.modifiers.traits.general;

import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.modifiers.slotless.OverslimeModifier;

public class OverlordModifier extends Modifier implements ToolStatsModifierHook {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
  }

  @Override
  public int getPriority() {
    return 50; // after all the stuff
  }

  @Override
  public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
    float level = Math.min(modifier.getEffectiveLevel(), 6);
    // add 10% of current durability as overslime
    // TODO: consider if we should cancel out overcast's bonus here or let them stack. If they stack, it will be a lot of overslime
    OverslimeModifier.OVERSLIME_STAT.add(builder, builder.getStat(ToolStats.DURABILITY) * level * 0.1f / builder.getMultiplier(ToolStats.DURABILITY));
    // subtract 15% durability per level, capped at 6 levels (90%)
    ToolStats.DURABILITY.multiply(builder, 1 - level * 0.15f);
  }
}
