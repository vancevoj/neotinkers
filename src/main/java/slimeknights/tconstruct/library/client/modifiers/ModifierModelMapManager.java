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
import slimeknights.tconstruct.library.client.modifiers.model.CompoundModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Manager for getting modifier models
 */
public class ModifierModelMapManager extends MergingJsonDataLoader<Map<ModifierId, JsonElement>> {
  /** Folder for the modifier models */
  public static final String FOLDER = "tinkering/modifier_models";
  /** Instance of this manager */
  public static final ModifierModelMapManager INSTANCE = new ModifierModelMapManager();

  /** List of loaded models */
  private Map<ResourceLocation, Map<ModifierId, ModifierModel>> models = new HashMap<>();

  private ModifierModelMapManager() {
    super(JsonHelper.DEFAULT_GSON, FOLDER, id -> new HashMap<>());
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

  @Override
  protected void parse(Map<ModifierId, JsonElement> builder, ResourceLocation id, JsonElement element) throws JsonSyntaxException {
    JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
    // each entry in the set is a modifier to model pair. We only merge at the top level
    for (Entry<String,JsonElement> entry : json.entrySet()) {
      // TODO: special case empty string as a mergable list of always shown modifiers
      ModifierId modifier = ModifierId.tryParse(entry.getKey());
      if (modifier == null) {
        TConstruct.LOG.error("Invalid modifier ID {} while parsing modifier models {}", entry.getKey(), id);
      } else {
        JsonElement value = entry.getValue();
        // null means discard this model
        if (value.isJsonNull()) {
          builder.remove(modifier);
          // objects are a model to parse
        } else if (value.isJsonObject() || value.isJsonArray()) {
          builder.put(modifier, value.getAsJsonObject());
        } else {
          TConstruct.LOG.error("Invalid entry for modifier {} while parsing modifier models {}: expected null, a JSON object, or a JSON array", modifier, id);
        }
      }
    }
  }

  /** Creates context for modifier parsing */
  private static TypedMap context(ResourceLocation file, ModifierId modifier) {
    return TypedMapBuilder.builder()
      .put(ContextKey.ID, file)
      .put(ModifierId.CONTEXT_KEY, modifier)
      .put(ContextKey.DEBUG, "Model Map " + file + " for Modifier " + modifier)
      .build();
  }

  @Override
  protected void finishLoad(Map<ResourceLocation, Map<ModifierId, JsonElement>> map, ResourceManager manager) {
    Map<ResourceLocation, Map<ModifierId, ModifierModel>> modelMaps = new HashMap<>();
    for (Entry<ResourceLocation, Map<ModifierId, JsonElement>> file : map.entrySet()) {
      ResourceLocation id = file.getKey();
      Map<ModifierId, ModifierModel> models = new HashMap<>();
      for (Entry<ModifierId, JsonElement> entry : file.getValue().entrySet()) {
        ModifierId modifier = entry.getKey();
        JsonElement value = entry.getValue();
        try {
          // if it's an object, it's a single model
          ModifierModel model;
          if (value.isJsonObject()) {
            model = ModifierModel.LOADER.deserialize(value.getAsJsonObject(), context(id, modifier));
          } else {
            // for simplicity, treat an array as a compound
            model = CompoundModifierModel.create(CompoundModifierModel.LIST_LOADABLE.convert(value, modifier.toString()));
          }
          // model may be empty if a load condition failed
          if (model != ModifierModel.EMPTY) {
            models.put(modifier, model);
          }
        } catch (JsonSyntaxException e) {
          TConstruct.LOG.error("Failed to parse modifier model map {} for modifier ID {}", id, modifier, e);
        }
      }
      modelMaps.put(id, Map.copyOf(models));
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
  public Map<ModifierId, ModifierModel> getModelsForTool(Function<Material, TextureAtlasSprite> spriteGetter, List<ResourceLocation> options) {
    if (options.isEmpty()) {
      return Map.of();
    }
    // first, load in the map
    Map<ModifierId, ModifierModel> modelMap;
    // if only one is requested, reuse that instance
    if (options.size() == 1) {
      modelMap = this.models.getOrDefault(options.get(0), Map.of());
    } else {
      modelMap = new HashMap<>();
      // loop backwards as we want the first that appears to take priority
      for (int i = options.size() - 1; i >= 0; i--) {
        ResourceLocation option = options.get(i);
        Map<ModifierId, ModifierModel> optionMap = this.models.get(option);
        if (optionMap != null) {
          modelMap.putAll(optionMap);
        }
      }
      modelMap = Map.copyOf(modelMap);
    }
    // validate all model textures
    for (ModifierModel model : modelMap.values()) {
      model.validate(spriteGetter);
    }
    return modelMap;
  }

  /** Gets a map of modifier models for the given tool, considering the legacy model system */
  public Map<ModifierId, ? extends IBakedModifierModel> getModelsForTool(Function<Material, TextureAtlasSprite> spriteGetter, List<ResourceLocation> options, List<ResourceLocation> smallRoots, List<ResourceLocation> largeRoots, ResourceLocation modelLocation) {
    Map<ModifierId, ModifierModel> models = getModelsForTool(spriteGetter, options);
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
      return legacy;
    }
    // have both so we need to combine
    Map<ModifierId, IBakedModifierModel> builder = new HashMap<>(models);
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
    return Map.copyOf(builder);
  }
}
