package slimeknights.tconstruct.library.tools.definition.module.material;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.field.OptionallyNestedLoadable;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import java.util.List;
import java.util.function.Supplier;

/** Tool using tool parts for its material stats, allows part swapping and tool building */
public class PartStatsModule extends MaterialStatsModule implements ToolPartsHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<PartStatsModule>defaultHooks(ToolHooks.TOOL_STATS, ToolHooks.TOOL_TRAITS, ToolHooks.TOOL_MATERIALS, ToolHooks.TOOL_PARTS, ToolHooks.MATERIAL_REPAIR);
  public static final RecordLoadable<PartStatsModule> LOADER = RecordLoadable.create(
    new OptionallyNestedLoadable<>(TinkerLoadables.TOOL_PART_ITEM, "item").list().requiredField("parts", m -> m.parts),
    new StatScaleField("item", "parts"),
    PartStatsModule::new);

  private final List<IToolPart> parts;
  public PartStatsModule(List<IToolPart> parts, float[] scales) {
    super(parts.stream().map(IToolPart::getStatType).toList(), scales);
    this.parts = parts;
  }

  @Override
  public RecordLoadable<PartStatsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public List<IToolPart> getParts(ToolDefinition definition) {
    return parts;
  }


  /* Builder */

  public static Builder parts() {
    return new Builder();
  }

  /** Starts a builder for armor stats */
  public static ArmorBuilder armor(List<ArmorSlotType> slots) {
    return new ArmorBuilder(slots);
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final ImmutableList.Builder<IToolPart> parts = ImmutableList.builder();
    private final ImmutableList.Builder<Float> scales = ImmutableList.builder();

    /** Adds a part to the builder */
    public Builder part(IToolPart part, float scale) {
      parts.add(part);
      scales.add(scale);
      return this;
    }

    /** Adds a part to the builder */
    public Builder part(Supplier<? extends IToolPart> part, float scale) {
      return part(part.get(), scale);
    }

    /** Adds a part to the builder */
    public Builder part(IToolPart part) {
      return part(part, 1);
    }

    /** Adds a part to the builder */
    public Builder part(Supplier<? extends IToolPart> part) {
      return part(part, 1);
    }

    /** Builds the module */
    public PartStatsModule build() {
      return new PartStatsModule(parts.build(), MaterialStatsModule.Builder.buildScales(scales.build()));
    }
  }

  /** Builder for armor */
  public static class ArmorBuilder implements ArmorSlotType.ArmorBuilder<PartStatsModule> {
    private final List<ArmorSlotType> slotTypes;
    private final Builder[] builders = new Builder[4];

    private ArmorBuilder(List<ArmorSlotType> slotTypes) {
      this.slotTypes = slotTypes;
      for (ArmorSlotType slotType : slotTypes) {
        builders[slotType.getIndex()] = new Builder();
      }
    }

    /** Gets the builder for the given slot */
    protected Builder getBuilder(ArmorSlotType slotType) {
      Builder builder = builders[slotType.getIndex()];
      if (builder == null) {
        throw new IllegalArgumentException("Unsupported slot type " + slotType);
      }
      return builder;
    }

    /** Adds a part to the given slot */
    public ArmorBuilder part(ArmorSlotType slotType, IToolPart part, float scale) {
      getBuilder(slotType).part(part, scale);
      return this;
    }

    /** Adds a part to all slots */
    public ArmorBuilder part(IToolPart part, float scale) {
      for (ArmorSlotType slotType : slotTypes) {
        getBuilder(slotType).part(part, scale);
      }
      return this;
    }

    /** Adds a part to all slots */
    public ArmorBuilder part(Supplier<? extends IToolPart> part, float scale) {
      return part(part.get(), scale);
    }

    /** Adds parts to the builder from the passed object */
    public ArmorBuilder part(EnumObject<ArmorSlotType, ? extends IToolPart> parts, float scale) {
      for (ArmorSlotType slotType : slotTypes) {
        getBuilder(slotType).part(parts.get(slotType), scale);
      }
      return this;
    }


    @Override
    public PartStatsModule build(ArmorSlotType slot) {
      return getBuilder(slot).build();
    }
  }
}
