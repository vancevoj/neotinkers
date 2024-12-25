package slimeknights.tconstruct.library.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;

/**
 * Wrapper that swaps the model for the GUI
 */
public class BakedUniqueGuiModel extends BakedModelWrapper<BakedModel> {
  private final BakedModel gui;

  public BakedUniqueGuiModel(BakedModel base, BakedModel gui) {
    super(base);
    this.gui = gui;
  }

  @Override
  public BakedModel applyTransform(ItemDisplayContext itemDisplay, PoseStack mat, boolean applyLeftHandTransform) {
    if (itemDisplay == ItemDisplayContext.GUI) {
      return gui.applyTransform(itemDisplay, mat, applyLeftHandTransform);
    }
    return originalModel.applyTransform(itemDisplay, mat, applyLeftHandTransform);
  }
}
