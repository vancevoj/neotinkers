package slimeknights.tconstruct.world.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModelBase;

/** Extension of {@link PiglinHeadModel} to adjust scale for slimeskulls */
public class PiglinSkullModel extends PiglinHeadModel {
  public PiglinSkullModel(ModelPart pRoot) {
    super(pRoot);
  }

  @Override
  public void setupAnim(SkullModelBase.State state) {
    super.setupAnim(state);
    // renderToBuffer is final since 26.x, so scale through the root pose instead of the pose stack
    this.root.xScale = 0.97f;
    this.root.yScale = 0.97f;
    this.root.zScale = 0.97f;
  }
}
