package slimeknights.tconstruct.world.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.function.Supplier;

/**
 * Renders an item model in place of a skull, for the sake of making a Slimeskull with a block item.
 * <p>
 * 26.3: this can no longer be a {@link net.minecraft.client.model.object.skull.SkullModelBase}, as models are drawn later with a
 * single render type and cannot draw item quads. Instead, call {@link #submit} where the skull model would be submitted, using the
 * same pose as a skull model.
 **/
public class BlockModelSkullRenderer {
  /** Stack to render, resolved lazily as item stacks cannot be created before components are bound */
  private final Supplier<ItemStack> stack;
  /** Render state for the item, reused as submitting copies everything it needs */
  private final ItemStackRenderState renderState = new ItemStackRenderState();

  public BlockModelSkullRenderer(Supplier<ItemStack> stack) {
    this.stack = stack;
  }

  public BlockModelSkullRenderer(ItemStack stack) {
    this(() -> stack);
  }

  /** Gets the stack rendered by this skull */
  public ItemStack getStack() {
    return stack.get();
  }

  /**
   * Submits the item at the skull location
   * @param poseStack     Pose stack, positioned like a skull model
   * @param collector     Collector to submit to
   * @param light         Light coordinates
   * @param overlay       Overlay coordinates
   * @param outlineColor  Entity outline color, 0 for none
   * @param yRot          Head Y rotation in degrees, as {@link net.minecraft.client.model.object.skull.SkullModelBase.State#yRot}
   * @param xRot          Head X rotation in degrees, as {@link net.minecraft.client.model.object.skull.SkullModelBase.State#xRot}
   */
  public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outlineColor, float yRot, float xRot) {
    Minecraft minecraft = Minecraft.getInstance();
    // display transform for the head context is applied by the render state
    minecraft.getItemModelResolver().updateForTopItem(renderState, stack.get(), ItemDisplayContext.HEAD, minecraft.level, null, 0);
    if (renderState.isEmpty()) {
      return;
    }
    poseStack.pushPose();

    // from CustomHeadLayer#translateToHead, with final scale adjusted
    poseStack.translate(0.0F, -0.25F, 0.0F);
    poseStack.rotateDegrees(Axis.YP, 180.0F);
    poseStack.scale(0.5F, -0.5F, -0.5F);

    // we don't really use rotation, but just in case
    if (yRot != 0 || xRot != 0) {
      poseStack.rotate((new Quaternionf()).rotationZYX(0, yRot * ((float)Math.PI / 180F), xRot * ((float)Math.PI / 180F)));
    }
    renderState.submit(poseStack, collector, light, overlay, outlineColor);

    poseStack.popPose();
  }
}
