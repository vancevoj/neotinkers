package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ingredient matching material items with the given value. Typically, matches ingots or blocks
 */
public class MaterialValueIngredient implements ICustomIngredient {
  /** Field for the material predicate */
  private static final LoadableField<IJsonPredicate<MaterialVariantId>,MaterialValueIngredient> MATERIAL_FIELD = new MaterialPredicateField<>("material", i -> i.material);
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<MaterialValueIngredient> LOADABLE = RecordLoadable.create(
    MATERIAL_FIELD,
    ValueField.INSTANCE,
    (material, value) -> new MaterialValueIngredient(material, value[0], value[1]));
  /** Ingredient type instance, registered as {@code tconstruct:material_value} in {@link slimeknights.tconstruct.tools.TinkerTools} */
  public static final IngredientType<MaterialValueIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  private final IJsonPredicate<MaterialVariantId> material;
  private final float minValue;
  private final float maxValue;
  /** Ingredients of all matching material recipes, in material order */
  @Nullable
  private List<Ingredient> ingredients;

  public MaterialValueIngredient(IJsonPredicate<MaterialVariantId> material, float minValue, float maxValue) {
    this.material = material;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  /** Creates an ingredient matching a range of values */
  public static MaterialValueIngredient of(IJsonPredicate<MaterialVariantId> materials, float minValue, float maxValue) {
    return new MaterialValueIngredient(materials, minValue, maxValue);
  }

  /** Creates an ingredient matching an exact value */
  public static MaterialValueIngredient of(IJsonPredicate<MaterialVariantId> materials, float value) {
    return of(materials, value, value);
  }

  /** Gets the material predicate for this ingredient */
  public IJsonPredicate<MaterialVariantId> getMaterial() {
    return material;
  }

  /** Gets the minimum value matched by this ingredient */
  public float getMinValue() {
    return minValue;
  }

  /** Gets the maximum value matched by this ingredient */
  public float getMaxValue() {
    return maxValue;
  }

  /** Checks the given material recipe against our filters */
  public boolean test(MaterialRecipe material) {
    float value = material.getValue() / (float) material.getNeeded();
    return minValue <= value && value <= maxValue && this.material.matches(material.getMaterial().getVariant());
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null) {
      return false;
    }
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe);
  }

  /** Gets the ingredients of all matching material recipes */
  private List<Ingredient> getMatchingIngredients() {
    if (ingredients == null) {
      ingredients = MaterialRecipeCache.getSortedRecipes().stream()
        .filter(this::test)
        .map(MaterialRecipe::getIngredient)
        .toList();
    }
    return ingredients;
  }

  @SuppressWarnings("deprecation")
  @Override
  public Stream<Holder<Item>> items() {
    return getMatchingIngredients().stream().flatMap(Ingredient::items);
  }

  /** Displays the matching material recipe ingredients, keeping any components their display stacks have */
  @Override
  public SlotDisplay display() {
    return new SlotDisplay.Composite(getMatchingIngredients().stream().map(Ingredient::display).toList());
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof MaterialValueIngredient ingredient && this.minValue == ingredient.minValue && this.maxValue == ingredient.maxValue && this.material.equals(ingredient.material);
  }

  @Override
  public int hashCode() {
    return Objects.hash(MaterialValueIngredient.class, material, minValue, maxValue);
  }


  /* Helpers for ShapedMaterialRecipe */

  /** Checks if this ingredient fully contains the range of the other */
  private boolean contains(MaterialValueIngredient other) {
    return this.minValue <= other.minValue && other.maxValue <= this.maxValue;
  }

  /** Creates an ingredient that matches anything either of the two ingredients matches */
  public MaterialValueIngredient merge(MaterialValueIngredient other) {
    if (this == other) return this;

    // if we have the same predicate, we can possibly skip creating a new instance
    IJsonPredicate<MaterialVariantId> predicate = this.material;
    if (this.material.equals(other.material)) {
      if (this.contains(other)) {
        return this;
      }
      if (other.contains(this)) {
        return other;
      }
    } else {
      predicate = MaterialPredicate.or(this.material, other.material);
    }
    return new MaterialValueIngredient(predicate, Math.min(this.minValue, other.minValue), Math.max(this.maxValue, other.maxValue));
  }

  /** Gets the material matching this recipe */
  @Nullable
  public MaterialVariantId getMaterial(ItemStack stack) {
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe) ? recipe.getMaterial().getVariant() : null;
  }


  /* JSON */

  /** Field handling the compact or ranged {@code value} key, yielding a {@code [min, max]} pair */
  private enum ValueField implements RecordField<float[],MaterialValueIngredient> {
    INSTANCE;

    @Override
    public float[] get(JsonObject json, TypedMap context) {
      float minValue, maxValue;
      JsonElement value = json.get("value");
      if (value != null && value.isJsonPrimitive()) {
        minValue = maxValue = value.getAsJsonPrimitive().getAsFloat();
      } else {
        JsonObject object = GsonHelper.convertToJsonObject(value, "value");
        minValue = GsonHelper.getAsFloat(object, "min", 0);
        maxValue = GsonHelper.getAsFloat(object, "max", Float.POSITIVE_INFINITY);
      }
      return new float[] { minValue, maxValue };
    }

    @Override
    public void serialize(MaterialValueIngredient parent, JsonObject json) {
      if (parent.minValue == parent.maxValue) {
        json.addProperty("value", parent.minValue);
      } else {
        JsonObject value = new JsonObject();
        if (parent.minValue > 0) {
          value.addProperty("min", parent.minValue);
        }
        if (Float.isFinite(parent.maxValue)) {
          value.addProperty("max", parent.maxValue);
        }
        json.add("value", value);
      }
    }

    @Override
    public float[] decode(FriendlyByteBuf buffer, TypedMap context) {
      return new float[] { buffer.readFloat(), buffer.readFloat() };
    }

    @Override
    public void encode(FriendlyByteBuf buffer, MaterialValueIngredient parent) {
      buffer.writeFloat(parent.minValue);
      buffer.writeFloat(parent.maxValue);
    }
  }
}
