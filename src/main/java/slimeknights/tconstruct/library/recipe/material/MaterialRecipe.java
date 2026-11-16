package slimeknights.tconstruct.library.recipe.material;

import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.List;

/**
 * Recipe to get the material from an ingredient
 */
public class MaterialRecipe implements ICustomOutputRecipe<IMaterialRecipeContainer>, IMaterialValue, IDisplayMaterialRecipe {
  /** Empty material instance for the cache */
  @SuppressWarnings("removal")
  public static final MaterialRecipe EMPTY = new MaterialRecipe(Identifier.parse("missingno"), "", EmptyIngredient.VANILLA, 0, 0, MaterialId.UNKNOWN, ItemOutput.EMPTY);
  public static final RecordLoadable<MaterialRecipe> LOADER = RecordLoadable.create(
    ContextKey.ID.requiredField(),
    LoadableRecipeSerializer.RECIPE_GROUP,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("ingredient", MaterialRecipe::getIngredient),
    IMaterialValue.VALUE_FIELD,
    IMaterialValue.NEEDED_FIELD,
    MaterialVariantId.LOADABLE.requiredField("material", r -> r.getMaterial().getVariant()),
    ItemOutput.Loadable.OPTIONAL_STACK.emptyField("leftover", r -> r.leftover),
    MaterialRecipe::new);

  /** Vanilla requires 4 ingots for full repair, we drop it down to 3 to mesh better with nuggets and blocks and to fit small head costs better */
  public static final float INGOTS_PER_REPAIR = 3f;

  @Getter
  protected final Identifier id;
  @Getter
  protected final String group;
  @Getter
  protected final Ingredient ingredient;
  /** Amount of material this recipe returns */
  @Getter
  protected final int value;
  /** Amount of input items needed to craft this material */
  @Getter
  protected final int needed;
  /** Material ID for the recipe return */
  @Getter
  protected final MaterialVariant material;
  /** Leftover stack of value 1, used if the value is more than 1 */
  protected final ItemOutput leftover;

  /**
   * Creates a new material recipe
   */
  @SuppressWarnings("WeakerAccess")
  public MaterialRecipe(Identifier id, String group, Ingredient ingredient, int value, int needed, MaterialVariantId materialId, ItemOutput leftover) {
    this.id = id;
    this.group = group;
    this.ingredient = ingredient;
    this.value = value;
    this.needed = needed;
    this.material = MaterialVariant.of(materialId);
    // ignore leftover if the value is 1, its useless to us
    this.leftover = value > 1 ? leftover : ItemOutput.EMPTY;

    // save recipe into the cache
    MaterialRecipeCache.registerRecipe(this);
  }

  /* Basic */

  @Override
  public RecipeType<? extends Recipe<IMaterialRecipeContainer>> getType() {
    return TinkerRecipeTypes.MATERIAL.get();
  }

  @Override
  public RecipeSerializer<? extends Recipe<IMaterialRecipeContainer>> getSerializer() {
    return TinkerTables.materialRecipeSerializer.get();
  }

  @Override
  public boolean hasLeftover() {
    return !this.leftover.isEmpty();
  }

  @Override
  public ItemStack getLeftover() {
    // TODO: would be nice to not copy for the recipe display
    return this.leftover.get().copy();
  }

  /* Material methods */

  @Override
  public boolean matches(IMaterialRecipeContainer inv, Level worldIn) {
    return !material.isUnknown() && this.ingredient.test(inv.getStack());
  }

  /** Gets the list of ingredients in this recipe. Was part of {@link Recipe} before 1.21.2. */
  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.of(EmptyIngredient.VANILLA, ingredient);
  }

  /** Cache of the display items list */
  private List<ItemStack> displayItems = null;

  /** Gets a list of stacks for display in the recipe */
  @Override
  public List<ItemStack> getDisplayItems() {
    if (displayItems == null) {
      if (needed > 1) {
        displayItems = IngredientHelper.getStacks(ingredient).stream().map(stack -> stack.copyWithCount(needed)).toList();
      } else {
        displayItems = List.copyOf(IngredientHelper.getStacks(ingredient));
      }
    }
    return displayItems;
  }

  /**
   * Gets the amount to repair per item for tool repair
   * @param amount  Base material amount, typically the head durability stat
   * @return  Float amount per item to repair
   */
  public float scaleRepair(float amount) {
    // not cached as it may vary per stat type
    return this.getValue() * amount / INGOTS_PER_REPAIR / this.getNeeded();
  }

  /** Checks that all materials in this recipe are known */
  @Override
  public boolean isVisible() {
    return !material.isUnknown() && !material.get().isHidden();
  }


  /* JEI */

  @Override
  public Identifier getRecipeId() {
    return getId();
  }
}
