package slimeknights.tconstruct.library.modifiers.modules.unserializable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Modifier that to keep track of a stat that is contributed to by all armor pieces. Can scale the stat on different modifiers or for incremental and can use float values unlike {@link ArmorLevelModule}.
 * @see ArmorLevelModule
 * @see TinkerDataKey
 */
public record ArmorStatModule(TinkerDataKey<Float> key, float scale, boolean allowBroken, @Nullable TagKey<Item> heldTag) implements HookProvider, EquipmentChangeModifierHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ArmorStatModule>defaultHooks(ModifierHooks.EQUIPMENT_CHANGE);

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
    ArmorStatModule.addStatIfArmor(tool, context, key, modifier.getEffectiveLevel() * scale, allowBroken, heldTag);
  }

  @Override
  public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
    ArmorStatModule.addStatIfArmor(tool, context, key, -modifier.getEffectiveLevel() * scale, allowBroken, heldTag);
  }


  /* Helpers */

  /**
   * Adds to the armor stat for the given key. Make sure to subtract on unequip if you add on equip, it will not automatically be removed.
   * @param context  Equipment change context
   * @param key      Key to modify
   * @param amount   Amount to add
   */
  public static void addStat(EquipmentChangeContext context, TinkerDataKey<Float> key, float amount) {
    context.getTinkerData().ifPresent(data -> {
      float totalLevels = data.get(key, 0f) + amount;
      if (totalLevels <= 0.005f) {
        data.remove(key);
      } else {
        data.put(key, totalLevels);
      }
    });
  }

  /**
   * Adds to the armor stat for the given key if the tool is in a valid armor slot
   * @param tool     Tool instance
   * @param context  Equipment change context
   * @param key      Key to modify
   * @param amount   Amount to add
   * @param heldTag  Tag to check to validate held items, null means held disallowed
   */
  public static void addStatIfArmor(IToolStackView tool, EquipmentChangeContext context, TinkerDataKey<Float> key, float amount, boolean allowBroken, @Nullable TagKey<Item> heldTag) {
    if (ArmorLevelModule.validSlot(tool, context.getChangedSlot(), heldTag) && (!tool.isBroken() || allowBroken)) {
      addStat(context, key, amount);
    }
  }

  /**
   * Gets the total level from the key in the entity modifier data
   * @param living  Living entity
   * @param key     Key to get
   * @return  Level from the key
   */
  public static float getStat(LivingEntity living, TinkerDataKey<Float> key) {
    return living.getCapability(TinkerDataCapability.CAPABILITY).resolve().map(data -> data.get(key)).orElse(0f);
  }
}
