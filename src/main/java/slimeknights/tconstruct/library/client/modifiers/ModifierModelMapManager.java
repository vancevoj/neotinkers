package slimeknights.tconstruct.library.client.modifiers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.ModLoader;
import slimeknights.mantle.data.listener.MergingJsonDataLoader;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager.Builder;
import slimeknights.tconstruct.library.client.modifiers.model.CompoundModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Manager for getting modifier models
 */
public class ModifierModelMapManager extends MergingJsonDataLoader<Builder> {
  /** Folder for the modifier models */
  public static final String FOLDER = "tinkering/modifier_models";
  /** Instance of this manager */
  public static final ModifierModelMapManager INSTANCE = new ModifierModelMapManager();

  /** List of loaded models */
  private Map<ResourceLocation, ModifierModelMap> models = new HashMap<>();

  private ModifierModelMapManager() {
    super(JsonHelper.DEFAULT_GSON, FOLDER, id -> new Builder());
  }

  @Override
  public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
    // run in the first stage instead of the second stage
    return CompletableFuture.runAsync(() -> {
      if (ModLoader.isLoadingStateValid()) {
        this.onResourceManagerReload(resourceManager);
      }
    }, backgroundExecutor).thenCompose(stage::wait);
  }

  /** Builder for a given tool model */
  protected static class Builder {
    private final Map<String, JsonElement> constant = new LinkedHashMap<>();
    private final Map<ModifierId, JsonElement> modifiers = new LinkedHashMap<>();
  }

  /** Inserts the given element into the map */
  private static <T> void insert(Map<T, JsonElement> map, T key, JsonElement value, String errorPrefix, ResourceLocation id) {
    // null means discard this model
    if (value.isJsonNull()) {
      map.remove(key);
      // objects are a model to parse
    } else if (value.isJsonObject() || value.isJsonArray()) {
      map.put(key, value.getAsJsonObject());
    } else {
      TConstruct.LOG.error("Invalid entry for {} {} while parsing modifier models {}: expected null, a JSON object, or a JSON array", errorPrefix, key, id);
    }
  }

  @Override
  protected void parse(Builder builder, ResourceLocation id, JsonElement element) throws JsonSyntaxException {
    JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());

    // fixed entries merge at a top level
    if (json.has("constant")) {
      for (Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "constant").entrySet()) {
        insert(builder.constant, entry.getKey(), entry.getValue(), "constant key", id);
      }
    }

    // each entry in the set is a modifier to model pair. We only merge at the top level
    if (json.has("modifiers")) {
      for (Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "modifiers").entrySet()) {
        ModifierId modifier = ModifierId.tryParse(entry.getKey());
        if (modifier == null) {
          TConstruct.LOG.error("Invalid modifier ID {} while parsing modifier models {}", entry.getKey(), id);
        } else {
          insert(builder.modifiers, modifier, entry.getValue(), "modifier", id);
        }
      }
    }
  }

  /** Creates context for constant key parsing */
  private static TypedMap context(ResourceLocation file, String key) {
    return TypedMapBuilder.builder()
      .put(ContextKey.ID, file)
      .put(ContextKey.DEBUG, "Model Map " + file + " for constant key " + key)
      .build();
  }

  /** Creates context for modifier parsing */
  private static TypedMap context(ResourceLocation file, ModifierId modifier) {
    return TypedMapBuilder.builder()
      .put(ContextKey.ID, file)
      .put(ModifierId.CONTEXT_KEY, modifier)
      .put(ContextKey.DEBUG, "Model Map " + file + " for Modifier " + modifier)
      .build();
  }

  /** Parses the given model from the map */
  private static <T> void parseModel(Map<T, ModifierModel> map, T key, JsonElement value, String errorPrefix, ResourceLocation id, BiFunction<ResourceLocation, T,TypedMap> context) {
    try {
      // if it's an object, it's a single model
      ModifierModel model;
      if (value.isJsonObject()) {
        model = ModifierModel.LOADER.deserialize(value.getAsJsonObject(), context.apply(id, key));
      } else {
        // for simplicity, treat an array as a compound
        model = CompoundModifierModel.create(CompoundModifierModel.LIST_LOADABLE.convert(value, key.toString(), context.apply(id, key)));
      }
      // model may be empty if a load condition failed
      if (model != ModifierModel.EMPTY) {
        map.put(key, model);
      }
    } catch (JsonSyntaxException e) {
      TConstruct.LOG.error("Failed to parse modifier model map {} for {} {}", id, errorPrefix, key, e);
    }
  }

  @Override
  protected void finishLoad(Map<ResourceLocation, Builder> map, ResourceManager manager) {
    BiFunction<ResourceLocation,String,TypedMap> constantContext = ModifierModelMapManager::context;
    BiFunction<ResourceLocation,ModifierId,TypedMap> modifierContext = ModifierModelMapManager::context;

    Map<ResourceLocation, ModifierModelMap> modelMaps = new HashMap<>();
    for (Entry<ResourceLocation, Builder> file : map.entrySet()) {
      ResourceLocation id = file.getKey();
      Map<String, ModifierModel> constant = new LinkedHashMap<>();
      Map<ModifierId, ModifierModel> modifiers = new HashMap<>();
      for (Entry<String,JsonElement> entry : file.getValue().constant.entrySet()) {
        parseModel(constant, entry.getKey(), entry.getValue(), "constant key", id, constantContext);
      }
      for (Entry<ModifierId,JsonElement> entry : file.getValue().modifiers.entrySet()) {
        parseModel(modifiers, entry.getKey(), entry.getValue(), "modifier", id, modifierContext);
      }
      // ensure we actually managed to parse something
      ModifierModelMap modelMap = ModifierModelMap.create(constant, modifiers);
      if (modelMap != ModifierModelMap.EMPTY) {
        modelMaps.put(id, modelMap);
      }
    }
    this.models = Map.copyOf(modelMaps);
  }

  @Override
  public void onResourceManagerReload(ResourceManager manager) {
    long time = System.nanoTime();
    super.onResourceManagerReload(manager);
    TConstruct.LOG.info("{} modifier model maps in {} ms", this.models.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Gets a map of modifier models for the given tool */
  public ModifierModelMap getModelsForTool(Function<Material, TextureAtlasSprite> spriteGetter, List<ResourceLocation> options) {
    if (options.isEmpty()) {
      return ModifierModelMap.EMPTY;
    }
    // first, load in the map
    ModifierModelMap modelMap;
    // if only one is requested, reuse that instance
    if (options.size() == 1) {
      modelMap = this.models.getOrDefault(options.get(0), ModifierModelMap.EMPTY);
    } else {
      Map<String, ModifierModel> constant = new LinkedHashMap<>();
      Map<ModifierId, IBakedModifierModel> modifiers = new HashMap<>();
      // loop backwards as we want the first that appears to take priority
      for (int i = options.size() - 1; i >= 0; i--) {
        ModifierModelMap optionMap = this.models.get(options.get(i));
        if (optionMap != null) {
          constant.putAll(optionMap.constant());
          modifiers.putAll(optionMap.modifiers());
        }
      }
      modelMap = ModifierModelMap.create(constant, modifiers);
    }
    // validate all model textures
    for (ModifierModel model : modelMap.constant().values()) {
      model.validate(spriteGetter);
    }
    for (IBakedModifierModel model : modelMap.modifiers().values()) {
      // we loaded this map in so know the type
      ((ModifierModel)model).validate(spriteGetter);
    }
    return modelMap;
  }

  /** Gets a map of modifier models for the given tool, considering the legacy model system */
  public ModifierModelMap getModelsForTool(Function<Material, TextureAtlasSprite> spriteGetter, List<ResourceLocation> options, List<ResourceLocation> smallRoots, List<ResourceLocation> largeRoots, ResourceLocation modelLocation) {
    ModifierModelMap models = getModelsForTool(spriteGetter, options);
    // if not using the legacy system, we are done
    if (smallRoots.isEmpty() && largeRoots.isEmpty()) {
      return models;
    }
    Map<ModifierId, IBakedModifierModel> legacy = ModifierModelManager.getModelsForTool(spriteGetter, smallRoots, largeRoots);
    // nothing on the old system? nothing to do
    if (legacy.isEmpty()) {
      return models;
    }
    // if nothing is on the new system, just return the legacy one with a warning
    if (models.isEmpty()) {
      TConstruct.LOG.warn("Tool model {} is using deprecated system for modifier models. Use modifier model maps to apply modifier textures.", modelLocation);
      return ModifierModelMap.create(Map.of(), legacy);
    }
    // have both so we need to combine
    Map<ModifierId, IBakedModifierModel> builder = new HashMap<>(models.modifiers());
    boolean hasLegacy = false;
    for (Entry<ModifierId, IBakedModifierModel> entry : legacy.entrySet()) {
      if (builder.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
        hasLegacy = true;
      }
    }
    // only warn of legacy usage if we have legacy models
    if (hasLegacy) {
      TConstruct.LOG.warn("Tool model {} is using deprecated system for modifier models. Use modifier model maps to apply modifier textures.", modelLocation);
    }
    return ModifierModelMap.create(models.constant(), builder);
  }
}
