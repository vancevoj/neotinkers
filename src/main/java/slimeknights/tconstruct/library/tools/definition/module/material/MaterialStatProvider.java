package slimeknights.tconstruct.library.tools.definition.module.material;

import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStatsBuilder;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container class for a material stat provider
 * @param getId         Unique ID for this provider
 * @param requiredType  At least one type from this set must be included to be valid
 * @param otherTypes    Other valid stat types, any parts with types not in this list will error
 * @param builder       Function to create a builder
 *
 */
public record MaterialStatProvider(ResourceLocation getId, Set<MaterialStatsId> requiredType, Set<MaterialStatsId> otherTypes, BiFunction<List<WeightedStatType>,MaterialNBT,ToolStatsBuilder> builder) implements IdAwareObject {
  /** Builds the stats from the given definition and materials */
  public void addStats(List<WeightedStatType> statTypes, MaterialNBT materials, ModifierStatsBuilder statBuilder) {
    // if the NBT is invalid, no-op to prevent an exception here (could kill itemstacks)
    if (materials.size() == statTypes.size()) {
      this.builder.apply(statTypes, materials).addStats(statBuilder);
    }
  }

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
      throw error.create(getId + " must have at least one tool part");
    }
    boolean foundHead = false;
    for (WeightedStatType weightedStat : stats) {
      MaterialStatsId statType = weightedStat.stat();
      if (requiredType.contains(statType)) {
        foundHead = true;
      } else if (!otherTypes.contains(statType)) {
        throw new IllegalStateException(getId + " does not support type " + statType + ", must be one of: " + join(Stream.concat(requiredType.stream(), otherTypes.stream())));
      }
    }
    if (!foundHead) {
      throw new IllegalStateException(getId + " must use at least one of " + join(requiredType.stream()) + " part");
    }
  }
}
