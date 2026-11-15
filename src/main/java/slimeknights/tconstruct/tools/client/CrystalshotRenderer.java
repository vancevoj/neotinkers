package slimeknights.tconstruct.tools.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tools.item.CrystalshotItem.CrystalshotEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CrystalshotRenderer extends ArrowRenderer<CrystalshotEntity> {
  private static final Map<String,Identifier> TEXTURES = new HashMap<>();
  private static final Function<String,Identifier> TEXTURE_GETTER = variant -> TConstruct.getResource("textures/entity/arrow/" + variant + ".png");
  public CrystalshotRenderer(Context context) {
    super(context);
  }

  @Override
  public Identifier getTextureLocation(CrystalshotEntity arrow) {
    return TEXTURES.computeIfAbsent(arrow.getVariant(), TEXTURE_GETTER);
  }
}
