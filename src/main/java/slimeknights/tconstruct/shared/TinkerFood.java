package slimeknights.tconstruct.shared;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.block.FoliageType;

/**
 * Food definitions for Tinkers' food items and cakes.
 * Since 1.21.2 {@link FoodProperties} only holds nutrition, saturation and always edible. The effects, eat speed and animation live on the
 * {@link Consumable} component, so each food with effects has a matching {@code _USE} consumable, passed alongside it to
 * {@link net.minecraft.world.item.Item.Properties#food(FoodProperties, Consumable)}.
 */
@SuppressWarnings("WeakerAccess")
public final class TinkerFood {
  private TinkerFood() {}

  /**
   * Gets a registered TConstruct effect as a holder for use in {@link MobEffectInstance}.
   * Deferred holders are holder references themselves and compare equal to the registry holder, so they can be used before the registry is bound.
   */
  @SuppressWarnings("unchecked")
  private static Holder<MobEffect> holder(DeferredHolder<?, ?> effect) {
    return (Holder<MobEffect>) (Holder<?>) effect;
  }

  /** Creates a consume effect applying the given effect with the given chance */
  private static ApplyStatusEffectsConsumeEffect effect(MobEffectInstance effect, float probability) {
    return new ApplyStatusEffectsConsumeEffect(effect, probability);
  }

  /** Bacon. What more is there to say? */
  public static final FoodProperties BACON = (new FoodProperties.Builder()).nutrition(4).saturationModifier(0.6F).build();

  /** Cheese is used for both the block and the ingot, eating the block returns 3 ingots */
  public static final FoodProperties CHEESE = (new FoodProperties.Builder()).nutrition(3).saturationModifier(0.4F).build();

  /** For the modifier */
  public static final FoodProperties JEWELED_APPLE = (new FoodProperties.Builder()).nutrition(4).saturationModifier(1.2F).alwaysEdible().build();
  /** Effects from eating {@link #JEWELED_APPLE} */
  public static final Consumable JEWELED_APPLE_USE = Consumables.defaultFood()
    .onConsume(effect(new MobEffectInstance(MobEffects.HASTE, 1200, 0), 1.0F))
    .onConsume(effect(new MobEffectInstance(MobEffects.RESISTANCE, 2400, 0), 1.0F))
    .build();

  /* Cake block is set up to take food as a parameter */
  public static final FoodProperties EARTH_CAKE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).alwaysEdible().build();
  public static final FoodProperties SKY_CAKE   = new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).alwaysEdible().build();
  public static final FoodProperties ICHOR_CAKE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).alwaysEdible().build();
  public static final FoodProperties ENDER_CAKE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.4f).alwaysEdible().build();
  public static final FoodProperties MAGMA_CAKE = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build();
  // regen is 50 ticks per half heart, so this heals 3 per slice
  public static final FoodProperties BLOOD_CAKE = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build();
  // effects of a slice of cake
  public static final Consumable EARTH_CAKE_USE = Consumables.defaultFood().onConsume(effect(new MobEffectInstance(holder(TinkerEffects.bouncy),      30 * 20, 0), 1.0f)).build();
  public static final Consumable SKY_CAKE_USE   = Consumables.defaultFood().onConsume(effect(new MobEffectInstance(holder(TinkerEffects.doubleJump),  30 * 20, 0), 1.0f)).build();
  public static final Consumable ICHOR_CAKE_USE = Consumables.defaultFood().onConsume(effect(new MobEffectInstance(holder(TinkerEffects.antigravity), 30 * 20, 0), 1.0f)).build();
  // ender cake is fast to eat, which was the 1.21.1 fast() food property (0.8 seconds)
  public static final Consumable ENDER_CAKE_USE = Consumables.defaultFood().consumeSeconds(0.8f).onConsume(effect(new MobEffectInstance(holder(TinkerEffects.returning), 30 * 20, 0), 1.0f)).build();
  public static final Consumable MAGMA_CAKE_USE = Consumables.defaultFood().onConsume(effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,      30 * 20, 0), 1.0f)).build();
  public static final Consumable BLOOD_CAKE_USE = Consumables.defaultFood().onConsume(effect(new MobEffectInstance(MobEffects.REGENERATION, 3 * 50, 0), 1.0f)).build();

  /* Bottles are drinks, so they use the drink animation and sound */
  public static final FoodProperties EARTH_BOTTLE = new FoodProperties.Builder().alwaysEdible().build();
  public static final FoodProperties SKY_BOTTLE   = new FoodProperties.Builder().alwaysEdible().build();
  public static final FoodProperties ICHOR_BOTTLE = new FoodProperties.Builder().alwaysEdible().build();
  public static final FoodProperties ENDER_BOTTLE = new FoodProperties.Builder().alwaysEdible().build();
  public static final FoodProperties VENOM_BOTTLE = new FoodProperties.Builder().alwaysEdible().build();
  public static final Consumable EARTH_BOTTLE_USE = Consumables.defaultDrink()
    .onConsume(effect(new MobEffectInstance(holder(TinkerEffects.experienced),  120 * 20), 1.0f))
    .onConsume(effect(new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 1), 1.0f)).build();
  public static final Consumable SKY_BOTTLE_USE = Consumables.defaultDrink()
    .onConsume(effect(new MobEffectInstance(holder(TinkerEffects.ricochet),     120 * 20), 1.0f))
    .onConsume(effect(new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 1), 1.0f)).build();
  public static final Consumable ICHOR_BOTTLE_USE = Consumables.defaultDrink()
    .onConsume(effect(new MobEffectInstance(MobEffects.LEVITATION,             10 * 20), 1.0f))
    .onConsume(effect(new MobEffectInstance(MobEffects.SLOWNESS,  10 * 20, 1), 1.0f)).build();
  public static final Consumable ENDER_BOTTLE_USE = Consumables.defaultDrink()
    .onConsume(effect(new MobEffectInstance(holder(TinkerEffects.enderference),  60 * 20), 1.0f))
    .onConsume(effect(new MobEffectInstance(MobEffects.SLOWNESS,  60 * 20, 1), 1.0f)).build();
  // 250 is 10 poison damage
  public static final Consumable VENOM_BOTTLE_USE = Consumables.defaultDrink()
    .onConsume(effect(new MobEffectInstance(MobEffects.STRENGTH, 30 * 20), 1.0f))
    .onConsume(effect(new MobEffectInstance(MobEffects.POISON, 250), 1.0f)).build();
  /** @deprecated no longer used */
  @Deprecated(forRemoval = true)
  public static final FoodProperties MAGMA_BOTTLE = new FoodProperties.Builder().alwaysEdible().build();

  public static final FoodProperties MEAT_SOUP = new FoodProperties.Builder().nutrition(8).saturationModifier(0.6f).build();
  /** Consumable for {@link #MEAT_SOUP}, eaten like the vanilla stews */
  public static final Consumable MEAT_SOUP_USE = Consumables.defaultFood().build();

  /**
   * Gets the cake for the given slime type
   * @param slime  Slime type
   * @return  Cake food
   */
  public static FoodProperties getCake(FoliageType slime) {
    return switch (slime) {
      default -> EARTH_CAKE;
      case SKY -> SKY_CAKE;
      case ICHOR -> ICHOR_CAKE;
      case BLOOD -> BLOOD_CAKE;
      case ENDER -> ENDER_CAKE;
    };
  }

  /**
   * Gets the effects of a cake slice for the given slime type
   * @param slime  Slime type
   * @return  Cake consumable
   */
  public static Consumable getCakeUse(FoliageType slime) {
    return switch (slime) {
      default -> EARTH_CAKE_USE;
      case SKY -> SKY_CAKE_USE;
      case ICHOR -> ICHOR_CAKE_USE;
      case BLOOD -> BLOOD_CAKE_USE;
      case ENDER -> ENDER_CAKE_USE;
    };
  }

  /**
   * Gets the cake for the given slime type
   * @param slime  Slime type
   * @return  Cake food
   */
  public static FoodProperties getBottle(SlimeType slime) {
    return switch (slime) {
      default -> EARTH_BOTTLE;
      case SKY -> SKY_BOTTLE;
      case ICHOR -> ICHOR_BOTTLE;
      case ENDER -> ENDER_BOTTLE;
    };
  }

  /**
   * Gets the effects of drinking the bottle for the given slime type
   * @param slime  Slime type
   * @return  Bottle consumable
   */
  public static Consumable getBottleUse(SlimeType slime) {
    return switch (slime) {
      default -> EARTH_BOTTLE_USE;
      case SKY -> SKY_BOTTLE_USE;
      case ICHOR -> ICHOR_BOTTLE_USE;
      case ENDER -> ENDER_BOTTLE_USE;
    };
  }
}
