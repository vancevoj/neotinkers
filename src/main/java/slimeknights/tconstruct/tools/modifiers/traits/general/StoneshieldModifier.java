package slimeknights.tconstruct.tools.modifiers.traits.general;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ProcessLootModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.DurabilityShieldModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class StoneshieldModifier extends DurabilityShieldModifier implements ProcessLootModifierHook {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, ModifierHooks.PROCESS_LOOT);
  }

  @Override
  public int getShieldCapacity(IToolStackView tool, ModifierEntry modifier) {
    return (int)(modifier.getEffectiveLevel() * 100 * tool.getMultiplier(ToolStats.DURABILITY));
  }

  @Override
  public int getPriority() {
    // higher than overslime, to ensure this is removed first
    return 175;
  }

  @Override
  public void processLoot(IToolStackView tool, ModifierEntry modifier, List<ItemStack> generatedLoot, LootContext context) {
    Iterator<ItemStack> iterator = generatedLoot.iterator();
    int addedShield = 0;
    // 20% chance per level of consuming each stone
    float chance = modifier.getEffectiveLevel() * 0.20f;
    while (iterator.hasNext()) {
      ItemStack stack = iterator.next();
      // if the item is a stone, num time
      if (stack.is(TinkerTags.Items.STONESHIELDS)) {
        // 100% chance? just add the full count
        if (chance >= 1.0f) {
          addedShield += stack.getCount();
          iterator.remove();
        } else {
          // smaller chance, independant chance per stone
          int reduced = 0;
          for (int i = 0; i < stack.getCount(); i++) {
            if (RANDOM.nextFloat() < chance) {
              reduced++;
            }
          }
          // if we ate them all, remove, otherwise just shrink
          if (reduced == stack.getCount()) {
            iterator.remove();
          } else {
            stack.shrink(reduced);
          }
          addedShield += reduced;
        }
      }
    }

    // if we found any stone, add shield
    if (addedShield > 0) {
      // 3 stoneshield per stone eaten
      addShield(tool, modifier, addedShield * 3);
    }
  }

  /* Display */

  @Nullable
  @Override
  public Boolean showDurabilityBar(IToolStackView tool, ModifierEntry modifier) {
    // only show if we have any shield
    return getShield(tool) > 0 ? true : null;
  }

  @Override
  public int getDurabilityRGB(IToolStackView tool, ModifierEntry modifier) {
    if (getShield(tool) > 0) {
      // stoneshield shows in light grey
      return 0x7F7F7F;
    }
    return -1;
  }
}
