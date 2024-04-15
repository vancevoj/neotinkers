package slimeknights.tconstruct.library.tools.definition;

import lombok.Data;
import lombok.Getter;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import slimeknights.mantle.data.loadable.IAmLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.mapping.EitherLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import javax.annotation.Nullable;

/** Element that contains data about a single tool part */
@Data
public abstract class PartRequirement implements IAmLoadable.Record {
  /* Loadables */
  private static final LoadableField<Integer,PartRequirement> WEIGHT_FIELD = IntLoadable.FROM_ONE.defaultField("weight", 1, r -> r.weight);
  private static final RecordLoadable<ToolPart> TOOL_PART = RecordLoadable.create(TinkerLoadables.TOOL_PART_ITEM.requiredField("item", r -> r.part), WEIGHT_FIELD, PartRequirement::ofPart);
  private static final RecordLoadable<StatType> STAT_TYPE = RecordLoadable.create(MaterialStatsId.PARSER.requiredField("stat", r -> r.statType), WEIGHT_FIELD, PartRequirement::ofStat);
  public static final RecordLoadable<PartRequirement> LOADABLE = EitherLoadable.<PartRequirement>record().key("item", TOOL_PART).key("stat", STAT_TYPE).build();

  /** Creates a new part requirement for a part */
  public static ToolPart ofPart(IToolPart part, int weight) {
    return new ToolPart(part, weight);
  }

  /** Creates a new part requirement for a stat type */
  public static StatType ofStat(MaterialStatsId statsId, int weight) {
    return new StatType(statsId, weight);
  }

  /** Weight of this part for the stat builder */
  private final int weight;

  /** Gets the part for this requirement (if present) */
  @Nullable
  public abstract IToolPart getPart();

  /** If true, this part requirement matches the given item */
  public abstract boolean matches(Item item);

  /** If true, this requirement can use the given material */
  public abstract boolean canUseMaterial(MaterialVariantId material);

  /** Gets the name of this part for the given material */
  public abstract Component nameForMaterial(MaterialVariantId material);

  /** Gets the stat type for this part */
  public abstract MaterialStatsId getStatType();


  /** Implementation that contains a tool part */
  private static class ToolPart extends PartRequirement {
    @Getter
    private final IToolPart part;
    public ToolPart(IToolPart part, int weight) {
      super(weight);
      this.part = part;
    }

    @Override
    public boolean matches(Item item) {
      return part.asItem() == item;
    }

    @Override
    public boolean canUseMaterial(MaterialVariantId material) {
      return part.canUseMaterial(material.getId());
    }

    @Override
    public Component nameForMaterial(MaterialVariantId material) {
      return part.withMaterial(material).getHoverName();
    }

    @Override
    public MaterialStatsId getStatType() {
      return part.getStatType();
    }

    @Override
    public RecordLoadable<?> loadable() {
      return TOOL_PART;
    }

    @Override
    public String toString() {
      return "PartRequirement.ToolPart{" + Registry.ITEM.getKey(part.asItem()) + '*' + getWeight() + '}';
    }
  }

  /** Implementation specifying a stat type with no part */
  private static class StatType extends PartRequirement {
    @Getter
    private final MaterialStatsId statType;
    public StatType(MaterialStatsId statType, int weight) {
      super(weight);
      this.statType = statType;
    }

    @Nullable
    @Override
    public IToolPart getPart() {
      return null;
    }

    @Override
    public boolean matches(Item item) {
      return false;
    }

    @Override
    public boolean canUseMaterial(MaterialVariantId material) {
      return MaterialRegistry.getInstance().getMaterialStats(material.getId(), statType).isPresent();
    }

    @Override
    public Component nameForMaterial(MaterialVariantId material) {
      return MaterialTooltipCache.getDisplayName(material);
    }

    @Override
    public RecordLoadable<?> loadable() {
      return STAT_TYPE;
    }


    @Override
    public String toString() {
      return "PartRequirement.StatType{" + statType + '*' + getWeight() + '}';
    }
  }
}
