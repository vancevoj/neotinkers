package slimeknights.tconstruct.library.recipe.casting;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.common.recipe.LoadableFinishedRecipe;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;

/**
 * Builder for a potion bottle filling recipe. Takes a fluid and optional cast to create an item that copies the fluid NBT
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PotionCastingRecipeBuilder extends AbstractRecipeBuilder<PotionCastingRecipeBuilder> {
  private final Item result;
  @Nullable
  private final ModifierId modifier;
  /** Registered serializer, must be type aware. Since 26.1 serializers are records, see {@link LoadableFinishedRecipe#typeAware(RecipeSerializer)} */
  private final RecipeSerializer<? extends PotionCastingRecipe> recipeSerializer;
  /** If true, the modifier recipe is a tip-clearing recipe (TipClearingCastingRecipe) rather than a tipping recipe */
  private final boolean tipClearing;
  private Ingredient bottle = EmptyIngredient.VANILLA;
  private FluidIngredient fluid = FluidIngredient.EMPTY;
  @Setter @Accessors(chain = true)
  private int coolingTime = 5;

  /* Bottle filling */

  /** Creates a new casting recipe for a bottle */
  public static PotionCastingRecipeBuilder castingRecipe(ItemLike result, RecipeSerializer<PotionCastingRecipe> serializer) {
    return new PotionCastingRecipeBuilder(result.asItem(), null, serializer, false);
  }

  /**
   * Creates a new casting basin recipe
   * @param result  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder basinRecipe(ItemLike result) {
    return castingRecipe(result, TinkerSmeltery.basinPotionRecipeSerializer.get());
  }

  /**
   * Creates a new casting table recipe
   * @param result  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder tableRecipe(ItemLike result) {
    return castingRecipe(result, TinkerSmeltery.tablePotionRecipeSerializer.get());
  }


  /* Modifier casting */

  /** Creates a new casting recipe for a bottle */
  public static PotionCastingRecipeBuilder tippingRecipe(ModifierId modifier, RecipeSerializer<? extends PotionCastingRecipe> serializer, boolean tipClearing) {
    return new PotionCastingRecipeBuilder(Items.AIR, modifier, serializer, tipClearing);
  }

  /**
   * Creates a new tool potion casting basin recipe
   * @param modifier  Modifier required to cast
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder basinTipping(ModifierId modifier) {
    return tippingRecipe(modifier, TinkerSmeltery.basinTippingRecipeSerializer.get(), false);
  }

  /**
   * Creates a new tool potion casting table recipe
   * @param modifier  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder tableTipping(ModifierId modifier) {
    return tippingRecipe(modifier, TinkerSmeltery.tableTippingRecipeSerializer.get(), false);
  }

  /**
   * Creates a new tool potion casting basin recipe
   * @param modifier  Modifier required to cast
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder basinClearing(ModifierId modifier) {
    return tippingRecipe(modifier, TinkerSmeltery.basinTipClearingRecipeSerializer.get(), true);
  }

  /**
   * Creates a new tool potion casting table recipe
   * @param modifier  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder tableClearing(ModifierId modifier) {
    return tippingRecipe(modifier, TinkerSmeltery.tableTipClearingRecipeSerializer.get(), true);
  }


  /* Fluids */

  /**
   * Sets the fluid for this recipe
   * @param tagIn   Tag<Fluid> instance
   * @param amount  amount of fluid
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setFluid(TagKey<Fluid> tagIn, int amount) {
    return this.setFluid(FluidIngredient.of(tagIn, amount));
  }

  /**
   * Sets the fluid ingredient
   * @param fluid  Fluid ingredient instance
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setFluid(FluidIngredient fluid) {
    this.fluid = fluid;
    return this;
  }


  /* Cast */

  /**
   * Sets the cast from a tag, bottles are always consumed
   * @param tagIn     Cast tag
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setBottle(TagKey<Item> tagIn) {
    return this.setBottle(IngredientHelper.tag(tagIn));
  }

  /**
   * Sets the bottle from an item, bottles are always consumed
   * @param itemIn    Cast item
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setBottle(ItemLike itemIn) {
    return this.setBottle(Ingredient.of(itemIn));
  }

  /**
   * Sets the bottle from an ingredient, bottles are always consumed
   * @param ingredient  Cast ingredient
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setBottle(Ingredient ingredient) {
    this.bottle = ingredient;
    return this;
  }

  /**
   * Builds a recipe using the registry name as the recipe name
   * @param output  Recipe output
   */
  @Override
  public void save(RecipeOutput output) {
    this.save(output, Loadables.ITEM.getKey(this.result));
  }

  @Override
  public void save(RecipeOutput output, Identifier id) {
    if (this.fluid == FluidIngredient.EMPTY) {
      throw new IllegalStateException("Casting recipes require a fluid input");
    }
    if (this.coolingTime < 0) {
      throw new IllegalStateException("Cooling time is too low, must be at least 0");
    }
    AdvancementHolder advancement = this.buildOptionalAdvancement(output, id, "casting");
    TypeAwareRecipeSerializer<? extends PotionCastingRecipe> serializer = LoadableFinishedRecipe.typeAware(recipeSerializer);
    if (modifier != null) {
      PotionCastingRecipe recipe = tipClearing
        ? new TipClearingCastingRecipe(serializer, id, group, bottle, fluid, coolingTime, modifier)
        : new TippingCastingRecipe(serializer, id, group, bottle, fluid, coolingTime, modifier);
      output.accept(recipeKey(id), recipe, advancement);
    } else {
      output.accept(recipeKey(id), new PotionCastingRecipe(serializer, id, group, bottle, fluid, result, coolingTime), advancement);
    }
  }
}
