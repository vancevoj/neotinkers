package slimeknights.tconstruct.library.tools.definition.module.build;

import com.google.common.collect.ImmutableList;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.Collection;
import java.util.List;

/** Hook for tools exposing tool traits */
public interface ToolTraitHook {
  /**
   * Gets the traits for the given tool.
   * Note its preferred to use {@link #addTraits(ToolDefinition, ModifierNBT.Builder)} if possible as that will reduce number of builders used.
   */
  List<ModifierEntry> getTraits(ToolDefinition definition);

  /** Adds all traits to the given builder */
  default void addTraits(ToolDefinition definition, ModifierNBT.Builder builder) {
    builder.add(getTraits(definition));
  }

  /** Gets the traits for the given tool */
  record AllMerger(Collection<ToolTraitHook> hooks) implements ToolTraitHook {
    @Override
    public List<ModifierEntry> getTraits(ToolDefinition definition) {
      ImmutableList.Builder<ModifierEntry> traits = ImmutableList.builder();
      for (ToolTraitHook hook : hooks) {
        traits.addAll(hook.getTraits(definition));
      }
      return traits.build();
    }

    @Override
    public void addTraits(ToolDefinition definition, ModifierNBT.Builder builder) {
      for (ToolTraitHook hook : hooks) {
        hook.addTraits(definition, builder);
      }
    }
  }
}
