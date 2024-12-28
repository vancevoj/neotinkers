package slimeknights.tconstruct.smeltery.client.render;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.data.datamap.RegistryDataMapLoader;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.field.MergingField;
import slimeknights.tconstruct.library.json.field.MergingField.MissingMode;

/** Data class for rendering the fluids in a casting channel */
public record ChannelFluids(
  FluidCuboid down, FluidCuboid centerStill, FluidCuboid centerFlowing,
  FluidCuboid sideStill, FluidCuboid sideEdge, FluidCuboid sideIn, FluidCuboid sideOut) {
  public static final RecordLoadable<ChannelFluids> LOADABLE = RecordLoadable.create(
    FluidCuboid.LOADABLE.requiredField("down", ChannelFluids::down),
    new MergingField<>(FluidCuboid.LOADABLE.requiredField("still",   ChannelFluids::down), "center", MissingMode.DISALLOWED),
    new MergingField<>(FluidCuboid.LOADABLE.requiredField("flowing", ChannelFluids::down), "center", MissingMode.DISALLOWED),
    new MergingField<>(FluidCuboid.LOADABLE.requiredField("still", ChannelFluids::down), "side", MissingMode.DISALLOWED),
    new MergingField<>(FluidCuboid.LOADABLE.requiredField("in",    ChannelFluids::down), "side", MissingMode.DISALLOWED),
    new MergingField<>(FluidCuboid.LOADABLE.requiredField("out",   ChannelFluids::down), "side", MissingMode.DISALLOWED),
    new MergingField<>(FluidCuboid.LOADABLE.requiredField("edge",  ChannelFluids::down), "side", MissingMode.DISALLOWED),
    ChannelFluids::new);
  /** Registry for loading channel fluids */
  @SuppressWarnings("deprecation")
  public static final RegistryDataMapLoader<Block,ChannelFluids> REGISTRY = new RegistryDataMapLoader<>("Channel fluids", "tinkering/channel_fluids", BuiltInRegistries.BLOCK, LOADABLE);

  public FluidCuboid center(boolean flowing) {
    return flowing ? centerStill : centerFlowing;
  }

  public FluidCuboid sideFlow(boolean out) {
    return out ? sideOut : sideIn;
  }
}
