package slimeknights.tconstruct.library.recipe.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.List;

/**
 * Shapeless recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapelessMaterialsRecipe extends ShapelessRecipe implements MaterialsCraftingTableRecipe {
  /** Map codec for the recipe, JSON matches vanilla shapeless recipes with the added parts and extra materials */
  public static final MapCodec<ShapelessMaterialsRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
    ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
    Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients),
    Codec.INT.fieldOf("parts").forGetter(r -> r.partCount),
    ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS.optionalFieldOf(ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS_KEY, List.of()).forGetter(r -> r.extraMaterials)
  ).apply(inst, ShapelessMaterialsRecipe::new));
  /** Stream codec for the recipe */
  public static final StreamCodec<RegistryFriendlyByteBuf,ShapelessMaterialsRecipe> STREAM_CODEC = StreamCodec.composite(
    Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
    CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
    ItemStackTemplate.STREAM_CODEC, r -> r.result,
    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
    ByteBufCodecs.VAR_INT, r -> r.partCount,
    ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS_STREAM, r -> r.extraMaterials,
    ShapelessMaterialsRecipe::new);

  /** Number of parts to match */
  @Getter
  private final int partCount;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;
  /** Copy of the result, as the vanilla field is private */
  private final ItemStackTemplate result;
  /** Copy of the ingredients, as the vanilla field is private */
  private final List<Ingredient> ingredients;

  public ShapelessMaterialsRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result, List<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials) {
    super(commonInfo, bookInfo, result, ingredients);
    this.result = result;
    this.ingredients = ingredients;
    this.partCount = partCount;
    this.extraMaterials = extraMaterials;
  }

  /** Creates a new recipe, adapter for the pre 26.1 parameter list */
  public ShapelessMaterialsRecipe(String group, CraftingBookCategory category, ItemStack result, List<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials) {
    this(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(category, group), ItemStackTemplate.fromNonEmptyStack(result), ingredients, partCount, extraMaterials);
  }

  /**
   * Wraps a vanilla shapeless recipe. Used by {@link MaterialsConsumerBuilder} during datagen.
   * @param recipe          Vanilla shapeless recipe
   * @param partCount       Number of leading ingredients that count as parts
   * @param extraMaterials  Extra materials to add beyond the parts
   */
  public ShapelessMaterialsRecipe(ShapelessRecipe recipe, int partCount, List<MaterialVariantId> extraMaterials) {
    this(new Recipe.CommonInfo(recipe.showNotification()), new CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()), resultTemplate(recipe), ingredients(recipe), partCount, extraMaterials);
  }

  /** Gets the result template of a shapeless recipe */
  private static ItemStackTemplate resultTemplate(ShapelessRecipe recipe) {
    ItemStackTemplate result = recipe.result();
    if (result == null) {
      // NeoForge allows subclasses without a static result, those cannot be wrapped without creating stacks (unsafe during datagen)
      throw new IllegalArgumentException("Shapeless recipe " + recipe + " has no static result to wrap");
    }
    return result;
  }

  /** Gets the ingredients of a shapeless recipe, as the vanilla field is private */
  private static List<Ingredient> ingredients(ShapelessRecipe recipe) {
    if (recipe instanceof ShapelessMaterialsRecipe materials) {
      return materials.ingredients;
    }
    return recipe.ingredients;
  }

  /** Gets the list of ingredients in this recipe. Was part of {@link Recipe} before 1.21.2. */
  public List<Ingredient> getIngredients() {
    return ingredients;
  }

  @Override
  public ItemStack getResultItem() {
    return result.create();
  }

  @Override
  public List<Ingredient> getParts() {
    return ingredients;
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory) {
    return ShapedMaterialsRecipe.assemble(super.assemble(inventory), inventory, ingredients, partCount, false, extraMaterials);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})  // vanilla types the serializer as the parent class
  @Override
  public RecipeSerializer<ShapelessRecipe> getSerializer() {
    return (RecipeSerializer) TinkerTables.shapelessMaterialsRecipeSerializer.get();
  }

  /** Serializer logic, register {@link #toSerializer()} */
  public static class Serializer implements LoggingRecipeSerializer<ShapelessMaterialsRecipe> {
    @Override
    public MapCodec<ShapelessMaterialsRecipe> codec() {
      return MAP_CODEC;
    }

    @Override
    public ShapelessMaterialsRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      return STREAM_CODEC.decode(buffer);
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapelessMaterialsRecipe recipe) {
      STREAM_CODEC.encode(buffer, recipe);
    }
  }
}
