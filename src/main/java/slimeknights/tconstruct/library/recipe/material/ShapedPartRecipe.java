package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shaped recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} to set the materials of the result.
 */
public class ShapedPartRecipe extends ShapedRecipe {
  /** List of tool parts to search for in the final recipe */
  @Getter
  private final List<Ingredient> parts;
  /**
   * If true, a part may show up multiple times in the inputs, and all copies should match.
   * If false, only the first instance of a part is checked for each input, allowing a tool with the same part multiple times.
   */
  private final boolean checkRepeats;

  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;
  public ShapedPartRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    super(id, group, category, width, height, ingredients, result, showNotification);
    this.parts = parts;
    this.checkRepeats = parts.stream().distinct().count() == parts.size();
    this.extraMaterials = extraMaterials;
  }

  public ShapedPartRecipe(ShapedRecipe recipe, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    this(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(), recipe.result, recipe.showNotification(), parts, extraMaterials);
  }

  /**
   * Finds materials for each of the parts
   * @return Array of all matched materials. Array will have no null entries, though the array may be null if no match was found.
   */
  @Nullable
  private MaterialVariantId[] findMaterials(CraftingContainer inventory) {
    // want one material for each
    MaterialVariantId[] materials = new MaterialVariantId[parts.size()];
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        for (int p = 0; p < parts.size(); p++) {
          MaterialVariantId current = materials[p];
          // if we have not found the material yet, or repeats are considered the same material, test the ingredient
          if ((current == null || checkRepeats) && parts.get(p).test(stack)) {
            MaterialVariantId matched = IMaterialItem.getMaterialFromStack(stack);
            // first occurrence? thats our material
            if (current == null) {
              materials[p] = matched;
              break;
            } else if (!current.matchesVariant(matched)) {
              // if same material but different variants, just discard the variant
              if (current.getId().equals(matched.getId())) {
                materials[p] = current.getId();
                break;
              } else {
                // if different materials, no match
                return null;
              }
            }
          }
        }
      }
    }
    // ensure we found all materials needed
    for (int p = 0; p < parts.size(); p++) {
      if (materials[p] == null) {
        return null;
      }
    }
    return materials;
  }

  @Override
  public boolean matches(CraftingContainer inventory, Level level) {
    if (!super.matches(inventory, level)) {
      return false;
    }
    // ensure all part materials matched and we found all parts
    return findMaterials(inventory) != null;
  }

  /** Sets the material for the given stack */
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
    ItemStack stack = super.assemble(inventory, registryAccess);
    MaterialVariantId[] materials = findMaterials(inventory);
    if (materials != null) {
      // if the result is a tool part, and we only have the one material, set its material
      if (materials.length == 1 && extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
        return materialItem.setMaterial(stack, materials[0]);
      }
      MaterialNBT.Builder builder = MaterialNBT.builder();
      // add each material
      for (MaterialVariantId material : materials) {
        builder.add(material);
      }
      // add extra materials
      builder.add(extraMaterials);
      ToolStack.from(stack).setMaterials(builder.build());
    }
    return stack;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapedPartRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapedPartRecipe> {
    static final LoadableField<List<MaterialVariantId>,ShapedPartRecipe> MATERIAL_FIELD = ShapedMaterialRecipe.Serializer.EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);

    @Override
    public ShapedPartRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
      // from ShapedRecipe, copied as we want to get keys without creating multiple ingredient instances
      String group = GsonHelper.getAsString(json, "group", "");
      CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
      Map<String, Ingredient> key = ShapedRecipe.keyFromJson(GsonHelper.getAsJsonObject(json, "key"));
      String[] pattern = ShapedRecipe.shrink(ShapedRecipe.patternFromJson(GsonHelper.getAsJsonArray(json, "pattern")));
      int width = pattern[0].length();
      int height = pattern.length;
      NonNullList<Ingredient> inputs = ShapedRecipe.dissolvePattern(pattern, key, width, height);
      ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
      boolean showNotification = GsonHelper.getAsBoolean(json, "show_notification", true);

      // specific to shaped part recipe, map from a pattern string to the ingredients for each character
      // saves memory by not having separate copies of each, plus simplifies the JSON
      String partPattern = GsonHelper.getAsString(json, "parts");
      List<Ingredient> parts = new ArrayList<>();
      for (int i = 0; i < partPattern.length(); i++) {
        String sym = partPattern.substring(i, i + 1);
        Ingredient ingredient = key.get(sym);
        if (ingredient == null) {
          throw new JsonSyntaxException("Parts references symbol '" + sym + "' but it's not defined in the key");
        }
        parts.add(ingredient);
      }
      return new ShapedPartRecipe(recipeId, group, category, width, height, inputs, result, showNotification, List.copyOf(parts), MATERIAL_FIELD.get(json));
    }

    @Override
    @Nullable
    public ShapedPartRecipe fromNetworkSafe(ResourceLocation recipeId, FriendlyByteBuf buffer) {
      ShapedRecipe recipe = SHAPED_RECIPE.fromNetwork(recipeId, buffer);
      List<MaterialVariantId> extraMaterials = MATERIAL_FIELD.decode(buffer);
      int size = buffer.readVarInt();
      List<Ingredient> parts = new ArrayList<>(size);
      List<Ingredient> ingredients = recipe == null ? List.of() : recipe.getIngredients();
      for (int i = 0; i < size; i++) {
        byte index = buffer.readByte();
        if (index == -1 || index >= ingredients.size()) {
          parts.add(Ingredient.EMPTY);
        } else {
          parts.add(ingredients.get(i));
        }
      }
      return recipe == null ? null : new ShapedPartRecipe(recipe, List.copyOf(parts), extraMaterials);
    }

    @Override
    public void toNetworkSafe(FriendlyByteBuf buffer, ShapedPartRecipe recipe) {
      SHAPED_RECIPE.toNetwork(buffer, recipe);
      MATERIAL_FIELD.encode(buffer, recipe);
      // save some memory by just encoding indices of the ingredient instead of the ingredient, we know they are in the larger list
      buffer.writeVarInt(recipe.parts.size());
      List<Ingredient> ingredients = recipe.getIngredients();
      for (Ingredient part : recipe.parts) {
        buffer.writeByte(ingredients.indexOf(part));
      }
    }
  }
}
