package slimeknights.tconstruct.tools.modifiers.traits;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.DurabilityShieldModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;

/** Modifier for ice's trait */
public class FrostshieldModifier extends DurabilityShieldModifier implements ModifyDamageModifierHook {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, ModifierHooks.MODIFY_HURT);
  }


  /* Shield */

  @Override
  public int getPriority() {
    // higher than overslime, to ensure this is removed first
    return 175;
  }

  @Override
  public int getShieldCapacity(IToolStackView tool, ModifierEntry modifier) {
    return (int)(modifier.getEffectiveLevel() * 100 * tool.getMultiplier(ToolStats.DURABILITY));
  }


  /* Display */

  @Override
  public Component getDisplayName(int level) {
    return super.getDisplayName();
  }

  @Nullable
  @Override
  public Boolean showDurabilityBar(IToolStackView tool, ModifierEntry modifier) {
    return getShield(tool) > 0 ? true : null;
  }

  @Override
  public int getDurabilityRGB(IToolStackView tool, ModifierEntry modifier) {
    if (getShield(tool) > 0) {
      return 0xAAFFFF;
    }
    return -1;
  }


  /* Restoring */

  @Override
  public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
    // if its freezing damage, absorb it
    if (source.is(DamageTypeTags.IS_FREEZING)) {
      // absorbed damage becomes durability
      int capacity = getShieldCapacity(tool, modifier);
      int current = getShield(tool);
      // once it fills though, you take the damage directly
      if (current < capacity) {
        int added = Math.min(capacity - current, Mth.ceil(amount));
        setShield(tool.getPersistentData(), current + added);
        amount -= added;
      }
    }
    return amount;
  }
}
