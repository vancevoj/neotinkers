package slimeknights.tconstruct.library.recipe.molding;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICommonRecipe;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;

/** Recipe to combine two items on the top of a casting table, changing the first */
public class MoldingRecipe implements ICommonRecipe<IMoldingContainer> {
  public static final RecordLoadable<MoldingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(),
    ContextKey.ID.requiredField(),
    IngredientLoadable.DISALLOW_EMPTY.requiredField("material", MoldingRecipe::getMaterial),
    IngredientLoadable.ALLOW_EMPTY.defaultField("pattern", EmptyIngredient.VANILLA, MoldingRecipe::getPattern),
    BooleanLoadable.INSTANCE.defaultField("pattern_consumed", false, false, MoldingRecipe::isPatternConsumed),
    ItemOutput.Loadable.REQUIRED_ITEM.requiredField("result", r -> r.recipeOutput),
    MoldingRecipe::new);

  @Getter
  private final RecipeType<? extends Recipe<IMoldingContainer>> type;
  /** Registered serializer from the type aware serializer, since 26.1 the type aware serializer is no longer a serializer */
  @Getter
  private final RecipeSerializer<? extends Recipe<IMoldingContainer>> serializer;
  @Getter
  private final Identifier id;
  @Getter
  private final Ingredient material;
  @Getter
  private final Ingredient pattern;
  @Getter
  private final boolean patternConsumed;
  private final ItemOutput recipeOutput;

  @SuppressWarnings("unchecked")
  public MoldingRecipe(TypeAwareRecipeSerializer<?> serializer, Identifier id, Ingredient material, Ingredient pattern, boolean patternConsumed, ItemOutput recipeOutput) {
    // the type aware serializer cannot express the recipe input bound vanilla requires since 1.21.2
    this.type = (RecipeType<? extends Recipe<IMoldingContainer>>)serializer.getType();
    this.serializer = (RecipeSerializer<? extends Recipe<IMoldingContainer>>)(RecipeSerializer<?>)serializer.getSerializer();
    this.id = id;
    this.material = material;
    this.pattern = pattern;
    this.patternConsumed = !IngredientLoadable.isEmpty(pattern) && patternConsumed;
    this.recipeOutput = recipeOutput;
  }

  @Override
  public boolean matches(IMoldingContainer inv, Level worldIn) {
    return material.test(inv.getMaterial()) && pattern.test(inv.getPattern());
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return recipeOutput.get();
  }
}
