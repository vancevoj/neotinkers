package slimeknights.tconstruct.library.tools.stat;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Extendable utilities for a stats builder.
 * <p>
 * It's encouraged to extend this for the base of your calculation. Using this class directly will give a no parts stat builder
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ToolStatsBuilder {
  /**
   * Called after bonuses are processed to set the unique stats for this builder.
   * @param builder  Stats builder
   */
  public abstract void addStats(ModifierStatsBuilder builder);

  /** Gets the given stat, returning a default if its missing instead of the stat's default */
  @SuppressWarnings("SameParameterValue")
  protected <T extends Number> T getStatOrDefault(IToolStat<T> stat, T defaultValue) {
    return defaultValue;
  }


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
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    // iterating both lists at once, precondition that they have the same size
    int size = statTypes.size();
    for (int i = 0; i < size; i++) {
      // ensure stat type is valid
      WeightedStatType statType = statTypes.get(i);
      if (statType.stat().equals(statsId)) {
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
}
