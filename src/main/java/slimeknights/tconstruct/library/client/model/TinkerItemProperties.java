package slimeknights.tconstruct.library.client.model;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.ItemAbilities;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Properties for tinker tools */
@SuppressWarnings("deprecation")
public class TinkerItemProperties {
  /** ID for broken property */
  private static final Identifier BROKEN_ID = TConstruct.getResource("broken");
  /** Property declaring broken */
  private static final ItemPropertyFunction BROKEN = (stack, level, entity, seed) -> {
    return ToolDamageUtil.isBroken(stack) ? 1 : 0;
  };

  /** ID for ammo property */
  private static final Identifier AMMO_ID = TConstruct.getResource("ammo");
  /** ID of fireworks rocket for NBT check */
  private static final String FIREWORKS_ID = "minecraft:firework_rocket";
  /** Int declaring ammo type */
  private static final ItemPropertyFunction AMMO = (stack, level, entity, seed) -> {
    // tools store their NBT in the tconstruct:tool_data component, NOT minecraft:custom_data; reading the latter
    // always came back empty so the loaded crossbow model never rendered (issue #13). Mirror ModifierUtil.
    CompoundTag nbt = ToolDataComponents.getTag(stack);
    if (nbt != null && nbt.contains(ToolStack.TAG_PERSISTENT_MOD_DATA, Tag.TAG_COMPOUND)) {
      CompoundTag persistentData = nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA);
      CompoundTag ammo = persistentData.getCompound(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO.toString());
      if (!ammo.isEmpty()) {
        // no sense having two keys for ammo, just set 1 for arrow, 2 for fireworks
        return FIREWORKS_ID.equals(ammo.getString("id")) ? 2 : 1;
      }
    }
    return 0;
  };

  /** ID for the pulling property */
  private static final Identifier CHARGING_ID = TConstruct.getResource("charging");
  /** Boolean indicating the bow is pulling */
  private static final ItemPropertyFunction CHARGING = (stack, level, holder, seed) -> {
    if (holder != null && holder.isUsingItem() && holder.getUseItem() == stack) {
      ItemUseAnimation anim = stack.getUseAnimation();
      if (anim == ItemUseAnimation.BLOCK) {
        return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 2.5f : 2;
      }
      // TODO 1.21: space this out a bit more
      if (anim == ItemUseAnimation.TRIDENT) {
        // shouldn't need to worry about arrows on spearing, everything supporting arrows uses just bow or block
        return 1.75f;
      }
      if (anim != ItemUseAnimation.EAT && anim != ItemUseAnimation.DRINK) {
        return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 1.5f : 1;
      }
    }
    return 0;
  };
  /** ID for the pull property */
  private static final Identifier CHARGE_ID = TConstruct.getResource("charge");
  /** Property for bow pull amount */
  private static final ItemPropertyFunction CHARGE = (stack, level, holder, seed) -> {
    if (holder == null || holder.getUseItem() != stack) {
      return 0.0F;
    }
    int drawtime = ModifierUtil.getPersistentInt(stack, GeneralInteractionModifierHook.KEY_DRAWTIME, -1);
    return drawtime == -1 ? 0 : (float)(stack.getUseDuration(holder) - holder.getUseItemRemainingTicks()) / drawtime;
  };
  /** ID for the cast fishing rods */
  private static final Identifier CAST_ID = TConstruct.getResource("cast");
  /** Property for casting a fishing rod */
  private static final ItemPropertyFunction CAST = (stack, level, holder, seed) -> {
    // must be a fishing rod, and the player must be fishing
    // does player check first since its the fastest, avoids NBT parsing
    if (holder instanceof Player player && player.fishing != null && stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
      // must be in a hand, but if both hands have fishing rods, must be the one in the main hand
      ItemStack mainhand = holder.getMainHandItem();
      if (mainhand == stack || holder.getOffhandItem() == stack && !mainhand.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
        return 1;
      }
    }
    return 0;
  };

  /** Registers properties for a tool, including the option to have charge/block animations */
  public static void registerBrokenProperty(Item item) {
    ItemProperties.register(item, BROKEN_ID, BROKEN);
  }

  /** Registers properties for a tool, including the option to have charge/block animations */
  public static void registerToolProperties(ItemLike itemlike) {
    Item item = itemlike.asItem();
    registerBrokenProperty(item);
    ItemProperties.register(item, CHARGING_ID, CHARGING);
    ItemProperties.register(item, CHARGE_ID, CHARGE);
    ItemProperties.register(item, CAST_ID, CAST);
  }

  /** Registers properties for a bow */
  public static void registerCrossbowProperties(ItemLike item) {
    registerToolProperties(item);
    ItemProperties.register(item.asItem(), AMMO_ID, AMMO);
  }
}
