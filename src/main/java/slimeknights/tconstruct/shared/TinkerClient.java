package slimeknights.tconstruct.shared;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.ExtractBlockScreenEffectEvent.OverlayType;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.client.armor.TinkerArmorLayer;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.DyedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FirstArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FixedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.MaterialArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.MaterialHasFallbackTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.TrimArmorTextureSupplier;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.client.data.spritetransformer.FramesSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.IColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.ISpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.OffsettingSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.RecolorSpriteTransformer;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.modifiers.DyedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.client.modifiers.NormalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.PotionModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.BannerModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.CompoundModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ConditionalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.FluidModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.MaterialHasFallbackModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.MaterialModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.NestedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.TankModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.TrimModifierModel;
import slimeknights.tconstruct.tools.client.SlimeskullModifierModel;

import java.util.function.Consumer;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * This class should only be referenced on the client side
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class TinkerClient {
  /**
   * Called by TConstruct to handle any client side logic that needs to run during the constructor
   */
  public static void onConstruct() {
    TinkerBook.initBook();
    // needs to register listeners early enough for minecraft to load
    ModifierIconManager.init();
    MaterialRenderInfoLoader.init();

    // add the recipe cache invalidator to the client
    Consumer<RecipesReceivedEvent> recipesUpdated = event -> RecipeCacheInvalidator.reload(true);
    NeoForge.EVENT_BUS.addListener(recipesUpdated);

    // register datagen serializers
    ISpriteTransformer.SERIALIZER.registerDeserializer(RecolorSpriteTransformer.NAME, RecolorSpriteTransformer.DESERIALIZER);
    GreyToSpriteTransformer.init();
    ISpriteTransformer.SERIALIZER.registerDeserializer(OffsettingSpriteTransformer.NAME, OffsettingSpriteTransformer.DESERIALIZER);
    ISpriteTransformer.SERIALIZER.registerDeserializer(FramesSpriteTransformer.NAME, FramesSpriteTransformer.DESERIALIZER);
    IColorMapping.SERIALIZER.registerDeserializer(GreyToColorMapping.NAME, GreyToColorMapping.DESERIALIZER);

    // armor textures
    ArmorTextureSupplier.LOADER.register(getResource("fixed"), FixedArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("dyed"), DyedArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("first_present"), FirstArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("material"), MaterialArmorTextureSupplier.Material.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("persistent_data"), MaterialArmorTextureSupplier.PersistentData.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("trim"), TrimArmorTextureSupplier.LOADER);
    ArmorTextureSupplier.LOADER.register(getResource("material_has_fallback"), MaterialHasFallbackTextureSupplier.LOADER);

    // modifier models
    ModifierModel.LOADER.register(getResource("empty"), ModifierModel.EMPTY.getLoader());
    ModifierModel.LOADER.register(getResource("compound"), CompoundModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("conditional"), ConditionalModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("trait"), NestedModifierModel.Trait.LOADER);
    ModifierModel.LOADER.register(getResource("crafted"), NestedModifierModel.Crafted.LOADER);
    ModifierModel.LOADER.register(getResource("basic"), NormalModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("dyed"), DyedModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("material_index"), MaterialModifierModel.Index.LOADER);
    ModifierModel.LOADER.register(getResource("persistent_material"), MaterialModifierModel.PersistentData.LOADER);
    ModifierModel.LOADER.register(getResource("dyed_material"), MaterialModifierModel.Dyed.LOADER);
    ModifierModel.LOADER.register(getResource("potion"), PotionModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("armor_trim"), TrimModifierModel.Armor.LOADER);
    ModifierModel.LOADER.register(getResource("custom_trim"), TrimModifierModel.Custom.LOADER);
    ModifierModel.LOADER.register(getResource("banner"), BannerModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("fluid"), FluidModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("tank"), TankModifierModel.LOADER);
    ModifierModel.LOADER.register(getResource("material_has_fallback"), MaterialHasFallbackModifierModel.LOADER);
    // specialized
    ModifierModel.LOADER.register(getResource("slimeskull"), SlimeskullModifierModel.LOADER);
  }

  @SubscribeEvent
  static void addArmorLayers(EntityRenderersEvent.AddLayers event) {
    // Tinkers armor renders through its own layer since 26.3, the vanilla armor model hook cannot draw multiple layers
    TinkerArmorLayer.addLayers(event);
  }

  @SubscribeEvent
  static void renderBlockOverlay(ExtractBlockScreenEffectEvent event) {
    if (event.getOverlayType() != OverlayType.BLOCK) {
      return;
    }
    BlockState state = event.getState();
    if (state.is(TinkerTags.Blocks.TRANSPARENT_OVERLAY)) {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      assert level != null;
      Player player = event.getPlayer();
      BlockPos pos = event.getPos();
      float width = player.getBbWidth() * 0.8F;
      // check collision of the block again, for non-full blocks
      if (Shapes.joinIsNotEmpty(state.getShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(AABB.ofSize(player.getEyePosition(), width, 1.0E-6D, width)), BooleanOp.AND)) {
        // this is for the most part a clone of the vanilla logic from ScreenEffectRenderer with some changes mentioned below
        // everything is extracted here, the renderer runs later and only gets plain values

        TextureAtlasSprite texture = minecraft.getModelManager().getBlockStateModelSet().getParticleMaterial(state, level, pos).sprite();
        Identifier atlas = texture.atlasLocation();

        // change: handle brightness based on renderWater, vanilla uses a fixed dark color. Blend comes from the screen effect pipeline
        BlockPos blockpos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        float brightness = Lightmap.getBrightness(level.dimensionType(), level.getMaxLocalRawBrightness(blockpos));
        int color = ARGB.colorFromFloat(1.0f, brightness, brightness, brightness);

        // draw the quad
        float u0 = texture.getU0();
        float u1 = texture.getU1();
        float v0 = texture.getV0();
        float v1 = texture.getV1();
        event.setCustomRenderer((playerState, cameraState, collector, poseStack, sprites, partialTicks, hideGui) -> {
          collector.submitCustomGeometry(poseStack, RenderTypes.blockScreenEffect(atlas), (pose, builder) -> {
            Matrix4f matrix4f = pose.pose();
            builder.addVertex(matrix4f, -1, -1, -0.5f).setUv(u1, v1).setColor(color);
            builder.addVertex(matrix4f, 1, -1, -0.5f).setUv(u0, v1).setColor(color);
            builder.addVertex(matrix4f, 1, 1, -0.5f).setUv(u0, v0).setColor(color);
            builder.addVertex(matrix4f, -1, 1, -0.5f).setUv(u1, v0).setColor(color);
          });
          return true;
        });
      }
      // canceling skips the vanilla dark overlay, the custom renderer above (if set) still runs
      event.setCanceled(true);
    }
  }
}
