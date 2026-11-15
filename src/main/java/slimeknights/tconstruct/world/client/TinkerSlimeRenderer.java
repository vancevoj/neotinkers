package slimeknights.tconstruct.world.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.Slime;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.entity.ArmoredSlimeEntity;

/**
 * Renderer for sky and ender slimes. Replicates the vanilla slime renderer as its outer layer hardcodes the vanilla texture since 26.x.
 */
public class TinkerSlimeRenderer extends AbstractCubeMobRenderer<Slime,ArmoredSlimeRenderState,SlimeModel> {
  public static final Factory SKY_SLIME_FACTORY = new Factory(TConstruct.getResource("textures/entity/sky_slime.png"), TConstruct.getResource("textures/entity/steel_slime.png"));
  public static final Factory ENDER_SLIME_FACTORY = new Factory(TConstruct.getResource("textures/entity/ender_slime.png"), TConstruct.getResource("textures/entity/knightmetal_slime.png"));

  private final Identifier slime, metal;
  public TinkerSlimeRenderer(EntityRendererProvider.Context context, Identifier slime, Identifier metal) {
    super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)));
    this.slime = slime;
    this.metal = metal;
    addLayer(new OuterLayer(this, context.getModelSet()));
    addLayer(new SlimeArmorLayer<>(this, context, false));
  }

  @Override
  public ArmoredSlimeRenderState createRenderState() {
    return new ArmoredSlimeRenderState();
  }

  @Override
  public void extractRenderState(Slime entity, ArmoredSlimeRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.metal = entity instanceof ArmoredSlimeEntity armored && armored.isMetal();
    SlimeArmorLayer.extractHelmet(entity, state);
  }

  @Override
  protected void scale(ArmoredSlimeRenderState state, PoseStack poseStack) {
    // matches the vanilla slime renderer
    this.downscaleSlightly(poseStack);
    super.scale(state, poseStack);
  }

  @Override
  public Identifier getTextureLocation(ArmoredSlimeRenderState state) {
    if (slime != metal && state.metal) {
      return metal;
    }
    return slime;
  }

  /** Copy of the vanilla slime outer layer using our texture instead of the vanilla slime texture */
  private static class OuterLayer extends RenderLayer<ArmoredSlimeRenderState,SlimeModel> {
    private final TinkerSlimeRenderer renderer;
    private final SlimeModel model;

    public OuterLayer(TinkerSlimeRenderer renderer, EntityModelSet modelSet) {
      super(renderer);
      this.renderer = renderer;
      this.model = new SlimeModel(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ArmoredSlimeRenderState state, float yRot, float xRot) {
      boolean appearsGlowingWithInvisibility = state.appearsGlowing() && state.isInvisible;
      if (!state.isInvisible || appearsGlowingWithInvisibility) {
        Identifier texture = renderer.getTextureLocation(state);
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        if (appearsGlowingWithInvisibility) {
          submitNodeCollector.order(1).submitModel(this.model, state, poseStack, RenderTypes.outline(texture), lightCoords, overlayCoords, state.outlineColor);
        } else {
          submitNodeCollector.order(1).submitModel(this.model, state, poseStack, RenderTypes.entityTranslucent(texture), lightCoords, overlayCoords, state.outlineColor);
        }
      }
    }
  }

  private record Factory(Identifier slime, Identifier metal) implements EntityRendererProvider<Slime> {
    public Factory(Identifier texture) {
      this(texture, texture);
    }

    @Override
    public EntityRenderer<Slime,?> create(Context context) {
      return new TinkerSlimeRenderer(context, slime, metal);
    }
  }
}
