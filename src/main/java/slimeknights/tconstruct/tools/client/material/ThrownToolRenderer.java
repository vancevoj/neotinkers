package slimeknights.tconstruct.tools.client.material;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.InventoryMenu;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.tools.entity.ThrownTool;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

/** Renderer for {@link ThrownTool} */
public class ThrownToolRenderer<T extends AbstractArrow & ToolProjectile> extends EntityRenderer<T> {
  protected final ItemRenderer itemRenderer;
  public ThrownToolRenderer(Context context) {
    super(context);
    this.itemRenderer = context.getItemRenderer();
  }

  @Override
  public void render(T entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    poseStack.pushPose();
    poseStack.rotate(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90));
    poseStack.rotate(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 225));
    poseStack.translate(0.2, -0.2, 0);
    this.itemRenderer.renderStatic(entity.getDisplayTool(), TinkerItemDisplays.THROWN, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
    poseStack.popPose();
    super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
  }

  @Override
  public Identifier getTextureLocation(T entity) {
    return InventoryMenu.BLOCK_ATLAS;
  }
}
