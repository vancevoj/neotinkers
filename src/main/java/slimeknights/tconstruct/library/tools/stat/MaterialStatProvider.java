package slimeknights.tconstruct.library.tools.stat;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialStatsModule.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extendable utilities for a material stat builder.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class MaterialStatProvider implements IdAwareObject {
  @Getter
  private final ResourceLocation id;
  protected final Set<MaterialStatsId> requiredType;
  protected final Set<MaterialStatsId> otherTypes;

  /**
   * Called after bonuses are processed to set the unique stats for this builder.
   * @param builder  Stats builder
   */
  public abstract void addStats(List<WeightedStatType> statTypes, MaterialNBT materials, ModifierStatsBuilder builder);


  /* Helpers */

  /**
   * Fetches the given stat from the material, getting the default stats if missing
   * @param material   Material type
   * @param statsId    Stat type
   * @param <T>  Stat type
   * @return  Stat, or default if the part type accepts it, null if the part type does not
   */
  @Nullable
  public static <T extends IMaterialStats> T fetchStatsOrDefault(MaterialId material, MaterialStatsId statsId) {
      return MaterialRegistry.getInstance().<T>getMaterialStats(material, statsId)
        .orElseGet(() -> MaterialRegistry.getInstance().getDefaultStats(statsId));
  }

  /**
   * Gets a list of all stats for the given part type
   * @param statsId             Stat type
   * @param materials           Materials list
   * @param statTypes  List of required components, filters stat types
   * @param <T>  Type of stats
   * @return  List of stats
   */
  public static <T extends IMaterialStats> List<T> listOfCompatibleWith(MaterialStatsId statsId, MaterialNBT materials, List<WeightedStatType> statTypes) {
    return listOfCompatibleWith(Set.of(statsId), materials, statTypes);
  }

  /**
   * Gets a list of all stats for the given part type
   * @param statsIds            Stat IDs to fetch. Its the callers responsibility to make sure they all use the correct type.
   * @param materials           Materials list
   * @param statTypes  List of required components, filters stat types
   * @param <T>  Type of stats
   * @return  List of stats
   */
  public static <T extends IMaterialStats> List<T> listOfCompatibleWith(Set<MaterialStatsId> statsIds, MaterialNBT materials, List<WeightedStatType> statTypes) {
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    // iterating both lists at once, precondition that they have the same size
    int size = statTypes.size();
    for (int i = 0; i < size; i++) {
      // ensure stat type is valid
      WeightedStatType statType = statTypes.get(i);
      if (statsIds.contains(statType.stat())) {
        T stats = fetchStatsOrDefault(materials.get(i).getId(), statType.stat());
        if (stats != null) {
          // add a copy of the stat once per weight, lazy way to do weighting
          for (int w = 0; w < statType.weight(); w++) {
            builder.add(stats);
          }
        }
      }
    }
    return builder.build();
  }

  /**
   * Gets the average value from a list of stat types
   * @param stats       Stat list
   * @param statGetter  Function to get the value
   * @param <T>  Material type
   * @return  Average value
   */
  public static <T extends IMaterialStats> float getAverageValue(List<T> stats, Function<T, ? extends Number> statGetter) {
    return getAverageValue(stats, statGetter, 0);
  }

  /**
   * Gets the average value from a list of stat types
   * @param stats         Stat list
   * @param statGetter    Function to get the value
   * @param missingValue  Default value to use for missing stats
   * @param <T>  Material type
   * @return  Average value
   */
  public static <T extends IMaterialStats, N extends Number> float getAverageValue(List<T> stats, Function<T, N> statGetter, double missingValue) {
    return (float)stats.stream()
                .mapToDouble(value -> statGetter.apply(value).doubleValue())
                .average()
                .orElse(missingValue);
  }

  /**
   * Gets the average value from a list of stat types
   * @param stats         Stat list
   * @param statGetter    Function to get the value
   * @param <T>  Material type
   * @return  Average value
   */
  public static <T extends IMaterialStats, N extends Number> float getTotalValue(List<T> stats, Function<T, N> statGetter) {
    return (float)stats.stream()
                .mapToDouble(value -> statGetter.apply(value).doubleValue())
                .sum();
  }


  /* JSON parsing */

  /** Joins the stats by commas */
  private static String join(Stream<MaterialStatsId> stats) {
    return stats.map(MaterialStatsId::toString).collect(Collectors.joining(", "));
  }

  /**
   * Validates that the given parts meet the requirements
   * @param stats  Stats to validate
   * @param error  Error factory
   */
  public void validate(List<WeightedStatType> stats, ErrorFactory error) {
    // have a required type, make sure it exists and no unexpected types exist
    if (stats.isEmpty()) {
      throw error.create(id + " must have at least one tool part");
    }
    boolean foundHead = false;
    for (WeightedStatType weightedStat : stats) {
      MaterialStatsId statType = weightedStat.stat();
      if (requiredType.contains(statType)) {
        foundHead = true;
      } else if (!otherTypes.contains(statType)) {
        throw new IllegalStateException(id + " does not support type " + statType + ", must be one of: " + join(Stream.concat(requiredType.stream(), otherTypes.stream())));
      }
    }
    if (!foundHead) {
      throw new IllegalStateException(id + " must use at least one of " + join(requiredType.stream()) + " part");
    }
  }
}
