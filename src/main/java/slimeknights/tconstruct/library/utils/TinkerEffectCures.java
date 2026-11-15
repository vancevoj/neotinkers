package slimeknights.tconstruct.library.utils;

import com.google.common.collect.MapMaker;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper restoring Forge's per-stack curative items behavior, as NeoForge 26.3 removed the {@code EffectCure} tokens it was built on.
 * <p>
 * In 1.20.1 each {@link MobEffectInstance} carried a {@code List<ItemStack>} of curative items (default {@code [milk_bucket]}),
 * and {@code LivingEntity#curePotionEffects(ItemStack)} removed effects whose list contained that stack. NeoForge 1.21.1 replaced the list with
 * {@code EffectCure} tokens stored on the instance, which no longer exist. Tinkers now keeps the curative items of each instance itself:
 * <ul>
 *   <li>Instances without custom cures use the default, curable only by milk.</li>
 *   <li>Custom cures are stored per instance, persisted in the entity persistent data while the effect is active, and restored when the entity loads.</li>
 *   <li>Vanilla milk clears all effects through its consume effect, so removing an effect with custom cures not including milk is canceled while the entity finishes using such an item.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID)
public final class TinkerEffectCures {
  private TinkerEffectCures() {}

  /** Default curative items for effects that set no custom cures */
  private static final Set<Item> DEFAULT_CURES = Set.of(Items.MILK_BUCKET);
  /** Custom curative items per effect instance. Weak identity keys as instances compare by value. */
  private static final Map<MobEffectInstance, Set<Item>> CURES = new MapMaker().weakKeys().makeMap();
  /** Key in the entity persistent data storing custom cures of active effects */
  private static final String PERSISTENT_KEY = TConstruct.resourceString("effect_cures");
  /** Set while Tinkers removes effects for a cure, so the milk protection does not cancel it */
  private static final ThreadLocal<Boolean> CURING = ThreadLocal.withInitial(() -> false);

  /**
   * Gets the curative items for the given effect instance
   * @param instance  Effect instance
   * @return  Set of items that cure the effect, empty if it cannot be cured
   */
  public static Set<Item> getCures(MobEffectInstance instance) {
    return CURES.getOrDefault(instance, DEFAULT_CURES);
  }

  /**
   * Checks if the effect is cured by the given item, mirroring the legacy {@code isCurativeItem}. Milk buckets are the default curative item.
   * @param instance  Effect instance
   * @param item      Item curing the effect
   * @return  True if the item cures the effect
   */
  public static boolean isCuredBy(MobEffectInstance instance, Item item) {
    return getCures(instance).contains(item);
  }

  /**
   * Cures all effects on the entity cured by the given item, mirroring the legacy {@code curePotionEffects(ItemStack)}.
   * @param entity  Entity to cure
   * @param item    Item curing the effects
   * @return  True if any effect was removed
   * @see #isCuredBy(MobEffectInstance, Item)
   */
  public static boolean cure(LivingEntity entity, Item item) {
    if (entity.level().isClientSide()) {
      return false;
    }
    List<Holder<MobEffect>> cured = entity.getActiveEffects().stream().filter(effect -> isCuredBy(effect, item)).map(MobEffectInstance::getEffect).toList();
    if (cured.isEmpty()) {
      return false;
    }
    boolean removed = false;
    CURING.set(true);
    try {
      for (Holder<MobEffect> effect : cured) {
        removed |= entity.removeEffect(effect);
      }
    } finally {
      CURING.set(false);
    }
    return removed;
  }

  /**
   * Applies a list of curative items to an effect instance, mirroring the legacy {@code setCurativeItems} behavior.
   * <ul>
   *   <li>{@code null}: leave the effect's default cures untouched (e.g. still curable by milk).</li>
   *   <li>empty list: clear all cures, making the effect uncurable (legacy {@code setCurativeItems(new ArrayList<>())}).</li>
   *   <li>non-empty list: clear default cures and bind the effect to exactly the given items (legacy {@code clear()} then {@code add(item)}).</li>
   * </ul>
   * Call before adding the instance to the entity.
   * @param instance        Effect instance to modify
   * @param curativeItems   Items allowed to cure the effect, or null for defaults
   */
  public static void setCures(MobEffectInstance instance, @Nullable List<Item> curativeItems) {
    if (curativeItems != null) {
      CURES.put(instance, Set.copyOf(curativeItems));
    }
  }

  /**
   * Adds a curative item to an effect instance, keeping its existing cures (legacy {@code getCures().add(itemCure(item))}).
   * Call before adding the instance to the entity.
   * @param instance  Effect instance to modify
   * @param item      Item allowed to cure the effect
   */
  public static void addCure(MobEffectInstance instance, Item item) {
    Set<Item> cures = new HashSet<>(getCures(instance));
    cures.add(item);
    CURES.put(instance, Set.copyOf(cures));
  }


  /* Events */

  /** Checks if the given stack clears all effects when consumed, like milk */
  private static boolean clearsAllEffects(ItemStack stack) {
    Consumable consumable = stack.get(DataComponents.CONSUMABLE);
    return consumable != null && consumable.onConsumeEffects().stream().anyMatch(effect -> effect instanceof ClearAllStatusEffectsConsumeEffect);
  }

  /** Prevents milk from curing effects with custom cures that exclude it */
  @SubscribeEvent
  static void onEffectRemove(MobEffectEvent.Remove event) {
    MobEffectInstance instance = event.getEffectInstance();
    if (instance == null || CURING.get()) {
      return;
    }
    Set<Item> cures = CURES.get(instance);
    if (cures == null || cures.contains(Items.MILK_BUCKET)) {
      return;
    }
    LivingEntity entity = event.getEntity();
    if (entity.isUsingItem()) {
      ItemStack using = entity.getUseItem();
      if (clearsAllEffects(using) && !cures.contains(using.getItem())) {
        event.setCanceled(true);
      }
    }
  }

  /** Stores the custom cures of a newly added effect in the entity so they survive saving */
  @SubscribeEvent
  static void onEffectAdded(MobEffectEvent.Added event) {
    // when an old instance exists it is updated in place, keeping its own cures
    if (event.getOldEffectInstance() != null) {
      return;
    }
    LivingEntity entity = event.getEntity();
    if (entity.level().isClientSide()) {
      return;
    }
    MobEffectInstance instance = event.getEffectInstance();
    Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
    if (effectId == null) {
      return;
    }
    CompoundTag persistent = entity.getPersistentData();
    Set<Item> cures = CURES.get(instance);
    if (cures == null) {
      // default cures, clear any stale entry from an earlier instance
      persistent.getCompound(PERSISTENT_KEY).ifPresent(tag -> {
        tag.remove(effectId.toString());
        if (tag.isEmpty()) {
          persistent.remove(PERSISTENT_KEY);
        }
      });
    } else {
      ListTag list = new ListTag();
      for (Item item : cures) {
        list.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
      }
      CompoundTag tag = persistent.getCompoundOrEmpty(PERSISTENT_KEY);
      tag.put(effectId.toString(), list);
      persistent.put(PERSISTENT_KEY, tag);
    }
  }

  /** Restores the custom cures of active effects when an entity loads */
  @SubscribeEvent
  static void onEntityJoin(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
      return;
    }
    CompoundTag persistent = living.getPersistentData();
    CompoundTag saved = persistent.getCompound(PERSISTENT_KEY).orElse(null);
    if (saved == null) {
      return;
    }
    CompoundTag pruned = new CompoundTag();
    for (MobEffectInstance instance : living.getActiveEffects()) {
      Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
      if (effectId != null && saved.get(effectId.toString()) instanceof ListTag list) {
        Set<Item> cures = new HashSet<>();
        for (Tag element : list) {
          element.asString().map(Identifier::tryParse).flatMap(BuiltInRegistries.ITEM::getOptional).ifPresent(cures::add);
        }
        CURES.put(instance, Set.copyOf(cures));
        pruned.put(effectId.toString(), list);
      }
    }
    // drop entries for effects that are no longer active
    if (pruned.isEmpty()) {
      persistent.remove(PERSISTENT_KEY);
    } else {
      persistent.put(PERSISTENT_KEY, pruned);
    }
  }
}
