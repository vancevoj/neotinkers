package slimeknights.tconstruct.library.client.armor;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.tools.client.material.CombatFishingHookRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ArmorModelManager extends SimpleJsonResourceReloadListener<JsonElement> {
  /** Folder containing the logic */
  public static final String FOLDER = "tinkering/armor_models";
  /** ID of this reload listener */
  public static final Identifier LISTENER_ID = TConstruct.getResource("armor_models");

  /** Object representing parsed models */
  public record ArmorModel(List<ArmorTextureSupplier> layers) {
    /** Empty instace for fallback */
    public static final ArmorModel EMPTY = new ArmorModel(List.of());
    /** Loadable for JSON parsing */
    public static final RecordLoadable<ArmorModel> LOADABLE = RecordLoadable.create(ArmorTextureSupplier.LOADER.list(1).requiredField("layers", ArmorModel::layers), ArmorModel::new);
  }

  /* Instance data */
  public static final ArmorModelManager INSTANCE = new ArmorModelManager();
  /** Map of location to texture suppliers */
  private Map<Identifier,ArmorModel> models = Collections.emptyMap();

  private static final List<ArmorModelDispatcher> DISPATCHERS = new ArrayList<>();

  /**
   * Initializes this manager, registering it with the resource manager
   * @param manager  Manager
   */
  public static void init(AddClientReloadListenersEvent manager) {
    manager.addListener(LISTENER_ID, INSTANCE);
  }

  private ArmorModelManager() {
    super(ExtraCodecs.JSON, FileToIdConverter.json(FOLDER));
  }

  @Override
  protected void apply(Map<Identifier,JsonElement> splashList, ResourceManager manager, ProfilerFiller pProfiler) {
    long time = System.nanoTime();

    // first, load in all fluid textures, means we are allowed to reference them in fluid texture supplier constructors
    ArmorTextureSupplier.TEXTURE_VALIDATOR.onReloadSafe(manager);
    // reuses armor model logic for simplicity
    CombatFishingHookRenderer.clearCache();

    // load all models
    ImmutableMap.Builder<Identifier,ArmorModel> builder = ImmutableMap.builder();
    for (Entry<Identifier,JsonElement> entry : splashList.entrySet()) {
      Identifier key = entry.getKey();
      JsonElement element = entry.getValue();
      try {
        builder.put(key, ArmorModel.LOADABLE.convert(element, key.toString()));
      } catch (JsonSyntaxException e) {
        TConstruct.LOG.error("Failed to load armor model from {}", key, e);
      }
    }

    this.models = builder.build();
    // clear dispatcher model cache
    Set<Identifier> missing = new HashSet<>();
    for (ArmorModelDispatcher dispatcher : DISPATCHERS) {
      dispatcher.model = null;
      Identifier name = dispatcher.getName();
      if (!this.models.containsKey(name)) {
        missing.add(name);
      }
    }
    if (!missing.isEmpty()) {
      TConstruct.LOG.error("Missing armor models used by items: {}", missing);
    }
    TConstruct.LOG.info("Loaded {} armor models in {} ms", models.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Gets the armor model for the given location. Location typically corresponds to armor material name */
  public ArmorModel getModel(Identifier name) {
    return models.getOrDefault(name, ArmorModel.EMPTY);
  }

  /**
   * Helper to cache armor models in the item.
   * <p>
   * 26.3: register instances through {@code RegisterClientExtensionsEvent#registerItem} (items no longer have {@code initializeClient}).
   * Rendering moved from {@code getGenericArmorModel} to {@link #submitArmor}, called by {@link TinkerArmorLayer} for every humanoid
   * renderer with a vanilla armor layer. The item must still have an {@link net.minecraft.world.item.equipment.Equippable} with an asset ID
   * (the asset JSON may be missing) so vanilla copies it into the render state, the vanilla equipment layers are skipped by
   * {@link #getArmorLayerTintColor}.
   */
  public abstract static class ArmorModelDispatcher implements IClientItemExtensions {
    private ArmorModel model;

    public ArmorModelDispatcher() {
      DISPATCHERS.add(this);
    }

    /**
     * Gets the name of the model to use.
     * Not a constructor parameter as forge initializes client extensions before we can store fields from the parent constructor.
     */
    protected abstract Identifier getName();

    /** Fetches the model from the cache */
    protected ArmorModel getModel(ItemStack stack) {
      if (model == null) {
        model = ArmorModelManager.INSTANCE.getModel(getName());
        if (model == ArmorModel.EMPTY) {
          TConstruct.LOG.warn("Failed to find armor model {}, will skip rendering {}", getName(), stack);
        }
      }
      return model;
    }

    /**
     * Submits this armor piece, replaces {@code getGenericArmorModel}. Override to customize rendering, such as the slimeskull.
     * @param matrices     Pose stack, positioned like the entity model
     * @param collector    Collector to submit to
     * @param packedLight  Light coordinates
     * @param state        Humanoid render state of the wearer, poses the model when it is drawn
     * @param stack        Armor stack
     * @param slot         Slot the stack is worn in
     * @param armorModel   Vanilla armor model for the slot, drawn once per texture layer
     */
    public <S extends HumanoidRenderState> void submitArmor(PoseStack matrices, SubmitNodeCollector collector, int packedLight, S state, ItemStack stack, EquipmentSlot slot, HumanoidModel<S> armorModel) {
      MultilayerArmorModel.INSTANCE.submit(matrices, collector, packedLight, state, stack, slot, armorModel, getModel(stack));
    }

    /** Skips every vanilla equipment layer, all rendering happens in {@link #submitArmor} through {@link TinkerArmorLayer} */
    @Override
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor) {
      return 0;
    }
  }
}
