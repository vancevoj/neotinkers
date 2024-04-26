package slimeknights.tconstruct.tools.stats;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.IRepairableMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.tools.item.ArmorSlotType.ArmorBuilder;

import java.util.List;

/** Material stat class handling all four plating types */
public record PlatingMaterialStats(MaterialStatType<?> getType, int durability, float armor, float toughness, float knockbackResistance) implements IRepairableMaterialStats {
  private static final RecordLoadable<PlatingMaterialStats> LOADABLE = RecordLoadable.create(
    MaterialStatType.CONTEXT_KEY.requiredField(),
    IRepairableMaterialStats.DURABILITY_FIELD,
    FloatLoadable.FROM_ZERO.defaultField("armor", 0f, true, PlatingMaterialStats::armor),
    FloatLoadable.FROM_ZERO.defaultField("toughness", 0f, PlatingMaterialStats::toughness),
    FloatLoadable.FROM_ZERO.defaultField("knockback_resistance", 0f, PlatingMaterialStats::knockbackResistance),
    PlatingMaterialStats::new);
  private static final List<Component> DESCRIPTION = List.of(
    ToolStats.DURABILITY.getDescription(),
    ToolStats.ARMOR.getDescription(),
    ToolStats.ARMOR_TOUGHNESS.getDescription(),
    ToolStats.KNOCKBACK_RESISTANCE.getDescription());
  /* Types */
  public static final MaterialStatType<PlatingMaterialStats> HELMET = makeType("plating_helmet");
  public static final MaterialStatType<PlatingMaterialStats> CHESTPLATE = makeType("plating_chestplate");
  public static final MaterialStatType<PlatingMaterialStats> LEGGINGS = makeType("plating_leggings");
  public static final MaterialStatType<PlatingMaterialStats> BOOTS = makeType("plating_boots");
  public static final List<MaterialStatType<PlatingMaterialStats>> TYPES = List.of(BOOTS, LEGGINGS, CHESTPLATE, HELMET);

  @Override
  public List<Component> getLocalizedInfo() {
    return List.of(
      ToolStats.DURABILITY.formatValue(this.durability),
      ToolStats.ARMOR.formatValue(this.armor),
      ToolStats.ARMOR_TOUGHNESS.formatValue(this.toughness),
      ToolStats.KNOCKBACK_RESISTANCE.formatValue(this.knockbackResistance)
    );
  }

  @Override
  public List<Component> getLocalizedDescriptions() {
    return DESCRIPTION;
  }

  /** Makes a stat type for the given name */
  private static MaterialStatType<PlatingMaterialStats> makeType(String name) {
    return new MaterialStatType<PlatingMaterialStats>(new MaterialStatsId(TConstruct.MOD_ID, name), type -> new PlatingMaterialStats(type, 1, 0, 0, 0), LOADABLE);
  }


  public static Builder builder() {
    return new Builder();
  }

  /** Builder to create plating material stats for all four pieces */
  @Setter
  @Accessors(fluent = true)
  public static class Builder implements ArmorBuilder<PlatingMaterialStats> {
    private final int[] durability = new int[4];
    private final float[] armor = new float[4];
    private float toughness = 0;
    private float knockbackResistance = 0;

    private Builder() {}

    /** Sets the durability for the piece based on the given factor */
    public Builder durabilityFactor(float maxDamageFactor) {
      for (ArmorSlotType slotType : ArmorSlotType.values()) {
        int index = slotType.getIndex();
        durability[index] = (int)(ArmorSlotType.MAX_DAMAGE_ARRAY[index] * maxDamageFactor);
      }
      return this;
    }

    /** Sets the armor value for each piece */
    public Builder armor(float boots, float leggings, float chestplate, float helmet) {
      armor[ArmorSlotType.BOOTS.getIndex()] = boots;
      armor[ArmorSlotType.LEGGINGS.getIndex()] = leggings;
      armor[ArmorSlotType.CHESTPLATE.getIndex()] = chestplate;
      armor[ArmorSlotType.HELMET.getIndex()] = helmet;
      return this;
    }

    @Override
    public PlatingMaterialStats build(ArmorSlotType slot) {
      int index = slot.getIndex();
      return new PlatingMaterialStats(TYPES.get(index), durability[index], armor[index], toughness, knockbackResistance);
    }
  }
}
