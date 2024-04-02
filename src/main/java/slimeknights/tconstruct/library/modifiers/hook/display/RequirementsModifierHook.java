package slimeknights.tconstruct.library.modifiers.hook.display;

import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Hook for recipe display of modifier requirements. Note that the actual requirement are implemented via {@link slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook}
 */
public interface RequirementsModifierHook {
  /** Gets the list of modifiers to display on tools in recipe viewers */
  default List<ModifierEntry> displayModifiers(ModifierEntry entry) {
    return List.of();
  }

  /** Gets the hint about the modifier requirements, or null if no hint */
  @Nullable
  default Component requirementsError(ModifierEntry entry) {
    return null;
  }

  /** Merger that returns the first match, intended for level based dispatching */
  record FirstMerger(Collection<RequirementsModifierHook> modules) implements RequirementsModifierHook {
    @Override
    public List<ModifierEntry> displayModifiers(ModifierEntry entry) {
      for (RequirementsModifierHook module : modules) {
        List<ModifierEntry> modifiers = module.displayModifiers(entry);
        if (!modifiers.isEmpty()) {
          return modifiers;
        }
      }
      return List.of();
    }

    @Nullable
    @Override
    public Component requirementsError(ModifierEntry entry) {
      for (RequirementsModifierHook module : modules) {
        Component hint = module.requirementsError(entry);
        if (hint != null) {
          return hint;
        }
      }
      return null;
    }
  }
}
