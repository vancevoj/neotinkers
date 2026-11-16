package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.joml.Quaternionf;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.library.client.TinkerRenderTypes;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;
import slimeknights.tconstruct.smeltery.client.render.SmelteryTankRenderer.TankFluids;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HeatingStructureBlockEntityRenderer implements BlockEntityRenderer<HeatingStructureBlockEntity, HeatingStructureBlockEntityRenderer.State> {
  private static final float ITEM_SCALE = 15f/16f;

  private final ItemModelResolver itemModelResolver;
  public HeatingStructureBlockEntityRenderer(Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public AABB getRenderBoundingBox(HeatingStructureBlockEntity smeltery) {
    return smeltery.getRenderBoundingBox();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(HeatingStructureBlockEntity smeltery, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(smeltery, state, partialTicks, cameraPosition, breakProgress);
    state.errorOffset = null;
    state.structureValid = false;
    state.fluids = null;
    state.items.clear();
    Level world = smeltery.getLevel();
    if (world == null) return;
    BlockState blockState = smeltery.getBlockState();
    StructureData structure = smeltery.getStructure();
    boolean structureValid = blockState.getValue(ControllerBlock.IN_STRUCTURE) && structure != null;
    state.structureValid = structureValid;
    BlockPos pos = smeltery.getBlockPos();

    // render erroring block, done whether in the structure or not
    BlockPos errorPos = smeltery.getErrorPos();
    LocalPlayer player = Minecraft.getInstance().player;
    if (errorPos != null && player != null) {
      // either we must be holding the book, or the structure must be erroring and it be within 10 seconds of last update
      boolean highlightError = smeltery.isHighlightError();
      if ((!structureValid && highlightError) || smeltery.showDebugBlockBorder(player)) {
        // distance check, 512 is the squared length of the diagonal of a max size structure
        BlockPos playerPos = player.blockPosition();
        int dx = playerPos.getX() - pos.getX();
        int dz = playerPos.getZ() - pos.getZ();
        if ((dx * dx + dz * dz) < 512) {
          state.errorOffset = errorPos.subtract(pos);
          state.highlightError = highlightError;
        }
      }
    }

    // if no structure, nothing else to do
    if (!structureValid) {
      return;
    }

    // relevant positions
    BlockPos minPos = structure.getMinInside();
    BlockPos maxPos = structure.getMaxInside();
    state.minOffset = minPos.subtract(pos);

    // tank fluids, use minPos for brightness
    state.fluids = SmelteryTankRenderer.extract(smeltery.getTank(), minPos, maxPos, LightCoordsUtil.getLightCoords(world, minPos));

    // items
    int xd = 1 + maxPos.getX() - minPos.getX();
    int zd = 1 + maxPos.getZ() - minPos.getZ();
    int layer = xd * zd;
    state.facing = blockState.getValue(ControllerBlock.FACING);
    MeltingModuleInventory inventory = smeltery.getMeltingInventory();
    int max = Config.CLIENT.maxSmelteryItemQuads.get();
    if (max != 0) {
      int quadsRendered = 0;
      QuadCounter counter = max != -1 ? new QuadCounter() : null;
      PoseStack countPose = counter != null ? new PoseStack() : null;
      int slots = inventory.size();
      for (int i = 0; i < slots; i++) {
        ItemStack stack = ItemUtil.getStack(inventory, i);
        if (!stack.isEmpty()) {
          // calculate position inside the smeltery from slot index
          int height = i / layer;
          int layerIndex = i % layer;
          int offsetX = layerIndex % xd;
          int offsetZ = layerIndex / xd;
          BlockPos itemPos = minPos.offset(offsetX, height, offsetZ);

          ItemStackRenderState itemState = new ItemStackRenderState();
          itemModelResolver.updateForTopItem(itemState, stack, TinkerItemDisplays.MELTER, world, null, 0);
          state.items.add(new SmelteryItem(offsetX, height, offsetZ, LightCoordsUtil.getLightCoords(world, itemPos), itemState));

          // done as quads rather than items as its not that expensive to draw blocks, items are the problem
          if (counter != null) {
            // counts the quads the item would submit, special renderers count as 100 quads as they are more expensive
            counter.reset();
            itemState.submit(countPose, counter, 0, OverlayTexture.NO_OVERLAY, 0);
            quadsRendered += counter.getQuads();
            if (quadsRendered > max) {
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    // render erroring block
    BlockPos errorOffset = state.errorOffset;
    if (errorOffset != null) {
      // color will be yellow if the structure is valid (expanding), red if invalid
      RenderType renderType = state.highlightError ? TinkerRenderTypes.ERROR_BLOCK : RenderTypes.lines();
      int color = ARGB.colorFromFloat(0.5f, 1f, state.structureValid ? 1f : 0f, 0f);
      float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
      matrices.pushPose();
      matrices.translate(errorOffset.getX(), errorOffset.getY(), errorOffset.getZ());
      collector.submitShapeOutline(matrices, Shapes.block(), renderType, color, width, false);
      matrices.popPose();
    }

    // if no structure, nothing else to do
    if (!state.structureValid) {
      return;
    }

    // offset to make rendering min pos relative
    matrices.pushPose();
    BlockPos minOffset = state.minOffset;
    matrices.translate(minOffset.getX(), minOffset.getY(), minOffset.getZ());
    // render tank fluids
    SmelteryTankRenderer.renderFluids(matrices, collector, state.fluids);

    // render items
    Quaternionf itemRotation = Axis.YP.rotationDegrees(-90.0F * (float)state.facing.get2DDataValue());
    for (SmelteryItem item : state.items) {
      // offset to the slot position in the structure, scale, and rotate the item
      matrices.pushPose();
      matrices.translate(item.offsetX() + 0.5f, item.height() + 0.5f, item.offsetZ() + 0.5f);
      matrices.rotate(itemRotation);
      matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
      item.item().submit(matrices, collector, item.light(), OverlayTexture.NO_OVERLAY, 0);
      matrices.popPose();
    }

    matrices.popPose();
  }

  /**
   * Renders even when the controller is off screen, as the structure contents extend past the controller.
   * 26.3 decides this once per renderer instead of per block entity, culling still uses {@link #getRenderBoundingBox(HeatingStructureBlockEntity)}.
   */
  @Override
  public boolean shouldRenderOffScreen() {
    return true;
  }

  /**
   * Item to render inside the structure
   * @param offsetX  X offset from the min position
   * @param height   Y offset from the min position
   * @param offsetZ  Z offset from the min position
   * @param light    Light at the item position
   * @param item     Resolved item
   */
  private record SmelteryItem(int offsetX, int height, int offsetZ, int light, ItemStackRenderState item) {}

  /** Render state for smelteries and foundries */
  public static class State extends BlockEntityRenderState {
    /** Offset of the error position from the controller, null if no error to render */
    @Nullable
    BlockPos errorOffset;
    /** If true, the error is rendered through blocks */
    boolean highlightError;
    /** If true, the structure is formed */
    boolean structureValid;
    /** Offset of the min inside position from the controller */
    BlockPos minOffset = BlockPos.ZERO;
    /** Controller facing, used to rotate items */
    Direction facing = Direction.NORTH;
    /** Tank fluids, null if nothing to render */
    @Nullable
    TankFluids fluids;
    /** Items to render */
    final List<SmelteryItem> items = new ArrayList<>();
  }

  /**
   * Submit node collector that discards everything, counting the item quads submitted to it.
   * Used to enforce {@link Config.Client#maxSmelteryItemQuads} during extraction.
   */
  private static class QuadCounter implements SubmitNodeCollector {
    /** Number of quads submitted since the last reset */
    private int quads = 0;
    /** If true, a special renderer submitted something */
    private boolean special = false;

    /** Resets the counter */
    public void reset() {
      quads = 0;
      special = false;
    }

    /** Gets the number of quads, special renderers count as 100 */
    public int getQuads() {
      return special ? quads + 100 : quads;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
      return this;
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, ItemQuads quads, ItemStackRenderState.FoilType foilType) {
      this.quads += quads.all().size();
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor) {
      special = true;
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
      special = true;
    }

    @Override
    public void submitMultiLayerBlockModel(PoseStack poseStack, List<BlockStateModelPart> modelParts, boolean translucent, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
      special = true;
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
      special = true;
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
      special = true;
    }

    @Override
    public <T extends SubmitNode, S extends T> void submitSpecial(RenderPhaseKey<T> phaseKey, S submitNode) {
      special = true;
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera) {}

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {}

    @Override
    public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {}

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {}

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {}

    @Override
    public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {}

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {}

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {}

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {}

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {}
  }
}
