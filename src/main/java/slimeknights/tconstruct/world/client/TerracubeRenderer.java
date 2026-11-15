package slimeknights.tconstruct.world.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.Slime;
import slimeknights.tconstruct.TConstruct;

/** Renderer for the terracube, using the magma cube model. Size and squish scaling come from {@link AbstractCubeMobRenderer} */
public class TerracubeRenderer extends AbstractCubeMobRenderer<Slime,ArmoredSlimeRenderState,MagmaCubeModel> {
  private static final Identifier TEXTURE = TConstruct.getResource("textures/entity/terracube.png");
  public TerracubeRenderer(EntityRendererProvider.Context context) {
    super(context, new MagmaCubeModel(context.bakeLayer(ModelLayers.MAGMA_CUBE)));
    addLayer(new SlimeArmorLayer<>(this, context, true));
  }

  @Override
  public ArmoredSlimeRenderState createRenderState() {
    return new ArmoredSlimeRenderState();
  }

  @Override
  public void extractRenderState(Slime entity, ArmoredSlimeRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    SlimeArmorLayer.extractHelmet(entity, state);
  }

  @Override
  protected float getShadowRadius(SlimeRenderState state) {
    // the terracube always had a fixed shadow, unlike vanilla cube mobs
    return 0.25F;
  }

  @Override
  public Identifier getTextureLocation(ArmoredSlimeRenderState state) {
    return TEXTURE;
  }
}
