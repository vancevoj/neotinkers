package slimeknights.tconstruct.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;

/**
 * Simple helper for saving a recipe serialized with a loadable.
 * In 1.21 {@link RecipeOutput} encodes the recipe through its serializer codec, so the finished recipe wrapper and loader are no longer needed.
 * Recipes also no longer expose a common ID getter, so the ID is passed in. Since 1.21.2 recipes are keyed by a {@link ResourceKey}.
 * TODO: move to Mantle
 */
public final class LoadableFinishedRecipe {
  private LoadableFinishedRecipe() {}

  /** One-liner to accept a new recipe with no advancement */
  public static <T extends Recipe<?>> void save(RecipeOutput consumer, Identifier id, T recipe) {
    consumer.accept(ResourceKey.create(Registries.RECIPE, id), recipe, null);
  }

  /**
   * Gets the item ID of an output for a default recipe ID. Avoids creating the output stack where possible, as since 26.1 item components are not bound during datagen.
   * @param output  Item output
   * @return  Item ID of the output
   */
  public static Identifier getItemId(ItemOutput output) {
    JsonElement json = output.serialize(false);
    if (json.isJsonObject()) {
      JsonObject object = json.getAsJsonObject();
      if (object.has("item")) {
        json = object.get("item");
      }
    }
    if (json.isJsonPrimitive()) {
      return Identifier.parse(json.getAsString());
    }
    // tag outputs have no item until the tag preference resolves
    return Loadables.ITEM.getKey(output.get().getItem());
  }

  /**
   * Gets the type aware serializer for a registered serializer, for builders constructing recipes that take a {@link TypeAwareRecipeSerializer}.
   * Since 26.1 serializers are final records, so the type aware serializer is a separate object linked to the registered serializer.
   * @param serializer  Registered serializer, created by {@link LoadableRecipeSerializer#of(slimeknights.mantle.data.loadable.record.RecordLoadable, java.util.function.Supplier)}
   * @param <T>  Recipe type
   * @return  Type aware serializer
   * @throws IllegalArgumentException  If the serializer is not type aware
   */
  public static <T extends Recipe<?>> TypeAwareRecipeSerializer<T> typeAware(RecipeSerializer<T> serializer) {
    TypeAwareRecipeSerializer<T> typeAware = LoadableRecipeSerializer.getTypeAware(serializer);
    if (typeAware == null) {
      throw new IllegalArgumentException("Recipe serializer " + serializer + " is not type aware");
    }
    return typeAware;
  }
}
