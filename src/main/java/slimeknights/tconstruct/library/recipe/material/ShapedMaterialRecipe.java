package slimeknights.tconstruct.library.recipe.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Shaped recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the material of the result.
 * @deprecated use {@link ShapedMaterialsRecipe}, which requires specifying the ingredients for each part.
 */
@Deprecated
public class ShapedMaterialRecipe extends ShapedRecipe {
  /** Map codec for the recipe, JSON matches vanilla shaped recipes with the added extra materials */
  public static final MapCodec<ShapedMaterialRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
    ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
    ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
    Serializer.EXTRA_MATERIALS.optionalFieldOf(Serializer.MATERIAL_FIELD_KEY, List.of()).forGetter(r -> r.extraMaterials)
  ).apply(inst, ShapedMaterialRecipe::new));
  /** Stream codec for the recipe */
  public static final StreamCodec<RegistryFriendlyByteBuf,ShapedMaterialRecipe> STREAM_CODEC = StreamCodec.composite(
    Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
    CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
    ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
    ItemStackTemplate.STREAM_CODEC, r -> r.result,
    ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS_STREAM, r -> r.extraMaterials,
    ShapedMaterialRecipe::new);

  private MaterialValueIngredient material;
  private final List<MaterialVariantId> extraMaterials;
  /** Copy of the result, as the vanilla field is private */
  private final ItemStackTemplate result;

  public ShapedMaterialRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result, List<MaterialVariantId> extraMaterials) {
    super(commonInfo, bookInfo, pattern, result);
    this.result = result;
    this.extraMaterials = extraMaterials;
  }

  /** Creates a new recipe, adapter for the pre 26.1 parameter list */
  public ShapedMaterialRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, List<MaterialVariantId> extraMaterials) {
    this(new Recipe.CommonInfo(showNotification), new CraftingRecipe.CraftingBookInfo(category, group), pattern, ItemStackTemplate.fromNonEmptyStack(result), extraMaterials);
  }

  public ShapedMaterialRecipe(ShapedRecipe recipe, List<MaterialVariantId> extraMaterials) {
    this(new Recipe.CommonInfo(recipe.showNotification()), new CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()), recipe.pattern, ShapedMaterialsRecipe.resultTemplate(recipe), extraMaterials);
  }

  /** @deprecated use {@link #ShapedMaterialRecipe(ShapedRecipe,List)} */
  @Deprecated(forRemoval = true)
  public ShapedMaterialRecipe(ShapedRecipe recipe) {
    this(recipe, List.of());
  }

  /** Gets the result template of this recipe, as the vanilla field is private */
  public ItemStackTemplate getResult() {
    return result;
  }

  /** Gets the untextured result of this recipe */
  public ItemStack getResultItem() {
    return result.create();
  }

  /** @deprecated use {@link #getResultItem()}, registries are no longer needed */
  @Deprecated
  public ItemStack getResultItem(HolderLookup.Provider registries) {
    return getResultItem();
  }

  /** Gets the material to match */
  @Nullable
  public MaterialValueIngredient getMaterial() {
    if (material == null) {
      // assume all material ingredients match the same stat type
      for (Optional<Ingredient> optional : pattern.ingredients()) {
        // empty slots have no ingredient
        if (optional.isEmpty()) {
          continue;
        }
        // collect all ingredients that match
        ICustomIngredient custom = optional.get().getCustomIngredient();
        if (custom instanceof MaterialValueIngredient materialValue) {
          if (material == null) {
            material = materialValue;
          } else {
            // ensure the stat type matches, and expand the range
            material = material.merge(materialValue);
          }
        }
      }
      // if we found no materials, that is also an issue
      if (material == null) {
        TConstruct.LOG.error("No material ingredient found for material shaped recipe, this indicates a broken recipe");
      }
    }
    return material;
  }

  @Nullable
  private MaterialVariantId findMaterial(CraftingInput inventory) {
    MaterialValueIngredient material = getMaterial();
    if (material == null) {
      return null;
    }
    // ensure same material in all slots
    MaterialVariantId firstMaterial = null;
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        // ignore anything that does not meet our requirements
        MaterialVariantId matchedMaterial = material.getMaterial(stack);
        if (matchedMaterial != null) {
          // first match is set
          if (firstMaterial == null) {
            firstMaterial = matchedMaterial;
          } else if (!firstMaterial.matchesVariant(matchedMaterial)) {
            // if same material but different variants, just discard the variant
            if (firstMaterial.getId().equals(matchedMaterial.getId())) {
              firstMaterial = firstMaterial.getId();
            } else {
              // if different materials, no match
              return null;
            }
          }
        }
      }
    }
    return firstMaterial;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    if (!super.matches(inventory, level)) {
      return false;
    }

    // must have a material to match, no mixing
    return findMaterial(inventory) != null;
  }

  /** Sets the material for the given stack */
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory) {
    ItemStack stack = super.assemble(inventory);
    MaterialVariantId material = findMaterial(inventory);
    if (material != null) {
      setMaterial(stack, material);
    }
    return stack;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})  // vanilla types the serializer as the parent class
  @Override
  public RecipeSerializer<ShapedRecipe> getSerializer() {
    return (RecipeSerializer) TinkerTables.shapedMaterialRecipeSerializer.get();
  }

  /** Serializer logic, register {@link #toSerializer()} */
  public static class Serializer implements LoggingRecipeSerializer<ShapedMaterialRecipe> {
    /** Codec for the list of extra materials */
    static final Codec<List<MaterialVariantId>> EXTRA_MATERIALS = ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS;
    /** Field key for the extra materials list */
    static final String MATERIAL_FIELD_KEY = ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS_KEY;

    @Override
    public MapCodec<ShapedMaterialRecipe> codec() {
      return MAP_CODEC;
    }

    @Override
    public ShapedMaterialRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      return STREAM_CODEC.decode(buffer);
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapedMaterialRecipe recipe) {
      STREAM_CODEC.encode(buffer, recipe);
    }
  }
}
