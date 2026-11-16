package slimeknights.tconstruct.fluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.fluid.texture.ClientInvertedFluidType;
import slimeknights.mantle.fluid.texture.ClientTextureFluidType;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.model.FluidContainerModel;

@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class FluidClientEvents extends ClientEventBase {
  /**
   * Registers fluid models. Mantle registers models for its own fluid types at the lowest priority and skips fluids that already have one,
   * so the models here replace those for the listed fluids.
   */
  @SubscribeEvent
  static void registerFluidModels(RegisterFluidModelsEvent event) {
    // the fluid render layer comes from the fluid model since 26.1, so force the translucent layer through the materials
    registerTranslucent(event, TinkerFluids.honey);
    // slime
    registerTranslucent(event, TinkerFluids.earthSlime);
    registerTranslucent(event, TinkerFluids.skySlime);
    registerTranslucent(event, TinkerFluids.enderSlime);
    // molten
    registerTranslucent(event, TinkerFluids.moltenDiamond);
    registerTranslucent(event, TinkerFluids.moltenEmerald);
    registerTranslucent(event, TinkerFluids.moltenGlass);
    registerTranslucent(event, TinkerFluids.liquidSoul);
    registerTranslucent(event, TinkerFluids.moltenSoulsteel);
    registerTranslucent(event, TinkerFluids.moltenAmethyst);

    // potion is not a Mantle fluid type, so Mantle does not register it; tint it from the potion contents
    FluidType potion = TinkerFluids.potion.getType();
    event.register(FluidTextureManager.createModel(TinkerFluids.potion.get(), new PotionTint(potion)), TinkerFluids.potion.get());
  }

  /**
   * Registers client fluid extensions, used for the camera overlay and fog, as {@code initializeClient} no longer exists on fluid types.
   * Mantle handles the remaining Tinkers fluids at the lowest priority.
   */
  @SubscribeEvent
  static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    FluidType potion = TinkerFluids.potion.getType();
    event.registerFluidType(new ClientTextureFluidType(potion), potion);
    // ichor uses a slime fluid type, which Mantle would give the non-inverted extension
    FluidType ichor = TinkerFluids.ichor.getType();
    event.registerFluidType(new ClientInvertedFluidType(ichor), ichor);
  }

  @SubscribeEvent
  static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
    event.register(TConstruct.getResource("fluid_container"), FluidContainerModel.LOADER);
  }

  @SubscribeEvent
  static void registerItemModels(RegisterItemModelsEvent event) {
    event.register(FluidContainerModel.Unbaked.ID, FluidContainerModel.Unbaked.MAP_CODEC);
  }

  /** Registers a fluid model for the given fluid that renders in the translucent layer */
  private static void registerTranslucent(RegisterFluidModelsEvent event, FlowingFluidObject<?> fluid) {
    // inverted fluids get their own flowing texture, so create the model per fluid like Mantle
    event.register(translucent(FluidTextureManager.createModel(fluid.getStill())), fluid.getStill());
    event.register(translucent(FluidTextureManager.createModel(fluid.getFlowing())), fluid.getFlowing());
  }

  /** Copies the given model, forcing all materials to be translucent */
  private static FluidModel.Unbaked translucent(FluidModel.Unbaked model) {
    Material overlay = model.overlayMaterial();
    return new FluidModel.Unbaked(
      model.stillMaterial().withForceTranslucent(true),
      model.flowingMaterial().withForceTranslucent(true),
      overlay == null ? null : overlay.withForceTranslucent(true),
      model.fluidTintSource(), model.customRenderer());
  }

  /**
   * Tint for the potion fluid, using the color from {@link PotionContents#getColor()} on fluid stacks.
   * @param type  Potion fluid type, used for the fallback color from its fluid texture
   */
  private record PotionTint(FluidType type) implements FluidTintSource {
    @Override
    public int color(FluidState state) {
      return FluidTextureManager.getColor(type);
    }

    @Override
    public int colorAsStack(FluidStack stack) {
      PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
      if (contents == null || contents == PotionContents.EMPTY) {
        return color(stack.getFluid().defaultFluidState());
      }
      if (contents.customColor().isPresent()) {
        return contents.customColor().get() | 0xFF000000;
      }
      return contents.getColor() | 0xFF000000;
    }
  }
}
