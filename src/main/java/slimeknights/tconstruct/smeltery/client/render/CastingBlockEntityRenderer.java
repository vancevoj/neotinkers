package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.client.RenderUtils.FluidTankInfo;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.CastingFluidHandler;
import slimeknights.tconstruct.smeltery.client.util.CastingItemRenderTypeBuffer;

import javax.annotation.Nullable;
import java.util.List;

public class CastingBlockEntityRenderer implements BlockEntityRenderer<CastingBlockEntity, CastingBlockEntityRenderer.State> {
  private final ItemModelResolver itemModelResolver;
  public CastingBlockEntityRenderer(Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(CastingBlockEntity casting, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(casting, state, partialTicks, cameraPosition, breakProgress);
    BlockState blockState = casting.getBlockState();
    state.blockState = blockState;
    state.fluids = List.copyOf(FluidCuboid.REGISTRY.get(blockState, List.of()));
    state.renderItems = RenderItem.STATE_REGISTRY.get(blockState, List.of());
    state.fluidSprites = null;
    state.tankInfo = null;
    state.coolingOutput = false;
    state.input.clear();
    state.output.clear();
    if (state.fluids.isEmpty() && state.renderItems.isEmpty()) {
      return;
    }

    // if the recipe is in progress, start fading the item away
    int timer = casting.getTimer();
    int totalTime = casting.getCoolingTime();
    int itemOpacity = 0;
    int fluidOpacity = 0xFF;
    if (timer > 0 && totalTime > 0) {
      int opacity = (4 * 0xFF) * timer / totalTime;
      // fade item in
      itemOpacity = opacity / 4;

      // fade fluid and temperature out during last 10%
      if (opacity > 3 * 0xFF) {
        fluidOpacity = (4 * 0xFF) - opacity;
      }
    }
    state.itemOpacity = itemOpacity;
    state.fluidOpacity = fluidOpacity;

    // fluids
    if (!state.fluids.isEmpty()) {
      CastingFluidHandler tank = casting.getTank();
      FluidStack fluidStack = tank.getFluid();
      if (!fluidStack.isEmpty()) {
        int capacity = tank.getCapacity();
        // if full, start rendering with opacity for progress
        if (fluidStack.getAmount() == capacity) {
          state.fluidSprites = FluidSprites.of(fluidStack);
          state.fluidLight = FluidRenderer.getFluidLight(state.lightCoords, fluidStack);
        } else {
          // not strictly useful to scale the fluids down, but who knows what the modeler does
          state.tankInfo = FluidTankInfo.of(fluidStack, capacity, 0, state.lightCoords, false);
        }
      }
    }

    // items
    if (!state.renderItems.isEmpty()) {
      // input is normal
      RenderingHelper.extractItem(itemModelResolver, state.input, casting.getItem(0), state.renderItems.get(0), casting.getLevel(), 0);

      // output may be the recipe output instead of the current item
      if (state.renderItems.size() >= 2) {
        RenderItem outputModel = state.renderItems.get(1);
        if (!outputModel.isHidden()) {
          // get output stack
          ItemStack output = casting.getItem(1);
          if (itemOpacity > 0 && output.isEmpty()) {
            output = casting.getRecipeOutput();
            // tint and add opacity while cooling
            state.coolingOutput = true;
          }
          RenderingHelper.extractItem(itemModelResolver, state.output, output, outputModel, casting.getLevel(), 0);
        }
      }
    }
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    List<FluidCuboid> fluids = state.fluids;
    List<RenderItem> renderItems = state.renderItems;

    // rotate the matrix
    if (!fluids.isEmpty() || !renderItems.isEmpty()) {
      boolean isRotated = RenderingHelper.applyRotation(matrices, state.blockState);

      // render fluids
      if (!fluids.isEmpty()) {
        FluidSprites sprites = state.fluidSprites;
        if (sprites != null) {
          int fluidOpacity = state.fluidOpacity;
          int fluidLight = state.fluidLight;
          collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> {
            for (FluidCuboid fluid : fluids) {
              RenderUtils.renderTransparentCuboid(pose, buffer, fluid, sprites, fluidOpacity, fluidLight);
            }
          });
        } else if (state.tankInfo != null) {
          state.tankInfo.submit(matrices, collector, fluids);
        }
      }

      // render renderItems
      if (!renderItems.isEmpty()) {
        // input is normal
        RenderingHelper.renderItem(matrices, collector, state.input, renderItems.get(0), state.lightCoords);

        // output may be the recipe output instead of the current item
        if (renderItems.size() >= 2) {
          // apply a collector wrapper to tint and add opacity
          SubmitNodeCollector outputCollector = state.coolingOutput ? new CastingItemRenderTypeBuffer(collector, state.itemOpacity, state.fluidOpacity) : collector;
          RenderingHelper.renderItem(matrices, outputCollector, state.output, renderItems.get(1), state.lightCoords);
        }
      }

      // pop back rotation
      if (isRotated) {
        matrices.popPose();
      }
    }
  }

  /** Render state for casting tables and basins */
  public static class State extends BlockEntityRenderState {
    /** Block state of the casting block */
    BlockState blockState = Blocks.AIR.defaultBlockState();
    /** Fluid display locations */
    List<FluidCuboid> fluids = List.of();
    /** Item display locations */
    List<RenderItem> renderItems = List.of();
    /** Sprites for a full tank, rendered with {@link #fluidOpacity} */
    @Nullable
    FluidSprites fluidSprites;
    /** Light for a full tank */
    int fluidLight;
    /** Tank data for a partially full tank */
    @Nullable
    FluidTankInfo tankInfo;
    /** Opacity of the cooling item and fluid */
    int itemOpacity, fluidOpacity;
    /** If true, the output is the cooling recipe output */
    boolean coolingOutput;
    /** Resolved input item */
    final ItemStackRenderState input = new ItemStackRenderState();
    /** Resolved output item */
    final ItemStackRenderState output = new ItemStackRenderState();
  }
}
