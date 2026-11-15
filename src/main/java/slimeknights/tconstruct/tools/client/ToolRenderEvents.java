package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator.AOEMatchType;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.BlockSideHitListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Renders block outlines and block breaking progress on the extra blocks of area of effect tools.
 * <p>
 * 26.3: both are collected during level render state extraction. The outlines use a custom block outline renderer, the breaking
 * progress adds entries to the vanilla block breaking render states.
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class ToolRenderEvents {
  /** Maximum number of blocks from the iterator to render */
  private static final int MAX_BLOCKS = 60;

  /**
   * Outline to render on an extra block
   * @param pos            Block position
   * @param shape          Outline shape
   * @param isTranslucent  If true, the block is translucent, so the outline renders after translucent terrain
   */
  private record ExtraOutline(BlockPos pos, VoxelShape shape, boolean isTranslucent) {}

  /**
   * Renders the outline on the extra blocks
   *
   * @param event the outline extraction event
   */
  @SubscribeEvent
  static void renderBlockHighlights(ExtractBlockOutlineRenderStateEvent event) {
    Minecraft mc = Minecraft.getInstance();
    ClientLevel world = event.getLevel();
    Player player = mc.player;
    if (player == null) {
      return;
    }
    // must have the right tags
    ItemStack stack = player.getMainHandItem();
    if (stack.isEmpty() || !stack.is(TinkerTags.Items.MODIFIABLE)) {
      return;
    }
    // must be targeting a block
    HitResult result = mc.hitResult;
    if (result == null || result.getType() != Type.BLOCK) {
      return;
    }
    // must not be broken, must be right interface
    ToolStack tool = ToolStack.from(stack);
    if (tool.isBroken()) {
      return;
    }
    BlockHitResult blockTrace = event.getHitResult();
    BlockPos origin = blockTrace.getBlockPos();
    BlockState state = world.getBlockState(origin);
    AOEMatchType matchType = AOEMatchType.BREAKING;
    // if we have any modifier that has an AOE interaction, make our match type more liberal
    if (tool.getModifiers().has(TinkerTags.Modifiers.AOE_INTERACTION)) {
      matchType = AOEMatchType.DISPLAY;
    } else if (!IsEffectiveToolHook.isEffective(tool, state)) {
      return;
    }
    UseOnContext context = new UseOnContext(world, player, InteractionHand.MAIN_HAND, stack, blockTrace);
    Iterator<BlockPos> extraBlocks = tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, context, state, matchType).iterator();
    if (!extraBlocks.hasNext()) {
      return;
    }

    // collect the outlines, the renderer runs later and may not read the level
    Entity viewEntity = event.getCamera().entity();
    CollisionContext collisionContext = CollisionContext.of(viewEntity != null ? viewEntity : player);
    List<ExtraOutline> outlines = new ArrayList<>();
    int rendered = 0;
    do {
      BlockPos pos = extraBlocks.next();
      if (world.getWorldBorder().isWithinBounds(pos)) {
        rendered++;
        BlockState extraState = world.getBlockState(pos);
        if (!extraState.isAir()) {
          VoxelShape shape = extraState.getShape(world, pos, collisionContext);
          if (!shape.isEmpty()) {
            boolean translucent = mc.getModelManager().getBlockStateModelSet().get(extraState).hasMaterialFlag(world, pos, extraState, 1);
            outlines.add(new ExtraOutline(pos.immutable(), shape, translucent));
          }
        }
      }
    } while(rendered < MAX_BLOCKS && extraBlocks.hasNext());
    if (outlines.isEmpty()) {
      return;
    }

    // same render types, colors and widths as the vanilla outline of the targeted block
    boolean highContrast = event.isHighContrast();
    RenderType renderType;
    if (highContrast) {
      renderType = RenderTypes.linesDepthBias();
    } else if (mc.gameRenderer.useImprovedTransparency()) {
      renderType = RenderTypes.linesTranslucentNoDepthWrite();
    } else {
      renderType = RenderTypes.linesTranslucent();
    }
    int color = highContrast ? -11010079 : ARGB.black(102);
    float width = mc.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
    event.addCustomRenderer((outlineState, collector, matrices, levelState) -> {
      Vec3 camera = levelState.cameraRenderState.pos;
      for (ExtraOutline outline : outlines) {
        BlockPos pos = outline.pos();
        matrices.pushPose();
        matrices.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        submitOutline(matrices, collector, outline, renderType, color, width, highContrast);
        matrices.popPose();
      }
      // keep the vanilla outline on the targeted block
      return false;
    });
  }

  /** Submits a single outline */
  private static void submitOutline(PoseStack matrices, SubmitNodeCollector collector, ExtraOutline outline, RenderType renderType, int color, float width, boolean highContrast) {
    if (highContrast) {
      collector.submitShapeOutline(matrices, outline.shape(), RenderTypes.secondaryBlockOutline(), 0xFF000000, 7.0F, outline.isTranslucent());
    }
    collector.submitShapeOutline(matrices, outline.shape(), renderType, color, width, outline.isTranslucent());
  }

  /** Renders the block damage process on the extra blocks */
  @SubscribeEvent
  static void renderBlockDamageProgress(ExtractLevelRenderStateEvent event) {
    // validate required variables are set
    Minecraft mc = Minecraft.getInstance();
    MultiPlayerGameMode controller = mc.gameMode;
    if (controller == null || !controller.isDestroying()) {
      return;
    }
    ClientLevel world = event.getLevel();
    Player player = mc.player;
    if (world == null || player == null || mc.getCameraEntity() == null) {
      return;
    }
    // must have the right tags
    ItemStack stack = player.getMainHandItem();
    if (stack.isEmpty() || !stack.is(TinkerTags.Items.HARVEST)) {
      return;
    }
    // must be targeting a block
    HitResult result = mc.hitResult;
    if (result == null || result.getType() != Type.BLOCK) {
      return;
    }
    // must not be broken, must be right interface
    ToolStack tool = ToolStack.from(stack);
    if (tool.isBroken()) {
      return;
    }
    // find breaking progress
    BlockHitResult blockTrace = (BlockHitResult)result;
    BlockPos target = blockTrace.getBlockPos();
    SortedSet<BlockDestructionProgress> progresses = world.destructionProgress().get(target.asLong());
    if (progresses == null || progresses.isEmpty()) {
      return;
    }
    int progress = progresses.last().getProgress();
    // determine extra blocks to highlight
    BlockState state = world.getBlockState(target);
    // must not be broken, and the tool definition must be effective
    if (!IsEffectiveToolHook.isEffective(tool, state)) {
      return;
    }
    UseOnContext context = new UseOnContext(world, player, InteractionHand.MAIN_HAND, stack, blockTrace.withDirection(BlockSideHitListener.getSideHit(player)));
    Iterator<BlockPos> extraBlocks = tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, context, state, AreaOfEffectIterator.AOEMatchType.BREAKING).iterator();
    if (!extraBlocks.hasNext()) {
      return;
    }

    // finally, add the blocks to the vanilla breaking animation, which renders them like the targeted block
    List<BlockBreakingRenderState> breaking = event.getRenderState().blockBreakingRenderStates;
    int rendered = 0;
    do {
      BlockPos pos = extraBlocks.next().immutable();
      breaking.add(new BlockBreakingRenderState(pos, world.getBlockState(pos), progress));
      rendered++;
    } while (rendered < MAX_BLOCKS && extraBlocks.hasNext());
  }
}
