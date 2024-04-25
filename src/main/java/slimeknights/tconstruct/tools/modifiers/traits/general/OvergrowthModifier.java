package slimeknights.tconstruct.tools.modifiers.traits.general;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.slotless.OverslimeModifier;

public class OvergrowthModifier extends Modifier implements InventoryTickModifierHook {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
  }

  @Override
  public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
    // update 1 times a second, but skip when active (messes with pulling bow back)
    if (!world.isClientSide && holder.tickCount % 20 == 0 && holder.getUseItem() != stack) {
      OverslimeModifier overslime = TinkerModifiers.overslime.get();
      ModifierEntry entry = tool.getModifier(TinkerModifiers.overslime.getId());
      // has a 5% chance of restoring each second per level
      if (entry.getLevel() > 0 && overslime.getShield(tool) < overslime.getShieldCapacity(tool, entry) && RANDOM.nextFloat() < (modifier.getLevel() * 0.05)) {
        overslime.addOverslime(tool, entry, 1);
      }
    }
  }
}
