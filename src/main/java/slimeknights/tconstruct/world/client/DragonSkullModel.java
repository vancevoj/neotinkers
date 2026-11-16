package slimeknights.tconstruct.world.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.skull.SkullModelBase;

/** Recreation of {@link net.minecraft.client.model.object.skull.DragonHeadModel} but adjusted for slimeskulls. */
public class DragonSkullModel extends SkullModelBase {
  private final ModelPart head;
  private final ModelPart jaw;

  public DragonSkullModel(ModelPart root) {
    super(root);
    this.head = root.getChild("head");
    this.jaw = this.head.getChild("jaw");
  }

  @Override
  public void setupAnim(SkullModelBase.State state) {
    super.setupAnim(state);
    this.jaw.xRot = (float)(Math.sin(state.animationPos * Math.PI * 0.2f) + 1) * 0.2f;
    this.head.yRot = state.yRot * ((float)Math.PI / 180);
    this.head.xRot = state.xRot * ((float)Math.PI / 180);
    // renderToBuffer is final since 26.x, so the old translate (0, -0.25, 0.075) and scale (0.5, 0.5, 0.49) are applied as the root pose
    // note this does not work properly with rotations due to the head origin being weird
    // however, our only usage of this is in SlimeskullArmorModel so its not an issue
    this.root.y = -0.25f * 16;
    this.root.z = 0.075f * 16;
    this.root.xScale = 0.5f;
    this.root.yScale = 0.5f;
    this.root.zScale = 0.49f;
  }
}
