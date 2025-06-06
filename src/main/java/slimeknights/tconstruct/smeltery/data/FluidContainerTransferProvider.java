package slimeknights.tconstruct.smeltery.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.fluid.transfer.AbstractFluidContainerTransferProvider;
import slimeknights.mantle.fluid.transfer.FillFluidContainerTransfer;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.shared.block.SlimeType;

public class FluidContainerTransferProvider extends AbstractFluidContainerTransferProvider {
  public FluidContainerTransferProvider(PackOutput packOutput) {
    super(packOutput, TConstruct.MOD_ID);
  }

  @Override
  protected void addTransfers() {
    addFillEmpty("meat_soup_", TinkerFluids.meatSoupBowl, Items.BOWL, TinkerFluids.meatSoup, FluidValues.BOWL, false);
    // these bottles are fluid handlers, but glass bottles are not
    addBottleFill("venom_bottle_fill", TinkerFluids.venomBottle, TinkerFluids.venom);
    addBottleFill("earth_slime_bottle_fill", TinkerFluids.slimeBottle.get(SlimeType.EARTH), TinkerFluids.earthSlime);
    addBottleFill("sky_slime_bottle_fill",   TinkerFluids.slimeBottle.get(SlimeType.SKY),   TinkerFluids.skySlime);
    addBottleFill("ichor_slime_bottle_fill", TinkerFluids.slimeBottle.get(SlimeType.ICHOR), TinkerFluids.ichor);
    addBottleFill("ender_slime_bottle_fill", TinkerFluids.slimeBottle.get(SlimeType.ENDER), TinkerFluids.enderSlime);
    addBottleFill("magma_bottle_fill",       TinkerFluids.magmaBottle,                      TinkerFluids.magma);
  }

  /** Adds a recipe for a bottle that fills with 250mb of fluid, emptying is assumed handled */
  protected void addBottleFill(String name, ItemLike output, FluidObject<?> fluid) {
    addTransfer(name, new FillFluidContainerTransfer(Ingredient.of(Items.GLASS_BOTTLE), ItemOutput.fromItem(output), fluid.ingredient(FluidValues.BOTTLE)));
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Fluid Container Transfer";
  }
}
