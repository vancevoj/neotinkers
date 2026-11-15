package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.tconstruct.library.client.TinkerRenderTypes;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiSmelteryTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to render the smeltery tank.
 * <p>
 * 26.3: fluids are resolved into an immutable {@link TankFluids} during render state extraction with
 * {@link #extract(SmelteryTank, BlockPos, BlockPos, int)}, then submitted with {@link #renderFluids(PoseStack, SubmitNodeCollector, TankFluids)}.
 */
public class SmelteryTankRenderer {
  /** Distance between the liquid and the edge of the block */
  private static final float FLUID_OFFSET = 0.005f;
  /** Amount to subtract from the height for fluid offset */
  private static final int HEIGHT_OFFSET = (int) (FLUID_OFFSET * 2000d);

  /**
   * Gets the integer bounds for rendering a fluid with the given delta
   * @param delta  Delta
   * @return  Position array
   */
  private static float[] getBlockBounds(int delta) {
    return getBlockBounds(delta, FLUID_OFFSET, delta + 1f - FLUID_OFFSET);
  }

  /**
   * Gets the integer bounds for rendering a fluid with the given delta
   * @param delta  Delta
   * @param start  Start position
   * @param end    End position
   * @return  Position array
   */
  private static float[] getBlockBounds(int delta, float start, float end) {
    float[] bounds = new float[2 + delta];
    bounds[0] = start;
    int offset = (int) start;
    for(int i = 1; i <= delta; i++) bounds[i] = i + offset;
    bounds[delta+1] = end;
    return bounds;
  }

  /**
   * Computes the fluid layers in the smeltery tank, relative to tankMinPos. Call during render state extraction.
   * @param tank        Smeltery tank
   * @param tankMinPos  Min position for fluid rendering
   * @param tankMaxPos  Max position for fluid rendering
   * @param brightness  Packed lighting values
   * @return  Tank fluids, or null if nothing to render
   */
  @Nullable
  public static TankFluids extract(SmelteryTank<?> tank, BlockPos tankMinPos, BlockPos tankMaxPos, int brightness) {
    List<FluidStack> fluids = tank.getFluids();
    // empty smeltery :(
    if (fluids.isEmpty()) {
      return null;
    }
    // determine x and z bounds, constant
    int xd = tankMaxPos.getX() - tankMinPos.getX();
    int zd = tankMaxPos.getZ() - tankMinPos.getZ();
    // somehow people are getting a rendering crash with these being negative, no idea how but easy to catch
    if (xd < 0 || zd < 0) {
      return null;
    }

    // calc heights, we use mB capacities and then convert it over to blockheights during rendering
    int yd = 1 + Math.max(0, tankMaxPos.getY() - tankMinPos.getY());
    // one block height = 1000 mb
    int[] heights = GuiSmelteryTank.calcLiquidHeights(fluids, tank.getCapacity(), yd * 1000 - HEIGHT_OFFSET, 100);

    // resolve sprites, colors and light for each fluid
    List<FluidLayer> layers = new ArrayList<>(fluids.size());
    float curY = FLUID_OFFSET;
    for (int i = 0; i < fluids.size(); i++) {
      float h = (float) heights[i] / 1000f;
      FluidStack fluid = fluids.get(i);
      if (h > 0 && !fluid.isEmpty()) {
        FluidSprites sprites = FluidSprites.of(fluid);
        layers.add(new FluidLayer(sprites.still(), sprites.color(), FluidRenderer.getFluidLight(brightness, fluid), fluid.getFluid().getFluidType().isLighterThanAir(), curY, curY + h));
      }
      curY += h;
    }
    if (layers.isEmpty()) {
      return null;
    }
    return new TankFluids(xd, zd, List.copyOf(layers));
  }

  /**
   * Submits the smeltery tank fluids, relative to tankMinPos
   * @param matrices   Matrix stack instance, the current pose is captured
   * @param collector  Submit node collector
   * @param fluids     Fluids from {@link #extract(SmelteryTank, BlockPos, BlockPos, int)}
   */
  public static void renderFluids(PoseStack matrices, SubmitNodeCollector collector, @Nullable TankFluids fluids) {
    if (fluids != null) {
      collector.submitCustomGeometry(matrices, TinkerRenderTypes.SMELTERY_FLUID, (pose, builder) -> fluids.render(pose, builder));
    }
  }

  /**
   * Renders the smeltery tank fluids, relative to tankMinPos. Fluid data is resolved immediately.
   * @param matrices    Matrix stack instance
   * @param collector   Submit node collector
   * @param tank        Smeltery tank
   * @param brightness  Packed lighting values
   * @param tankMinPos  Min position for fluid rendering
   * @param tankMaxPos  Max position for fluid rendering
   */
  public static void renderFluids(PoseStack matrices, SubmitNodeCollector collector, SmelteryTank<?> tank, BlockPos tankMinPos, BlockPos tankMaxPos, int brightness) {
    renderFluids(matrices, collector, extract(tank, tankMinPos, tankMaxPos, brightness));
  }

  /**
   * Immutable fluid data for the smeltery tank
   * @param xd      X size for renderer
   * @param zd      Z size for renderer
   * @param layers  Fluid layers from bottom to top
   */
  public record TankFluids(int xd, int zd, List<FluidLayer> layers) {
    /** Renders all layers */
    public void render(PoseStack.Pose pose, VertexConsumer builder) {
      float[] xBounds = getBlockBounds(xd);
      float[] zBounds = getBlockBounds(zd);
      for (FluidLayer layer : layers) {
        renderLargeFluidCuboid(pose, builder, layer, xd, xBounds, zd, zBounds);
      }
    }
  }

  /**
   * Single fluid in the smeltery tank
   * @param still       Still sprite
   * @param color       Fluid color
   * @param brightness  Packed lighting values including the fluid light
   * @param upsideDown  If true, the fluid is a gas and renders upside down
   * @param yMin        Min y position
   * @param yMax        Max y position
   */
  public record FluidLayer(TextureAtlasSprite still, int color, int brightness, boolean upsideDown, float yMin, float yMax) {}

  /**
   * Renders a large fluid cuboid
   * @param pose       Pose from the custom geometry renderer
   * @param builder    Builder instance
   * @param layer      Fluid to render
   * @param xd         X size for renderer
   * @param xBounds    X positions to render
   * @param zd         Z size for renderer
   * @param zBounds    Z positions to render
   */
  private static void renderLargeFluidCuboid(PoseStack.Pose pose, VertexConsumer builder, FluidLayer layer, int xd, float[] xBounds, int zd, float[] zBounds) {
    float yMin = layer.yMin();
    float yMax = layer.yMax();
    if(yMin >= yMax) {
      return;
    }
    // fluid attributes
    TextureAtlasSprite still = layer.still();
    int color = layer.color();
    int brightness = layer.brightness();

    // the liquid can stretch over more blocks than the subtracted height is if yMin's decimal is bigger than yMax's decimal (causing UV over 1)
    // ignoring the decimals prevents this, as yd then equals exactly how many ints are between the two
    // for example, if yMax = 5.1 and yMin = 2.3, 2.8 (which rounds to 2), with the face array becoming 2.3, 3, 4, 5.1
    int yd = (int) (yMax - (int) yMin);
    // except in the rare case of yMax perfectly aligned with the block, causing the top face to render multiple times
    // for example, if yMax = 3 and yMin = 1, the values of the face array become 1, 2, 3, 3 as we then have middle ints
    if (yMax % 1d == 0) yd--;
    float[] yBounds = getBlockBounds(yd, yMin, yMax);

    // render each side
    Matrix4f matrix = pose.pose();
    Vector3f from = new Vector3f();
    Vector3f to = new Vector3f();
    int rotation = layer.upsideDown() ? 180 : 0;
    for(int y = 0; y <= yd; y++) {
      for(int z = 0; z <= zd; z++) {
        for(int x = 0; x <= xd; x++) {
          from.set(xBounds[x], yBounds[y], zBounds[z]);
          to.set(xBounds[x + 1], yBounds[y + 1], zBounds[z + 1]);
          if (x == 0)  FluidRenderer.putTexturedQuad(builder, matrix, still, from, to, Direction.WEST,  color, brightness, rotation, false);
          if (x == xd) FluidRenderer.putTexturedQuad(builder, matrix, still, from, to, Direction.EAST,  color, brightness, rotation, false);
          if (z == 0)  FluidRenderer.putTexturedQuad(builder, matrix, still, from, to, Direction.NORTH, color, brightness, rotation, false);
          if (z == zd) FluidRenderer.putTexturedQuad(builder, matrix, still, from, to, Direction.SOUTH, color, brightness, rotation, false);
          if (y == yd) FluidRenderer.putTexturedQuad(builder, matrix, still, from, to, Direction.UP,    color, brightness, rotation, false);
          if (y == 0) {
            // increase Y position slightly to prevent z fighting on neighboring fluids
            from.y = from.y() + 0.001f;
            FluidRenderer.putTexturedQuad(builder, matrix, still,   from, to, Direction.DOWN,  color, brightness, rotation, false);
          }
        }
      }
    }
  }
}
