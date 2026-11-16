package slimeknights.tconstruct.library.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Rendering helpers for fluids in block entity renderers.
 * <p>
 * 26.3: nothing is drawn immediately. Resolve fluid data during render state extraction, then submit geometry with
 * {@link SubmitNodeCollector#submitCustomGeometry}. The methods taking a {@link PoseStack.Pose} and a {@link VertexConsumer}
 * are meant to run inside such a lambda, the methods taking a {@link SubmitNodeCollector} submit for you.
 * The old {@code bindTexture}, {@code setup} and {@code setColorRGBA} helpers are gone with the render system shader state,
 * pass colors to the draw calls instead.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RenderUtils {
  /**
   * Applies the given opacity to a color
   * @param color    Color in ARGB format
   * @param opacity  Opacity from 0 to 255
   * @return  Color with the opacity multiplied into the alpha
   */
  public static int applyOpacity(int color, int opacity) {
    if (opacity < 0xFF) {
      // alpha is top 8 bits, multiply by opacity and divide out remainder
      int alpha = ((color >> 24) & 0xFF) * opacity / 0xFF;
      // clear bits in color and or in the new alpha
      color = (color & 0xFFFFFF) | (alpha << 24);
    }
    return color;
  }

  /**
   * Adds a fluid cuboid with transparency
   * @param pose     Pose from the custom geometry renderer
   * @param buffer   Vertex consumer for {@link MantleRenderTypes#FLUID}
   * @param cube     Fluid cuboid instance
   * @param sprites  Fluid sprites and color, see {@link FluidSprites#of(FluidStack)}
   * @param opacity  Fluid opacity to blend in
   * @param light    Quad lighting, already including the fluid light, see {@link FluidRenderer#getFluidLight(int, FluidStack)}
   */
  public static void renderTransparentCuboid(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube, FluidSprites sprites, int opacity, int light) {
    // nothing to render? skip
    if (opacity < 0) {
      return;
    }
    FluidRenderer.renderCuboid(pose, buffer, cube, sprites.still(), sprites.flowing(), cube.getFromScaled(), cube.getToScaled(), applyOpacity(sprites.color(), opacity), light);
  }

  /**
   * Submits fluid cuboids with transparency. Sprites, color and light are resolved immediately.
   * @param matrices   Matrix stack instance, the current pose is captured
   * @param collector  Submit node collector
   * @param cubes      Fluid cuboids to render
   * @param fluid      Fluid to render
   * @param opacity    Fluid opacity to blend in
   * @param light      Quad lighting
   */
  public static void renderTransparentCuboids(PoseStack matrices, SubmitNodeCollector collector, List<FluidCuboid> cubes, FluidStack fluid, int opacity, int light) {
    // nothing to render? skip
    if (opacity < 0 || fluid.isEmpty() || cubes.isEmpty()) {
      return;
    }
    FluidSprites sprites = FluidSprites.of(fluid);
    int fluidLight = FluidRenderer.getFluidLight(light, fluid);
    List<FluidCuboid> toRender = List.copyOf(cubes);
    collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> {
      for (FluidCuboid cube : toRender) {
        renderTransparentCuboid(pose, buffer, cube, sprites, opacity, fluidLight);
      }
    });
  }

  /**
   * Submits a fluid cuboid with transparency. Sprites, color and light are resolved immediately.
   * @param matrices   Matrix stack instance, the current pose is captured
   * @param collector  Submit node collector
   * @param cube       Fluid cuboid instance
   * @param fluid      Fluid to render
   * @param opacity    Fluid opacity to blend in
   * @param light      Quad lighting
   */
  public static void renderTransparentCuboid(PoseStack matrices, SubmitNodeCollector collector, FluidCuboid cube, FluidStack fluid, int opacity, int light) {
    renderTransparentCuboids(matrices, collector, List.of(cube), fluid, opacity, light);
  }

  /**
   * Updates the animated render offset of the tank. Call once per frame during render state extraction.
   * @param tank          Tank to update
   * @param partialTicks  Partial ticks
   * @return  Offset to use for rendering this frame
   */
  public static float updateRenderOffset(FluidTankAnimated tank, float partialTicks) {
    // clear render offset if no liquid
    if (tank.getFluid().isEmpty() || tank.getCapacity() <= 0) {
      tank.setRenderOffset(0);
      return 0;
    }
    // update render offset
    float offset = tank.getRenderOffset();
    if (offset > 1.2f || offset < -1.2f) {
      offset = offset - ((offset / 12f + 0.1f) * partialTicks);
      tank.setRenderOffset(offset);
    } else {
      tank.setRenderOffset(0);
    }
    return offset;
  }

  /**
   * Submits the fluid in an animated fluid tank, updating the render offset of the tank.
   * Prefer {@link FluidTankInfo#of(FluidTankAnimated, float, int, boolean)} in render state extraction.
   * @param matrices      Matrix stack instance
   * @param collector     Submit node collector
   * @param cube          Fluid cuboid instance
   * @param tank          Fluid tank animated to render
   * @param light         Quad lighting
   * @param partialTicks  Partial ticks
   * @param flipGas       If true, flips gas cubes
   */
  public static void renderFluidTank(PoseStack matrices, SubmitNodeCollector collector, FluidCuboid cube, FluidTankAnimated tank, int light, float partialTicks, boolean flipGas) {
    FluidTankInfo info = FluidTankInfo.of(tank, partialTicks, light, flipGas);
    if (info != null) {
      info.submit(matrices, collector, List.of(cube));
    }
  }

  /**
   * Immutable fluid tank data for render states, computed during extraction.
   * @param sprites   Fluid sprites and color
   * @param amount    Fluid amount
   * @param capacity  Tank capacity, always above 0
   * @param offset    Fluid amount offset, used to animate transitions
   * @param flip      If true, the fluid fills from the top
   * @param light     Quad lighting including the fluid light
   */
  public record FluidTankInfo(FluidSprites sprites, int amount, int capacity, float offset, boolean flip, int light) {
    /**
     * Creates tank info for the given fluid
     * @param fluid     Fluid to render
     * @param capacity  Tank capacity
     * @param offset    Fluid amount offset, used to animate transitions
     * @param light     Quad lighting from the renderer
     * @param flipGas   If true, flips gas cubes
     * @return  Tank info, or null if nothing should render
     */
    @Nullable
    public static FluidTankInfo of(FluidStack fluid, int capacity, float offset, int light, boolean flipGas) {
      if (fluid.isEmpty() || capacity <= 0) {
        return null;
      }
      return new FluidTankInfo(FluidSprites.of(fluid), fluid.getAmount(), capacity, offset, FluidRenderer.shouldFlip(fluid, flipGas), FluidRenderer.getFluidLight(light, fluid));
    }

    /**
     * Creates tank info for the given animated tank, updating the tank render offset
     * @param tank          Tank to render
     * @param partialTicks  Partial ticks
     * @param light         Quad lighting from the renderer
     * @param flipGas       If true, flips gas cubes
     * @return  Tank info, or null if nothing should render
     */
    @Nullable
    public static FluidTankInfo of(FluidTankAnimated tank, float partialTicks, int light, boolean flipGas) {
      float offset = updateRenderOffset(tank, partialTicks);
      return of(tank.getFluid(), tank.getCapacity(), offset, light, flipGas);
    }

    /**
     * Renders a single cuboid scaled to the tank amount
     * @param pose    Pose from the custom geometry renderer
     * @param buffer  Vertex consumer for {@link MantleRenderTypes#FLUID}
     * @param cube    Cube to render
     */
    public void render(PoseStack.Pose pose, VertexConsumer buffer, FluidCuboid cube) {
      Vector3f from = cube.getFromScaled();
      Vector3f to = cube.getToScaled();
      float scaledY = FluidRenderer.getScaledY(from.y(), to.y(), amount, offset, capacity, flip);
      FluidRenderer.renderScaledCuboid(pose, buffer, cube, sprites, scaledY, flip, light);
    }

    /**
     * Submits the given cubes scaled to the tank amount
     * @param matrices   Matrix stack instance, the current pose is captured
     * @param collector  Submit node collector
     * @param cubes      Cubes to render
     */
    public void submit(PoseStack matrices, SubmitNodeCollector collector, List<FluidCuboid> cubes) {
      if (cubes.isEmpty()) {
        return;
      }
      List<FluidCuboid> toRender = List.copyOf(cubes);
      collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> {
        for (FluidCuboid cube : toRender) {
          render(pose, buffer, cube);
        }
      });
    }
  }

  public static int alpha(int c) {
    return (c >> 24) & 0xFF;
  }

  public static int red(int c) {
    return (c >> 16) & 0xFF;
  }

  public static int green(int c) {
    return (c >> 8) & 0xFF;
  }

  public static int blue(int c) {
    return (c) & 0xFF;
  }
}
