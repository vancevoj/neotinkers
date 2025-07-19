package slimeknights.tconstruct.common.data;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.predicate.TinkerPredicate;
import slimeknights.tconstruct.shared.command.subcommand.generate.GenerateMeltingRecipesCommand;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Data generator for someone-off JSON files used for command configuration */
public class ConfigurationDataProvider extends GenericDataProvider {
  private final Map<ResourceLocation, JsonObject> configuration = new LinkedHashMap<>();
  public ConfigurationDataProvider(PackOutput output) {
    super(output, Target.DATA_PACK, "");
  }

  @Override
  public CompletableFuture<?> run(CachedOutput output) {
    JsonObject meltingRecipes = config(GenerateMeltingRecipesCommand.MELTING_CONFIGURATION);
    item(meltingRecipes, "melt", ItemPredicate.and(
      ItemPredicate.tag(TinkerTags.Items.MODIFIABLE).inverted(),
      ItemPredicate.tag(TinkerTags.Items.BOOKS).inverted(),
      TinkerPredicate.MAY_HAVE_FLUID.inverted()
    ));
    item(meltingRecipes, "inputs", TinkerPredicate.MAY_HAVE_FLUID.inverted());
    item(meltingRecipes, "ignore", ItemPredicate.ANY);

    // save all JSON
    return allOf(configuration.entrySet().stream().map(entry -> saveJson(output, entry.getKey(), entry.getValue())));
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Configuration Data Provider";
  }

  /** Gets or creates a config object */
  private JsonObject config(ResourceLocation location) {
    String path = location.getPath();
    return configuration.computeIfAbsent(location.withPath(path.substring(0, path.length() - ".json".length())), p -> new JsonObject());
  }

  /** Adds an item predicate */
  private static void item(JsonObject json, String key, IJsonPredicate<Item> value) {
    json.add(key, ItemPredicate.LOADER.serialize(value));
  }
}
