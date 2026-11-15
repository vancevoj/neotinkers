package slimeknights.tconstruct.library.recipe.casting;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;

import javax.annotation.Nonnull;

/** Shared logic between item and material casting */
public abstract class AbstractCastingRecipe implements ICastingRecipe {
  /* Common fields */
  /** 1.21.2: vanilla has no empty ingredient, a missing cast is {@link EmptyIngredient#VANILLA}, which only matches an empty slot */
  protected static final LoadableField<Ingredient,AbstractCastingRecipe> CAST_FIELD = IngredientLoadable.ALLOW_EMPTY.defaultField("cast", EmptyIngredient.VANILLA, AbstractCastingRecipe::getCast);
  protected static final LoadableField<Boolean,AbstractCastingRecipe> CAST_CONSUMED_FIELD = BooleanLoadable.INSTANCE.defaultField("cast_consumed", false, false, AbstractCastingRecipe::isConsumed);
  protected static final LoadableField<Boolean,AbstractCastingRecipe> SWITCH_SLOTS_FIELD = BooleanLoadable.INSTANCE.defaultField("switch_slots", false, false, AbstractCastingRecipe::switchSlots);

  @Getter @Nonnull
  private final RecipeType<? extends Recipe<ICastingContainer>> type;
  /**
   * Recipe ID. In 1.21 vanilla recipes are keyed by their {@link net.minecraft.world.item.crafting.RecipeHolder},
   * but casting recipes still carry their own ID for JEI display purposes.
   */
  @Getter
  private final Identifier id;
  @Getter
  private final String group;
  /** 'cast' item for recipe (doesn't have to be an actual 'cast') */
  @Getter
  private final Ingredient cast;
  @Getter
  private final boolean consumed;
  @Getter @Accessors(fluent = true)
  private final boolean switchSlots;

  @SuppressWarnings("unchecked")
  protected AbstractCastingRecipe(RecipeType<?> type, Identifier id, String group, Ingredient cast, boolean consumed, boolean switchSlots) {
    // types come from the type aware serializer, which cannot express the recipe input bound vanilla requires since 1.21.2
    this.type = (RecipeType<? extends Recipe<ICastingContainer>>)type;
    this.id = id;
    this.group = group;
    this.cast = cast;
    this.consumed = consumed;
    this.switchSlots = switchSlots;
  }

  /** Checks if this recipe has a cast, false if the cast is the empty ingredient */
  protected boolean hasCastIngredient() {
    return !IngredientLoadable.isEmpty(this.cast);
  }
}
