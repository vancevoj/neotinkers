package slimeknights.tconstruct.gadgets.block;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.mantle.item.ContainerFoodItem;

import java.util.List;
import java.util.function.Consumer;

/**
 * Extension of cake that utilizes a food instance for properties.
 * Food effects moved from the food properties to the consumable component, so the cake takes both.
 */
public class FoodCakeBlock extends CakeBlock {
  private final FoodProperties food;
  /** Consumable holding the effects of a slice, only the status effects are used */
  @Getter
  private final Consumable consumable;
  /** Status effects applied when eating a slice */
  private final List<ApplyStatusEffectsConsumeEffect> effects;
  private final EffectCombination combination;

  public FoodCakeBlock(Properties properties, FoodProperties food, Consumable consumable, EffectCombination combination) {
    super(properties);
    this.food = food;
    this.consumable = consumable;
    this.effects = consumable.onConsumeEffects().stream()
      .filter(effect -> effect instanceof ApplyStatusEffectsConsumeEffect)
      .map(effect -> (ApplyStatusEffectsConsumeEffect) effect)
      .toList();
    this.combination = combination;
  }

  @Deprecated(forRemoval = true)
  public FoodCakeBlock(Properties properties, FoodProperties food, Consumable consumable) {
    this(properties, food, consumable, EffectCombination.BLOCK);
  }

  /** Adds the effects of a slice to the tooltip. Blocks no longer have tooltips, so this is called by the cake item */
  public void appendHoverText(Consumer<Component> tooltip) {
    ContainerFoodItem.addEffectTooltip(consumable, tooltip);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    InteractionResult result = this.eatSlice(world, pos, state, player);
    if (result.consumesAction()) {
      return result;
    }
    if (world.isClientSide() && player.getMainHandItem().isEmpty()) {
      return InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

  /** Checks if the given player has all potion effects from the food */
  private boolean hasAllEffects(Player player) {
    for (ApplyStatusEffectsConsumeEffect group : effects) {
      for (MobEffectInstance effect : group.effects()) {
        MobEffectInstance current = player.getEffect(effect.getEffect());
        if (current == null || current.getDuration() < 100) {
          return false;
        }
      }
    }
    return true;
  }

  /** Eats a single slice of cake if possible */
  private InteractionResult eatSlice(LevelAccessor world, BlockPos pos, BlockState state, Player player) {
    if (!player.canEat(false) && !food.canAlwaysEat()) {
      return InteractionResult.PASS;
    }
    // repurpose fast eating, will mean no eating if we have the effect
    if (combination == EffectCombination.BLOCK && hasAllEffects(player)) {
      return InteractionResult.PASS;
    }
    player.awardStat(Stats.EAT_CAKE_SLICE);
    // apply food stats
    player.getFoodData().eat(food);
    if (!world.isClientSide()) {
      for (ApplyStatusEffectsConsumeEffect group : effects) {
        if (world.getRandom().nextFloat() < group.probability()) {
          for (MobEffectInstance groupEffect : group.effects()) {
            MobEffectInstance effect = new MobEffectInstance(groupEffect);
            // if adding, increase duration by current duration, provided its an exact level match
            if (combination == EffectCombination.ADD) {
              MobEffectInstance current = player.getEffect(effect.getEffect());
              if (current != null && current.getAmplifier() == effect.getAmplifier()) {
                effect.duration += current.getDuration();
              }
            }
            player.addEffect(effect);
          }
        }
      }
    }
    // remove one bite from the cake
    int i = state.getValue(BITES);
    if (i < 6) {
      world.setBlock(pos, state.setValue(BITES, i + 1), 3);
    } else {
      world.removeBlock(pos, false);
    }
    return InteractionResult.SUCCESS;
  }

  public enum EffectCombination {
    /** New effect will update time on existing, like potions */
    SET,
    /** New effect will increase duration of existing */
    ADD,
    /** Cake cannot be eaten if effect is present  */
    BLOCK
  }
}
