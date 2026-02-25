package slimeknights.tconstruct.library.data;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.model.Material;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.library.client.modifiers.model.CompoundModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

/** Data provider for modifier model maps */
public abstract class AbstractModifierModelMapProvider extends GenericDataProvider {
  private final Map<ResourceLocation, Map<ModifierId, ModifierModel>> models = new HashMap<>();

  private final String modId;
  public AbstractModifierModelMapProvider(PackOutput output, String modId) {
    super(output, Target.RESOURCE_PACK, ModifierModelMapManager.FOLDER);
    this.modId = modId;
  }

  /** Creates a new material for the given texture */
  protected Material material(ResourceLocation texture) {
    return ModifierModel.blockAtlas(texture);
  }

  /** Creates a new material for the given texture */
  @SuppressWarnings("removal")
  protected Material material(String texture) {
    return ModifierModel.blockAtlas(new ResourceLocation(modId, texture));
  }

  /** Creates a tool texture for the given name */
  protected Material toolMaterial(String texture) {
    return material("item/tool/" + texture);
  }

  /** Adds all models */
  protected abstract void addModels();

  /** Adds the given model to the tool */
  protected void model(ResourceLocation tool, ModifierId modifier, ModifierModel model, ModifierModel... models) {
    if (models.length > 0) {
      List<ModifierModel> modelList = new ArrayList<>(models.length + 1);
      modelList.add(model);
      Collections.addAll(modelList, models);
      model = new CompoundModifierModel(modelList);
    }
    ModifierModel existing = this.models.computeIfAbsent(tool, loc -> new LinkedHashMap<>()).putIfAbsent(modifier, model);
    if (existing != null) {
      throw new IllegalArgumentException("Duplicate modifier model for " + modifier + ", already had " + existing);
    }
  }

  /** Adds the given model to the tool variant */
  protected void model(ResourceLocation tool, String variant, ModifierId modifier, ModifierModel model, ModifierModel... models) {
    model(tool.withSuffix('/' + variant), modifier, model, models);
  }

  /** Adds the given model to the tool */
  protected void model(IdAwareObject tool, ModifierId modifier, ModifierModel model, ModifierModel... models) {
    model(tool.getId(), modifier, model, models);
  }

  /** Adds the given model to the tool variant */
  protected void model(IdAwareObject tool, String variant, ModifierId modifier, ModifierModel model, ModifierModel... models) {
    model(tool.getId(), variant, modifier, model, models);
  }

  /** Adds the given model to the tool */
  protected void model(Item tool, ModifierId modifier, ModifierModel model, ModifierModel... models) {
    model(Loadables.ITEM.getKey(tool), modifier, model, models);
  }

  /** Adds the given model to the tool variant */
  protected void model(Item tool, String variant, ModifierId modifier, ModifierModel model, ModifierModel... models) {
    model(Loadables.ITEM.getKey(tool), variant, modifier, model, models);
  }

  @Override
  public CompletableFuture<?> run(CachedOutput output) {
    addModels();
    return allOf(models.entrySet().stream()
      .filter(file -> !file.getValue().isEmpty())
      .map(file -> {
        JsonObject json = new JsonObject();
        for (Entry<ModifierId, ModifierModel> entry : file.getValue().entrySet()) {
          json.add(entry.getKey().toString(), ModifierModel.LOADER.serialize(entry.getValue()));
        }
        return saveJson(output, file.getKey(), json);
      }));
  }
}
