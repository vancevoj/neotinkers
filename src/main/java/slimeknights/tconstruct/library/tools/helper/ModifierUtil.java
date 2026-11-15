package slimeknights.tconstruct.library.tools.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerToolActions;
import slimeknights.tconstruct.tools.TinkerTools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Generic modifier hooks that don't quite fit elsewhere */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModifierUtil {
  /**
   * Tool action for blocking with a tool. NeoForge removed {@code ItemAbilities.SHIELD_BLOCK} in 26.3, abilities are interned by name
   * so this matches every other use of {@code ItemAbility.get("shield_block")}. Vanilla no longer queries it, blocking requires the
   * {@link DataComponents#BLOCKS_ATTACKS} component which {@link #updateCombatComponents(ItemStack, IToolStackView, HolderLookup.Provider)} adds.
   */
  public static final ItemAbility SHIELD_BLOCK = ItemAbility.get("shield_block");
  /** Seconds a shield is disabled by a tool with {@link TinkerToolActions#SHIELD_DISABLE}, matching the 100 tick shield cooldown from before 1.21.2 */
  private static final float SHIELD_DISABLE_SECONDS = 5.0f;

  /** Drops an item at the given position */
  public static void dropItem(Level level, double x, double y, double z, ItemStack stack) {
    if (!stack.isEmpty() && !level.isClientSide()) {
      ItemEntity ent = new ItemEntity(level, x, y, z, stack);
      ent.setDefaultPickUpDelay();
      RandomSource rand = level.getRandom();
      ent.setDeltaMovement(ent.getDeltaMovement().add((rand.nextFloat() - rand.nextFloat()) * 0.1F,
                                                      rand.nextFloat() * 0.05F,
                                                      (rand.nextFloat() - rand.nextFloat()) * 0.1F));
      level.addFreshEntity(ent);
    }
  }

  /** Drops an item at the entity position */
  public static void dropItem(Entity target, ItemStack stack) {
    dropItem(target.level(), target.getX(), target.getY() + 1, target.getZ(), stack);
  }

  /** Drops an item at the entity position */
  public static void dropItem(Level level, Vec3 location, ItemStack stack) {
    dropItem(level, location.x(), location.y(), location.z(), stack);
  }

  /** Gets the entity as a living entity, or null if they are not a living entity */
  @Nullable
  public static LivingEntity asLiving(@Nullable Entity entity) {
    if (entity instanceof LivingEntity living) {
      return living;
    }
    return null;
  }

  /** Gets the entity as a player, or null if they are not a player */
  @Nullable
  public static Player asPlayer(@Nullable Entity entity) {
    if (entity instanceof Player player) {
      return player;
    }
    return null;
  }

  /**
   * Checks if the given entity pays the resource costs of using a tool, such as draining the tank or consuming ammo.
   * Creative players get their resources for free, matching {@link ToolDamageUtil#directDamage(IToolStackView, int, LivingEntity, ItemStack)} skipping durability for them.
   * @param entity  Entity using the tool. Anything that is not a creative player consumes, including mobs and null.
   * @return  True if resources should be consumed.
   */
  public static boolean consumesResources(@Nullable LivingEntity entity) {
    return !(entity instanceof Player player) || consumesResources(player);
  }

  /**
   * Checks if the given player pays the resource costs of using a tool, such as draining the tank or consuming ammo.
   * Creative players get their resources for free, matching {@link ToolDamageUtil#directDamage(IToolStackView, int, LivingEntity, ItemStack)} skipping durability for them.
   * @param player  Player using the tool. Null consumes, as only creative players get their resources for free.
   * @return  True if resources should be consumed.
   */
  public static boolean consumesResources(@Nullable Player player) {
    return player == null || !player.isCreative();
  }

  /**
   * Direct method to get the level of a modifier from a stack. If you need to get multiple modifier levels, using {@link ToolStack} is faster
   * @param stack     Stack to check
   * @param modifier  Modifier to search for
   * @return  Modifier level, or 0 if not present or the stack is not modifiable
   */
  public static int getModifierLevel(ItemStack stack, ModifierId modifier) {
    if (!stack.isEmpty() && stack.is(TinkerTags.Items.MODIFIABLE)) {
      CompoundTag nbt = ToolDataComponents.getTag(stack);
      if (nbt != null && nbt.get(ToolStack.TAG_MODIFIERS) instanceof ListTag list) {
        int size = list.size();
        if (size > 0) {
          String key = modifier.toString();
          for (int i = 0; i < size; i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            if (key.equals(entry.getStringOr(ModifierEntry.TAG_MODIFIER, ""))) {
              return entry.getIntOr(ModifierEntry.TAG_LEVEL, 0);
            }
          }
        }
      }
    }
    return 0;
  }

  /** Checks if the given stack has upgrades */
  public static boolean hasUpgrades(ItemStack stack) {
    if (!stack.isEmpty() && stack.is(TinkerTags.Items.MODIFIABLE)) {
      CompoundTag nbt = ToolDataComponents.getTag(stack);
      return nbt != null && !nbt.getListOrEmpty(ToolStack.TAG_UPGRADES).isEmpty();
    }
    return false;
  }

  /** Checks if the given slot may contain armor */
  public static boolean validArmorSlot(LivingEntity living, EquipmentSlot slot) {
    return slot.isArmor() || living.getItemBySlot(slot).is(TinkerTags.Items.HELD);
  }

  /** Checks if the given slot may contain armor */
  public static boolean validArmorSlot(IToolStackView tool, EquipmentSlot slot) {
    return slot.isArmor() || tool.hasTag(TinkerTags.Items.HELD);
  }

  /** Shortcut to get a volatile flag when the tool stack is not needed otherwise */
  public static boolean checkVolatileFlag(ItemStack stack, Identifier flag) {
    CompoundTag nbt = ToolDataComponents.getTag(stack);
    if (nbt != null && nbt.get(ToolStack.TAG_VOLATILE_MOD_DATA) instanceof CompoundTag volatileData) {
      return volatileData.getBooleanOr(flag.toString(), false);
    }
    return false;
  }

  /** Shortcut to get a persistent flag when the tool stack is not needed otherwise */
  public static boolean checkPersistentPresent(ItemStack stack, Identifier key) {
    CompoundTag nbt = ToolDataComponents.getTag(stack);
    if (nbt != null && nbt.get(ToolStack.TAG_PERSISTENT_MOD_DATA) instanceof CompoundTag persistent) {
      return persistent.contains(key.toString());
    }
    return false;
  }

  /** Shortcut to get a volatile int value when the tool stack is not needed otherwise */
  public static int getVolatileInt(ItemStack stack, Identifier flag) {
    CompoundTag nbt = ToolDataComponents.getTag(stack);
    if (nbt != null && nbt.get(ToolStack.TAG_VOLATILE_MOD_DATA) instanceof CompoundTag volatileData) {
      return volatileData.getIntOr(flag.toString(), 0);
    }
    return 0;
  }

  /** Shortcut to get a volatile int value when the tool stack is not needed otherwise */
  public static int getPersistentInt(ItemStack stack, Identifier flag, int defealtValue) {
    CompoundTag nbt = ToolDataComponents.getTag(stack);
    if (nbt != null && nbt.get(ToolStack.TAG_PERSISTENT_MOD_DATA) instanceof CompoundTag persistent) {
      // old check was contains(key, TAG_INT), which also matched any other numeric tag type
      if (persistent.get(flag.toString()) instanceof NumericTag numeric) {
        return numeric.intValue();
      }
    }
    return defealtValue;
  }

  /** Shortcut to get a persistent string value when the tool stack is not needed otherwise */
  public static String getPersistentString(ItemStack stack, Identifier flag) {
    CompoundTag nbt = ToolDataComponents.getTag(stack);
    if (nbt != null && nbt.get(ToolStack.TAG_PERSISTENT_MOD_DATA) instanceof CompoundTag persistent) {
      return persistent.getStringOr(flag.toString(), "");
    }
    return "";
  }

  /** Checks if a tool can perform the given action */
  public static boolean canPerformAction(IToolStackView tool, ItemAbility action) {
    if (!tool.isBroken()) {
      // can the tool do this action inherently?
      if (tool.getHook(ToolHooks.TOOL_ACTION).canPerformAction(tool, action)) {
        return true;
      }
      for (ModifierEntry entry : tool.getModifierList()) {
        if (entry.getHook(ModifierHooks.TOOL_ACTION).canPerformAction(tool, entry, action)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Makes the tool use the blocking animation if the blocking modifier is installed, falling back to the given animation.
   * Allows your tool to block while charging up.
   */
  public static ItemUseAnimation blockWhileCharging(IToolStackView tool, ItemUseAnimation fallback) {
    return canPerformAction(tool, SHIELD_BLOCK) ? ItemUseAnimation.BLOCK : fallback;
  }

  /**
   * Creates the blocking component given to tools that can block. Matches the vanilla shield, which replicates the blocking
   * behavior before 1.21.5: 5 tick block delay, full block in front of the holder, 1 + damage durability lost when blocking 3 or more damage.
   * Tinkers' block amount and block angle stats are applied on top by the shield block event handler.
   * @param registries  Registry access, needed for the damage type tag
   * @return  Blocking component
   */
  public static BlocksAttacks createBlockingComponent(HolderLookup.Provider registries) {
    return new BlocksAttacks(
      0.25f, 1.0f,
      List.of(new BlocksAttacks.DamageReduction(90.0f, Optional.empty(), 0.0f, 1.0f)),
      new BlocksAttacks.ItemDamageFunction(3.0f, 1.0f, 1.0f),
      Optional.of(registries.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
      Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK));
  }

  /**
   * Updates the vanilla combat components on a modifiable stack based on the tool actions of the tool.
   * Since 1.21.5 vanilla reads shield blocking and shield disabling from per stack components instead of asking the item:
   * <ul>
   *   <li>{@link DataComponents#BLOCKS_ATTACKS} replaces {@code canPerformAction(SHIELD_BLOCK)}, added when the tool can perform {@link #SHIELD_BLOCK}.</li>
   *   <li>{@link DataComponents#WEAPON} replaces {@code IItemExtension#canDisableShield}, added when the tool can perform {@link TinkerToolActions#SHIELD_DISABLE}.
   *       It deals no item damage per attack as tool damage is handled by Tinkers attack logic.</li>
   * </ul>
   * Called from the inventory tick of modifiable items, so the components follow modifier changes. Only changes the stack when the state changed.
   * @param stack       Stack to update
   * @param tool        Tool instance for the stack
   * @param registries  Registry access
   */
  public static void updateCombatComponents(ItemStack stack, IToolStackView tool, HolderLookup.Provider registries) {
    // matches the old canPerformAction overrides, which rejected stacked tools
    boolean single = stack.getCount() == 1;
    // blocking
    boolean canBlock = single && canPerformAction(tool, SHIELD_BLOCK);
    if (canBlock != stack.has(DataComponents.BLOCKS_ATTACKS)) {
      if (canBlock) {
        stack.set(DataComponents.BLOCKS_ATTACKS, createBlockingComponent(registries));
      } else {
        stack.remove(DataComponents.BLOCKS_ATTACKS);
      }
    }
    // shield disabling
    boolean canDisable = single && canPerformAction(tool, TinkerToolActions.SHIELD_DISABLE);
    Weapon weapon = stack.get(DataComponents.WEAPON);
    if (canDisable) {
      if (weapon == null || weapon.disableBlockingForSeconds() != SHIELD_DISABLE_SECONDS) {
        stack.set(DataComponents.WEAPON, new Weapon(0, SHIELD_DISABLE_SECONDS));
      }
    } else if (weapon != null) {
      stack.remove(DataComponents.WEAPON);
    }
  }

  /** Calculates inaccuracy from the conditional tool stat. */
  public static float getInaccuracy(IToolStackView tool, @Nullable LivingEntity living) {
    return 3 * (1 / ConditionalStatModifierHook.getModifiedStat(tool, living, ToolStats.ACCURACY) - 1);
  }

  /** @deprecated use {@link GeneralInteractionModifierHook#addCooldown(IToolStackView, Player, float)} */
  @Deprecated(forRemoval = true)
  public static void addCooldown(IToolStackView tool, Player player) {
    GeneralInteractionModifierHook.addCooldown(tool, player, 1);
  }

  /** Checks if this modifier is the one actively being used. Used for failure sound effects. */
  public static boolean isActiveModifier(IToolStackView tool, ModifierEntry modifier, ModifierEntry activeModifier) {
    // active modifier being us, or a bow is firing and no drawback ammo
    return modifier == activeModifier || (activeModifier.getLevel() == 0 && !tool.getPersistentData().contains(ModifiableLauncherItem.KEY_DRAWBACK_AMMO));
  }

  /**
   * Called before you call {@link Projectile#discard()} to update the fishing rod stack on the player.
   *
   * @param projectile  Projectile, will check if its our fishing bobber.
   * @param damage      Damage to deal to the rod.
   * @param applyCooldown  If true, applies draw speed as an item cooldown.
   * @return hand containing the fishing rod, or null if its in neither hand.
   */
  @SuppressWarnings("UnusedReturnValue") // API
  @Nullable
  public static InteractionHand updateFishingRod(Projectile projectile, int damage, boolean applyCooldown) {
    return updateFishingRod(projectile, damage, applyCooldown, ModifierId.EMPTY);
  }

  /**
   * Called before you call {@link Projectile#discard()} to update the fishing rod stack on the player.
   *
   * @param projectile  Projectile, will check if its our fishing bobber.
   * @param damage      Damage to deal to the rod.
   * @param applyCooldown  If true, applies draw speed as an item cooldown.
   * @param cause       Modifier causing the retraction.
   * @return hand containing the fishing rod, or null if its in neither hand.
   */
  @Nullable
  public static InteractionHand updateFishingRod(Projectile projectile, int damage, boolean applyCooldown, ModifierId cause) {
    if (projectile.getType() == TinkerTools.fishingHook.get() && projectile.getOwner() instanceof LivingEntity living) {
      ItemStack stack = living.getMainHandItem();
      InteractionHand hand = InteractionHand.MAIN_HAND;
      // must be able to cast
      if (!stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
        stack = living.getOffhandItem();
        if (!stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
          return null;
        }
        hand = InteractionHand.OFF_HAND;
      }
      // must be modifiable
      if (stack.is(TinkerTags.Items.MODIFIABLE)) {
        // skip making the tool stack object if not needed, might be asking just for the hand.
        if (applyCooldown || damage > 0) {
          IToolStackView tool = ToolStack.from(stack);
          // trigger cooldown on the item
          if (applyCooldown && living instanceof Player player) {
            addCooldown(tool, player);
          }
          // damage the rod
          if (damage > 0) {
            // if we are applying cooldown, means this is a full retraction from block so this was primary damage
            // no cooldown is done on secondary effects like entity hitting
            ToolDamageUtil.damageAnimated(tool, damage, living, hand, cause);
          }
        }
        return hand;
      }
    }
    return null;
  }

  /** Interface used for {@link #foodConsumer} */
  public interface FoodConsumer {
    /** Called when food is eaten to notify compat that food was eaten */
    void onConsume(Player player, ItemStack stack, int hunger, float saturation);

    /** Called when a list of foods is eaten at once is eaten to notify compat that food was eaten */
    default void onConsume(Player player, List<ItemStack> stacks, int hunger, float saturation) {}
  }

  /** Instance of the current food consumer, will be either no-op or an implementation calling the Diet API, never null. */
  @Nonnull
  public static FoodConsumer foodConsumer = (player, stack, hunger, saturation) -> {};

  /* Shield disabling */
  /** Map of how to disable shields for different targets */
  private static final Map<EntityType<?>, Consumer<Entity>> SHIELD_DISABLER = new HashMap<>();

  /** Registers a method for shield disabling */
  public static void registerShieldDisabler(Consumer<Entity> disabler, EntityType<?>... types) {
    for (EntityType<?> type : types){
      SHIELD_DISABLER.putIfAbsent(type, disabler);
    }
  }

  /** Disables shield for the target entity */
  public static void disableShield(Entity entity) {
    Consumer<Entity> consumer = SHIELD_DISABLER.get(entity.getType());
    if (consumer != null) {
      consumer.accept(entity);
    }
  }
}
