package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.RenderUtils.FluidTankInfo;
import slimeknights.tconstruct.smeltery.block.entity.ProxyTankBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/** Renderer for {@link ProxyTankBlockEntity}. Unlike {@link TankInventoryBlockEntityRenderer}, does not use a {@link slimeknights.tconstruct.library.fluid.FluidTankAnimated} */
public class ProxyTankBlockEntityRenderer implements BlockEntityRenderer<ProxyTankBlockEntity, ProxyTankBlockEntityRenderer.State> {
  private final ItemModelResolver itemModelResolver;
  public ProxyTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(ProxyTankBlockEntity proxyTank, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(proxyTank, state, partialTicks, cameraPosition, breakProgress);
    BlockState blockState = proxyTank.getBlockState();
    state.fluids = Config.CLIENT.tankFluidModel.get() ? List.of() : List.copyOf(FluidCuboid.REGISTRY.get(blockState, List.of()));
    state.renderItems = RenderItem.STATE_REGISTRY.get(blockState, List.of());
    state.tank = null;
    state.items.clear();
    if (!state.fluids.isEmpty() || !state.renderItems.isEmpty()) {
      state.facing = blockState.getValue(HORIZONTAL_FACING);

      // the proxy tank holds a single item, the fluid comes from that item's fluid handler
      ItemStack stack = proxyTank.getItemTank().getStack();

      // fluids
      if (!state.fluids.isEmpty() && !stack.isEmpty()) {
        ResourceHandler<FluidResource> itemTank = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        if (itemTank != null && itemTank.size() > 0) {
          FluidResource resource = itemTank.getResource(0);
          if (!resource.isEmpty()) {
            FluidStack fluid = resource.toStack(itemTank.getAmountAsInt(0));
            state.tank = FluidTankInfo.of(fluid, itemTank.getCapacityAsInt(0, resource), 0, state.lightCoords, true);
          }
        }
      }

      // items
      for (int i = 0; i < state.renderItems.size(); i++) {
        ItemStackRenderState itemState = new ItemStackRenderState();
        if (i == 0) {
          RenderingHelper.extractItem(itemModelResolver, itemState, stack, state.renderItems.get(i), proxyTank.getLevel(), 0);
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

  /** Render state for proxy tanks */
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
