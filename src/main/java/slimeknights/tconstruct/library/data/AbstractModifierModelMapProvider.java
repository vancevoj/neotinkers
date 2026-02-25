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
import slimeknights.tconstruct.library.client.modifiers.NormalModifierModel;
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
  private final Map<ResourceLocation, Builder> models = new HashMap<>();

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

  @Override
  public CompletableFuture<?> run(CachedOutput output) {
    addModels();
    return allOf(models.entrySet().stream()
      .filter(file -> !file.getValue().isEmpty())
      .map(file -> saveJson(output, file.getKey(), file.getValue().build())));
  }


  /* Builder */

  /** Gets the builder for the given tool */
  protected Builder tool(ResourceLocation tool) {
    return this.models.computeIfAbsent(tool, id -> new Builder());
  }

  /** Adds the given model to the tool variant */
  protected Builder tool(ResourceLocation tool, String variant) {
    return tool(tool.withSuffix('/' + variant));
  }

  /** Adds the given model to the tool */
  protected Builder tool(IdAwareObject tool) {
    return tool(tool.getId());
  }

  /** Adds the given model to the tool variant */
  protected Builder tool(IdAwareObject tool, String variant) {
    return tool(tool.getId(), variant);
  }

  /** Adds the given model to the tool */
  protected Builder tool(Item tool) {
    return tool(Loadables.ITEM.getKey(tool));
  }

  /** Adds the given model to the tool variant */
  protected Builder tool(Item tool, String variant) {
    return tool(Loadables.ITEM.getKey(tool), variant);
  }

  /** Builder for adding modifier models */
  protected static class Builder {
    private final Map<String, ModifierModel> constant = new LinkedHashMap<>();
    private final Map<ModifierId, ModifierModel> modifiers = new LinkedHashMap<>();

    private Builder() {}

    /** Merges the variable arguments */
    private static ModifierModel merge(ModifierModel model, ModifierModel... models) {
      if (models.length > 0) {
        List<ModifierModel> modelList = new ArrayList<>(models.length + 1);
        modelList.add(model);
        Collections.addAll(modelList, models);
        return new CompoundModifierModel(modelList);
      }
      return model;
    }

    /** Adds a new fixed model that always shows */
    public Builder constant(String id, ModifierModel model, ModifierModel... models) {
      ModifierModel existing = constant.putIfAbsent(id, merge(model, models));
      if (existing != null) {
        throw new IllegalArgumentException("Duplicate constant model: " + id + ", previous " + existing);
      }
      return this;
    }

    /** Adds a new modifier model that shows when the given crafted modifier is present */
    public Builder modifier(ModifierId id, ModifierModel model, ModifierModel... models) {
      ModifierModel existing = modifiers.putIfAbsent(id, merge(model, models));
      if (existing != null) {
        throw new IllegalArgumentException("Duplicate modifier: " + id + ", previous " + existing);
      }
      return this;
    }

    /** Checks if this file has anything */
    private boolean isEmpty() {
      return constant.isEmpty() && modifiers.isEmpty();
    }

    /** Serializes the model to JSON */
    private static JsonElement serialize(ModifierModel model) {
      // serialize compound as an array
      if (model instanceof CompoundModifierModel compound) {
        return CompoundModifierModel.LIST_LOADABLE.serialize(compound.models());
      }
      if (model.getLoader() == NormalModifierModel.LOADER) {
        NormalModifierModel basic = (NormalModifierModel) model;
        Material small = basic.small();
        if (small != null && basic.large() == null && basic.luminosity() == 0 && basic.color() == -1) {
          return new JsonPrimitive(small.texture().toString());
        }
        // type of objects is optional as long as its basic, leave it out so large tools are not as big
        return NormalModifierModel.LOADER.serialize(basic);
      }
      return ModifierModel.LOADER.serialize(model);
    }

    /** Builds the final JSON */
    private JsonObject build() {
      JsonObject json = new JsonObject();
      if (!this.constant.isEmpty()) {
        JsonObject constant = new JsonObject();
        for (Entry<String, ModifierModel> entry : this.constant.entrySet()) {
          constant.add(entry.getKey(), serialize(entry.getValue()));
        }
        json.add("constant", constant);
      }
      if (!this.modifiers.isEmpty()) {
        JsonObject modifiers = new JsonObject();
        for (Entry<ModifierId, ModifierModel> entry : this.modifiers.entrySet()) {
          modifiers.add(entry.getKey().toString(), serialize(entry.getValue()));
        }
        json.add("modifiers", modifiers);
      }
      return json;
    }
  }
}
