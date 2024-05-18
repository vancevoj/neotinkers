package slimeknights.tconstruct.library.modifiers.hook.behavior;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;
import java.util.List;

/** Hook for modifying items in a loot table. */
public interface ProcessLootModifierHook {
  /**
   * Called on entity or block loot to allow modifying loot
   * @param tool           Current tool instance
   * @param modifier          Modifier level
   * @param generatedLoot  Current loot list before this modifier
   * @param context        Full loot context
   * TODO: can we ditch this hook in favor of just using GLMs? Just need a loot condition to detect a modifier, and it gives us a lot more flexability
   */
  void processLoot(IToolStackView tool, ModifierEntry modifier, List<ItemStack> generatedLoot, LootContext context);

  /** Merger that runs all hooks */
  record AllMerger(Collection<ProcessLootModifierHook> modules) implements ProcessLootModifierHook {
    @Override
    public void processLoot(IToolStackView tool, ModifierEntry modifier, List<ItemStack> generatedLoot, LootContext context) {
      for (ProcessLootModifierHook module : modules) {
        module.processLoot(tool, modifier, generatedLoot, context);
      }
    }
  }
}
