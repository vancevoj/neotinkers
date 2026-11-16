package slimeknights.tconstruct.library.modifiers.hook.interaction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/** Hook that runs while the tool is in the inventory */
public interface InventoryTickModifierHook {
  /**
   * Called when the stack updates in the player inventory.
   * Since 1.21.5 vanilla only ticks items server side, tinker items also call this from the NeoForge client inventory tick so it runs on both sides as before.
   * @param tool           Current tool instance
   * @param modifier       Modifier running the hook
   * @param world          World containing tool
   * @param holder         Entity holding tool
   * @param itemSlot       Slot containing this tool. Note this may be from the hotbar, main inventory, or armor inventory.
   *                       Since 1.21.5 vanilla only tells us the equipment slot, so this is the inventory index of that equipment slot (see {@link #getSlotIndex(Entity, EquipmentSlot)}),
   *                       or {@link Inventory#NOT_FOUND_INDEX} for an item in the main inventory that is not selected.
   * @param isSelected     If true, this item is currently in the player's main hand
   * @param isCorrectSlot  If true, this item is in the proper slot. For tools, that is main hand or off hand. For armor, this means its in the correct armor slot
   * @param stack          Item stack instance to check other slots for the tool. Do not modify
   */
  void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack);

  /**
   * Gets the inventory index for the given equipment slot, for the {@code itemSlot} parameter of {@link #onInventoryTick(IToolStackView, ModifierEntry, Level, LivingEntity, int, boolean, boolean, ItemStack)}.
   * Vanilla stopped passing the inventory index to {@link net.minecraft.world.item.Item#inventoryTick(ItemStack, net.minecraft.server.level.ServerLevel, Entity, EquipmentSlot)} in 1.21.5.
   * @param entity  Entity holding the item
   * @param slot    Equipment slot from the vanilla inventory tick, null for main inventory slots that are not selected
   * @return  Index matching the player {@link Inventory} layout, or {@link Inventory#NOT_FOUND_INDEX} if not in an equipment slot
   */
  static int getSlotIndex(Entity entity, @Nullable EquipmentSlot slot) {
    if (slot == null) {
      return Inventory.NOT_FOUND_INDEX;
    }
    return switch (slot) {
      case MAINHAND -> entity instanceof Player player ? player.getInventory().getSelectedSlot() : 0;
      case OFFHAND -> Inventory.SLOT_OFFHAND;
      case BODY -> Inventory.SLOT_BODY_ARMOR;
      case SADDLE -> Inventory.SLOT_SADDLE;
      // armor slots
      default -> slot.getIndex(Inventory.INVENTORY_SIZE);
    };
  }

  /**
   * Handles ticking a modifiable item that works when held. Armor uses different logic.
   * Call from both {@link net.minecraft.world.item.Item#inventoryTick(ItemStack, net.minecraft.server.level.ServerLevel, Entity, EquipmentSlot)}
   * and {@link net.neoforged.neoforge.common.extensions.IItemExtension#clientInventoryTick(ItemStack, Level, Entity, EquipmentSlot)} to tick on both sides.
   * @param stack   Modifiable stack
   * @param level   World instance
   * @param entity  Entity holding the tool
   * @param slot    Equipment slot from the vanilla tick, null if in the main inventory and not selected
   */
  static void heldInventoryTick(ItemStack stack, Level level, Entity entity, @Nullable EquipmentSlot slot) {
    heldInventoryTick(stack, level, entity, getSlotIndex(entity, slot), slot == EquipmentSlot.MAINHAND);
  }

  /**
   * Handles ticking a modifiable item that works when held. Armor uses different logic
   * @param stack       Modifiable stack
   * @param worldIn     World instance
   * @param entityIn    Entity holding the tool
   * @param itemSlot    Slot with the tool
   * @param isSelected  If true, the tool is selected
   */
  static void heldInventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
    // don't care about non-living, they skip most tool context
    if (entityIn instanceof LivingEntity) {
      ToolStack tool = ToolStack.from(stack);
      if (!worldIn.isClientSide()) {
        tool.ensureHasData();
      }
      List<ModifierEntry> modifiers = tool.getModifierList();
      if (!modifiers.isEmpty()) {
        LivingEntity living = (LivingEntity) entityIn;
        // we pass in the stack for most custom context, but for the sake of armor it is easier to tell them that this is the correct slot for effects
        boolean isHeld = isSelected || living.getOffhandItem() == stack;
        for (ModifierEntry entry : modifiers) {
          entry.getHook(ModifierHooks.INVENTORY_TICK).onInventoryTick(tool, entry, worldIn, living, itemSlot, isSelected, isHeld, stack);
        }
      }
    }
  }

  /** Merger that runs all hooks one after another */
  record AllMerger(Collection<InventoryTickModifierHook> modules) implements InventoryTickModifierHook {
    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
      for (InventoryTickModifierHook module : modules) {
        module.onInventoryTick(tool, modifier, world, holder, itemSlot, isSelected, isCorrectSlot, stack);
      }
    }
  }
}
