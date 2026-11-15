package slimeknights.tconstruct.library.client;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import slimeknights.tconstruct.TConstruct;

/**
 * Render types used by Tinkers.
 * <p>
 * 26.3: render types can no longer be subclassed and blend, depth and cull state moved into render pipelines, so this is a plain holder.
 */
public final class TinkerRenderTypes {
  private TinkerRenderTypes() {}

  /**
   * Pipeline for lines seen through everything, mostly based on {@link RenderPipelines#LINES_TRANSLUCENT}.
   * Matches the old error block state: translucent, no cull, no depth test and no depth write.
   */
  public static final RenderPipeline ERROR_LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
    .withLocation(TConstruct.getResource("pipeline/error_lines"))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withCull(false)
    .build();

  /** Render type for the error block that is seen through everything, mostly based on {@link RenderTypes#lines()} */
  public static final RenderType ERROR_BLOCK = RenderType.create(
    TConstruct.resourceString("lines"),
    RenderSetup.builder(ERROR_LINES_PIPELINE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());

  /**
   * Render type for fluids, like {@link slimeknights.mantle.client.render.MantleRenderTypes#FLUID}, but disables cull so both sides show.
   * This is the vanilla entity translucent type, so it uses the {@code ENTITY} vertex format and stays compatible with shader packs.
   */
  public static final RenderType SMELTERY_FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

  /** Registers the custom pipelines, called from {@link RegisterRenderPipelinesEvent} on the client mod bus */
  public static void registerPipelines(RegisterRenderPipelinesEvent event) {
    event.registerPipeline(ERROR_LINES_PIPELINE);
  }
}
