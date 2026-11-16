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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.RenderUtils.FluidTankInfo;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity.ITankInventoryBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TankInventoryBlockEntityRenderer<T extends BlockEntity & ITankInventoryBlockEntity> implements BlockEntityRenderer<T, TankInventoryBlockEntityRenderer.State> {
  private final EnumProperty<Direction> directionProperty;
  private final ItemModelResolver itemModelResolver;
  public TankInventoryBlockEntityRenderer(EnumProperty<Direction> directionProperty, Context context) {
    this.directionProperty = directionProperty;
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T melter, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(melter, state, partialTicks, cameraPosition, breakProgress);
    BlockState blockState = melter.getBlockState();
    state.fluids = Config.CLIENT.tankFluidModel.get() ? List.of() : List.copyOf(FluidCuboid.REGISTRY.get(blockState, List.of()));
    state.renderItems = RenderItem.STATE_REGISTRY.get(blockState, List.of());
    state.tank = null;
    state.items.clear();
    if (!state.fluids.isEmpty() || !state.renderItems.isEmpty()) {
      state.facing = blockState.getValue(directionProperty);

      // fluids, updates the animated render offset of the tank
      if (!state.fluids.isEmpty()) {
        state.tank = FluidTankInfo.of(melter.getTank(), partialTicks, state.lightCoords, true);
      }

      // items
      // TODO: can we show count somehow?
      ResourceHandler<ItemResource> inventory = melter.getItemHandler();
      int size = inventory.size();
      for (int i = 0; i < state.renderItems.size(); i++) {
        ItemStackRenderState itemState = new ItemStackRenderState();
        if (i < size) {
          RenderingHelper.extractItem(itemModelResolver, itemState, ItemUtil.getStack(inventory, i), state.renderItems.get(i), melter.getLevel(), 0);
        }
        state.items.add(itemState);
      }
    }
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    List<FluidCuboid> fluids = state.fluids;
    List<RenderItem> renderItems = state.renderItems;
    if (!fluids.isEmpty() || !renderItems.isEmpty()) {
      // rotate the matrix
      boolean isRotated = RenderingHelper.applyRotation(matrices, state.facing);

      // render fluids
      if (state.tank != null) {
        state.tank.submit(matrices, collector, fluids);
      }

      // render items
      for (int i = 0; i < renderItems.size() && i < state.items.size(); i++) {
        RenderingHelper.renderItem(matrices, collector, state.items.get(i), renderItems.get(i), state.lightCoords);
      }

      // pop back rotation
      if (isRotated) {
        matrices.popPose();
      }
    }
  }

  /** Render state for tanks with an inventory */
  public static class State extends BlockEntityRenderState {
    /** Direction the block is facing */
    Direction facing = Direction.SOUTH;
    /** Fluid display locations */
    List<FluidCuboid> fluids = List.of();
    /** Item display locations */
    List<RenderItem> renderItems = List.of();
    /** Tank contents, null if nothing to render */
    @Nullable
    FluidTankInfo tank;
    /** Resolved items, one per render item */
    final List<ItemStackRenderState> items = new ArrayList<>();
  }
}
