package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.ChannelFluids;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.smeltery.block.ChannelBlock;
import slimeknights.tconstruct.smeltery.block.ChannelBlock.ChannelConnection;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;
import slimeknights.tconstruct.smeltery.client.render.FaucetBlockEntityRenderer.FaucetSegment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ChannelBlockEntityRenderer implements BlockEntityRenderer<ChannelBlockEntity, ChannelBlockEntityRenderer.State> {
  public ChannelBlockEntityRenderer(Context context) {}

	@Override
	public AABB getRenderBoundingBox(ChannelBlockEntity te) {
		BlockPos pos = te.getBlockPos();
		return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ChannelBlockEntity te, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(te, state, partialTicks, cameraPosition, breakProgress);
		state.sprites = null;
		FluidStack fluid = te.getFluid();
		if (fluid.isEmpty()) {
			return;
		}

		// fetch model properties
		Level world = te.getLevel();
		if (world == null) {
			return;
		}
		BlockPos pos = te.getBlockPos();
		BlockState blockState = te.getBlockState();
		ChannelFluids model = ChannelFluids.REGISTRY.get(blockState.getBlock());
		if (model == null) {
			return;
		}

		// fluid attributes
		state.sprites = FluidSprites.of(fluid);
		state.fluidLight = FluidRenderer.getFluidLight(state.lightCoords, fluid);

		// sides first, while doing so we will determine center "flow"
		List<SideFluid> sides = new ArrayList<>(4);
		FluidCuboid cube;
		Direction centerFlow = Direction.UP;
		for (Direction direction : Plane.HORIZONTAL) {
			// check if we have that side on the block
			ChannelConnection connection = blockState.getValue(ChannelBlock.DIRECTION_MAP.get(direction));
			if (connection.canFlow()) {
				// get the relevant fluid model
				FluidCuboid edge = null;
				if (te.isFlowing(direction)) {
					cube = model.side().flow(connection == ChannelConnection.OUT);

					// add to center direction
					if (connection == ChannelConnection.OUT) {
						// if unset (up), use this direction
						if (centerFlow == Direction.UP) {
							centerFlow = direction;
							// if set and it disagrees, set the fail state (down)
						} else if (centerFlow != direction) {
							centerFlow = Direction.DOWN;
						}
					}
					// render the extra edge against other blocks
					if (!world.getBlockState(pos.relative(direction)).is(blockState.getBlock())) {
						edge = model.side().edge();
					}
				} else {
					cube = model.side().still();
				}
				sides.add(new SideFluid(direction, cube, edge));
			}
		}
		state.sides = List.copyOf(sides);

		// center
		if (centerFlow.getAxis().isVertical()) {
			state.center = model.center(false);
			state.centerFlow = Direction.SOUTH; // no rotation
		} else {
			state.center = model.center(true);
			state.centerFlow = centerFlow;
		}

		// flow downwards
		if (blockState.getValue(ChannelBlock.DOWN) && te.isFlowing(Direction.DOWN)) {
			state.down = model.down();
			// fluids in the block(s) below
			state.below = FaucetBlockEntityRenderer.getFaucetFluids(world, pos, Direction.DOWN);
		} else {
			state.down = null;
			state.below = List.of();
		}
	}

	/**
	 * Applies horizontal rotation to a pose, same as {@link slimeknights.mantle.client.render.RenderingHelper#applyRotation(PoseStack, Direction)}
	 * @param pose    Pose to rotate
	 * @param facing  Direction of rotation
	 * @return  Rotated copy of the pose, or the pose if no rotation is needed
	 */
	private static PoseStack.Pose rotate(PoseStack.Pose pose, Direction facing) {
		// south has a facing of 0, no rotation needed
		if (facing.getAxis().isHorizontal() && facing != Direction.SOUTH) {
			PoseStack.Pose rotated = pose.copy();
			rotated.translate(0.5f, 0, 0.5f);
			rotated.rotate(Axis.YP.rotationDegrees(-90f * (facing.get2DDataValue())));
			rotated.translate(-0.5f, 0, -0.5f);
			return rotated;
		}
		return pose;
	}

	@Override
	public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera)  {
		FluidSprites sprites = state.sprites;
		if (sprites == null) {
			return;
		}
		List<SideFluid> sides = state.sides;
		FluidCuboid center = state.center;
		Direction centerFlow = state.centerFlow;
		FluidCuboid down = state.down;
		List<FaucetSegment> below = state.below;
		int light = state.fluidLight;
		collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> {
			// render sides first
			for (SideFluid side : sides) {
				// apply rotation for the side
				PoseStack.Pose sidePose = rotate(pose, side.direction());
				if (side.edge() != null) {
					renderCuboid(sidePose, buffer, side.edge(), sprites, light);
				}
				renderCuboid(sidePose, buffer, side.cube(), sprites, light);
			}

			// render center
			if (center != null) {
				renderCuboid(rotate(pose, centerFlow), buffer, center, sprites, light);
			}

			// render flow downwards
			if (down != null) {
				renderCuboid(pose, buffer, down, sprites, light);
				// render into the block(s) below
				FaucetBlockEntityRenderer.renderFaucetFluids(pose, buffer, below, sprites, light);
			}
		});
	}

	/** Renders a single cube at its own bounds */
	private static void renderCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, FluidSprites sprites, int light) {
		FluidRenderer.renderCuboid(pose, buffer, cube, sprites, light);
	}

	/**
	 * Fluid on a single side of the channel
	 * @param direction  Side direction
	 * @param cube       Fluid on the side
	 * @param edge       Extra edge fluid against other blocks, null if not needed
	 */
	private record SideFluid(Direction direction, FluidCuboid cube, @Nullable FluidCuboid edge) {}

	/** Render state for channels */
	public static class State extends BlockEntityRenderState {
		/** Fluid sprites and color, null if nothing to render */
		@Nullable
		FluidSprites sprites;
		/** Light including the fluid light */
		int fluidLight;
		/** Fluids on the sides */
		List<SideFluid> sides = List.of();
		/** Fluid in the center */
		@Nullable
		FluidCuboid center;
		/** Rotation for the center fluid */
		Direction centerFlow = Direction.SOUTH;
		/** Fluid flowing down, null if not flowing down */
		@Nullable
		FluidCuboid down;
		/** Fluids in the blocks below */
		List<FaucetSegment> below = List.of();
	}
}
