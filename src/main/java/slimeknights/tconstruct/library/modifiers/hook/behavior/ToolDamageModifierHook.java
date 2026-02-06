package slimeknights.tconstruct.library.modifiers.hook.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.primitive.EnumLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * Hook run when a tool is damaged to allow modifiers to change the damage amount.
 */
public interface ToolDamageModifierHook {
  /**
   * Called when the tool is damaged. Can be used to cancel, decrease, or increase the damage.
   * @param tool       Tool stack
   * @param modifier   Modifier running this hook
   * @param amount     Amount of damage to deal
   * @param holder     Entity holding the tool
   * @return  Replacement damage. Returning 0 cancels the damage and stops other modifiers from processing.
   * @deprecated use {@link #onDamageTool(IToolStackView, ModifierEntry, int, LivingEntity, ItemStack, DurabilityType)}. Overriding is okay.
   */
  @Deprecated
  int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder);

  /**
   * Called when the tool is damaged. Can be used to cancel, decrease, or increase the damage.
   * @param tool       Tool stack
   * @param modifier   Modifier running this hook
   * @param amount     Amount of damage to deal
   * @param holder     Entity holding the tool
   * @param stack      Stack instance being damaged. Useful for identifying the slot being damaged.
   * @return  Replacement damage. Returning 0 cancels the damage and stops other modifiers from processing.
   * @deprecated use {@link #onDamageTool(IToolStackView, ModifierEntry, int, LivingEntity, ItemStack, DurabilityType)}. Overriding is okay.
   */
  @Deprecated
  default int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder, @Nullable ItemStack stack) {
    return onDamageTool(tool, modifier, amount, holder);
  }

  /**
   * Called when the tool is damaged. Can be used to cancel, decrease, or increase the damage.
   * @param tool       Tool stack
   * @param modifier   Modifier running this hook
   * @param amount     Amount of damage to deal
   * @param holder     Entity holding the tool
   * @param stack      Stack instance being damaged. Useful for identifying the slot being damaged.
   * @param type       Type of durability loss being applied.
   * @return  Replacement damage. Returning 0 cancels the damage and stops other modifiers from processing.
   */
  default int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder, @Nullable ItemStack stack, DurabilityType type) {
    return onDamageTool(tool, modifier, amount, holder, stack);
  }

  /** Helper to allow distinguishing the cause of damage in modifiers. */
  enum DurabilityType {
    /** Main type of tool durability loss, triggers all relevant modifiers */
    PRIMARY,
    /** Extra damage applied after primary already applied. Used to allow modifiers with flat reductions to avoid impact of damaging multiple times. */
    SECONDARY;

    /** Loadable instance for JSON */
    public static final EnumLoadable<DurabilityType> LOADABLE = new EnumLoadable<>(DurabilityType.class);
    /** Loadable instance for JSON */
    public static final Loadable<Set<DurabilityType>> SET_LOADABLE = LOADABLE.set(ArrayLoadable.COMPACT_OR_EMPTY);

    /** Checks if the given set matches the given type. It matches on empty or set contains, to simplify an all condition. */
    public static boolean matches(Set<DurabilityType> values, DurabilityType type) {
      return values.isEmpty() || values.contains(type);
    }
  }

  /** Merger that runs all nested modules, but stops if the amount ever reaches 0 */
  record Merger(Collection<ToolDamageModifierHook> modules) implements ToolDamageModifierHook {
    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder) {
      for (ToolDamageModifierHook module : modules) {
        amount = module.onDamageTool(tool, modifier, amount, holder);
        if (amount <= 0) {
          break;
        }
      }
      return amount;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder, @Nullable ItemStack stack) {
      for (ToolDamageModifierHook module : modules) {
        amount = module.onDamageTool(tool, modifier, amount, holder, stack);
        if (amount <= 0) {
          break;
        }
      }
      return amount;
    }

    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder, @Nullable ItemStack stack, DurabilityType type) {
      for (ToolDamageModifierHook module : modules) {
        amount = module.onDamageTool(tool, modifier, amount, holder, stack, type);
        if (amount <= 0) {
          break;
        }
      }
      return amount;
    }
  }
}
