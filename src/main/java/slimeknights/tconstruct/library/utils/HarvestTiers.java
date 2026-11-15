package slimeknights.tconstruct.library.utils;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Harvest level display names
 * <p>
 * Since 1.21.2 tiers are vanilla {@link ToolMaterial} records, which have no names or ordering, and NeoForge's tier sorting registry is gone.
 * Vanilla materials are mapped to IDs and sorted explicitly here; materials from other mods have no ID and sort below vanilla ones.
 */
public class HarvestTiers {
  private HarvestTiers() {}

  /** Vanilla materials in harvest order, matching the 1.20.1 sorted tiers with copper (added in 1.21.9) between stone and iron */
  private static final List<ToolMaterial> SORTED = List.of(
    ToolMaterial.WOOD, ToolMaterial.GOLD, ToolMaterial.STONE, ToolMaterial.COPPER, ToolMaterial.IRON, ToolMaterial.DIAMOND, ToolMaterial.NETHERITE);
  /** IDs for each vanilla material, matching the old tier enum names */
  private static final ImmutableBiMap<ToolMaterial, Identifier> IDS = ImmutableBiMap.<ToolMaterial, Identifier>builder()
    .put(ToolMaterial.WOOD, Identifier.withDefaultNamespace("wood"))
    .put(ToolMaterial.GOLD, Identifier.withDefaultNamespace("gold"))
    .put(ToolMaterial.STONE, Identifier.withDefaultNamespace("stone"))
    .put(ToolMaterial.COPPER, Identifier.withDefaultNamespace("copper"))
    .put(ToolMaterial.IRON, Identifier.withDefaultNamespace("iron"))
    .put(ToolMaterial.DIAMOND, Identifier.withDefaultNamespace("diamond"))
    .put(ToolMaterial.NETHERITE, Identifier.withDefaultNamespace("netherite"))
    .build();

  /** Cache of name for each tier */
  private static final Map<ToolMaterial, Component> harvestLevelNames = Maps.newHashMap();
  /** Listener to clear name cache so we get new colors */
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> harvestLevelNames.clear();

  /**
   * Gets the ID used for the name of the given tier. Vanilla tiers use a minecraft namespaced ID;
   * any other tier falls back to a generic ID so a translation key still resolves.
   */
  private static Identifier tierId(ToolMaterial tier) {
    Identifier id = IDS.get(tier);
    return id != null ? id : TConstruct.getResource("unknown");
  }

  /**
   * Gets the ID for the given tier, replacement for {@code TierSortingRegistry.getName}.
   * Only vanilla tiers have a stable ID, returns null for unknown tiers.
   */
  @Nullable
  public static Identifier getId(ToolMaterial tier) {
    return IDS.get(tier);
  }

  /**
   * Gets the tier for the given ID, replacement for {@code TierSortingRegistry.byName}.
   * Only vanilla tiers are resolvable by ID, returns null otherwise.
   */
  @Nullable
  public static ToolMaterial byName(Identifier id) {
    return IDS.inverse().get(id);
  }

  /**
   * Checks if the given tier can harvest the given block for drops, replacement for {@code TierSortingRegistry.isCorrectTierForDrops}.
   * Cross mod tier ordering is gone, so this uses the vanilla incorrect blocks tag.
   */
  public static boolean isCorrectTierForDrops(ToolMaterial tier, BlockState state) {
    return !state.is(tier.incorrectBlocksForDrops());
  }

  /**
   * Gets a numeric sort key for a tier so the larger and smaller helpers keep working without a sorting registry.
   * Mirrors the order of {@code TierSortingRegistry.getSortedTiers()} from 1.20.1: wood, gold, stone, iron, diamond, netherite, with copper between stone and iron.
   * Unknown tiers return -1, sorting below all vanilla tiers.
   */
  private static int sortKey(ToolMaterial tier) {
    return SORTED.indexOf(tier);
  }

  /** Makes a translation key for the given name */
  private static MutableComponent makeLevelKey(ToolMaterial tier) {
    String key = Util.makeTranslationKey("harvest_tier", tierId(tier));
    TextColor color = ResourceColorManager.getTextColor(key);
    return TConstruct.makeTranslation("stat", key).withStyle(style -> style.withColor(color));
  }

  /**
   * Gets the harvest level name for the given level number
   * @param tier  Tier
   * @return  Level name
   */
  public static Component getName(ToolMaterial tier) {
    return harvestLevelNames.computeIfAbsent(tier, n ->  makeLevelKey(tier));
  }

  /** Gets the larger of two tiers */
  public static ToolMaterial max(ToolMaterial a, ToolMaterial b) {
    // note an unknown tier sorts below vanilla tiers, so the larger of an unknown tier and a vanilla one is the vanilla one
    if (sortKey(b) > sortKey(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smaller of two tiers */
  public static ToolMaterial min(ToolMaterial a, ToolMaterial b) {
    // note an unknown tier sorts below vanilla tiers, so the smaller of an unknown tier and a vanilla one is the unknown one
    if (sortKey(b) < sortKey(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smallest tier */
  public static ToolMaterial minTier() {
    // the registry sorted list is gone, so default to the lowest vanilla tier
    return ToolMaterial.WOOD;
  }
}
