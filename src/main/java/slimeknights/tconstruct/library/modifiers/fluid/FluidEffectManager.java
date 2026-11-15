package slimeknights.tconstruct.library.modifiers.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.typed.TypedMapBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Manager for spilling fluids for spilling, slurping, and wetting */
public class FluidEffectManager extends SimpleJsonResourceReloadListener<JsonElement> {
  /** Recipe folder */
  public static final String FOLDER = "tinkering/fluid_effects";

  /** Singleton instance of the modifier manager */
  public static final FluidEffectManager INSTANCE = new FluidEffectManager();

  /** List of available fluids */
  @Getter
  private List<FluidEffects.Entry> fluids = List.of();
  /** Cache of fluid to recipe, recipe will be null client side */
  private final Map<Fluid,FluidEffects> cache = new ConcurrentHashMap<>();

  /** Empty spilling fluid instance */
  private static final FluidEffects EMPTY = new FluidEffects(FluidIngredient.EMPTY, List.of(), List.of(), true);

  private FluidEffectManager() {
    super(ExtraCodecs.JSON, FileToIdConverter.json(FOLDER));
  }

  /** For internal use only */
  public void init() {
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AddServerReloadListenersEvent.class, this::addDataPackListeners);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OnDatapackSyncEvent.class, e -> JsonUtils.syncPackets(e, new UpdateFluidEffectsPacket(this.fluids)));
  }

  /** Adds the managers as datapack listeners. Condition context and registries are injected into the listener by NeoForge. */
  private void addDataPackListeners(final AddServerReloadListenersEvent event) {
    event.addListener(TConstruct.getResource("fluid_effects"), this);
  }

  /** Creates context for modifier parsing */
  public static TypedMapBuilder contextBuilder(Identifier key) {
    return TypedMapBuilder.builder().put(ContextKey.ID, key).put(ContextKey.DEBUG, "Fluid Effect " + key);
  }

  /**
   * Replacement for the removed {@code CraftingHelper.processConditions(JsonObject, String, IContext)}. Parses the optional
   * {@code "conditions"} array via {@link ICondition#LIST_CODEC} and tests each condition against the given context.
   * @return  true if all conditions pass (or no conditions are present)
   */
  private static boolean processConditions(JsonObject json, IContext context, HolderLookup.Provider registries) {
    if (!json.has("conditions")) {
      return true;
    }
    List<ICondition> conditions = ICondition.LIST_CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json.get("conditions")).getOrThrow(JsonSyntaxException::new);
    for (ICondition condition : conditions) {
      if (!condition.test(context)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void apply(Map<Identifier,JsonElement> splashList, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    long time = System.nanoTime();
    // condition context and registries are injected by NeoForge for the duration of the reload
    IContext conditionContext = getContext();
    HolderLookup.Provider registries = getRegistryLookup();

    // load spilling from JSON
    List<FluidEffects.Entry> fluids = new ArrayList<>(splashList.size());
    for (Entry<Identifier,JsonElement> entry : splashList.entrySet()) {
      Identifier key = entry.getKey();
      try {
        JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "fluid_effect");

        // want to parse condition without parsing effects, as the effect serializer may be missing
        if (!processConditions(json, conditionContext, registries)) {
          continue;
        }
        TypedMapBuilder context = contextBuilder(key).put(ContextKey.CONDITION_CONTEXT, conditionContext);
        if (registries != RegistryAccess.EMPTY) {
          context.put(ContextKey.REGISTRIES, registries);
        }
        fluids.add(new FluidEffects.Entry(key, FluidEffects.LOADABLE.deserialize(json, context.build())));
      } catch (JsonSyntaxException e) {
        TConstruct.LOG.error("Failed to load fluid effect {}", key, e);
      }
    }
    this.fluids = List.copyOf(fluids);
    this.cache.clear();
    TConstruct.LOG.info("Loaded {} spilling fluids in {} ms", fluids.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Updates the modifiers from the server */
  @Internal
  void updateFromServer(List<FluidEffects.Entry> fluids) {
    this.fluids = fluids;
    this.cache.clear();
  }

  /** Finds a fluid without checking the cache, returns null if missing */
  private final Function<Fluid,FluidEffects> FIND_UNCACHED = fluid -> {
    // find all severing recipes for the entity
    for (FluidEffects.Entry entry : fluids) {
      FluidEffects effects = entry.effects();
      if (effects.matches(fluid)) {
        return effects;
      }
    }
    // cache null if nothing
    return EMPTY;
  };

  /**
   * Gets the recipe for the given fluid. Does not work client side
   * @param fluid    Fluid
   * @return  Fluid, or empty if none exists
   */
  public FluidEffects find(Fluid fluid) {
    return cache.computeIfAbsent(fluid, FIND_UNCACHED);
  }
}
