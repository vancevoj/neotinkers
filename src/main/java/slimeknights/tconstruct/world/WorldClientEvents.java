package slimeknights.tconstruct.world;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.particle.SlimeParticle;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.client.DragonSkullModel;
import slimeknights.tconstruct.world.client.SkullModelHelper;
import slimeknights.tconstruct.world.client.SlimeColorReloadListener;
import slimeknights.tconstruct.world.client.SlimeColorizer;
import slimeknights.tconstruct.world.client.TerracubeRenderer;
import slimeknights.tconstruct.world.client.TinkerSlimeRenderer;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unused")
@EventBusSubscriber(modid=TConstruct.MOD_ID, value=Dist.CLIENT)
public class WorldClientEvents extends ClientEventBase {
  /** Skull textures for each head type, registered with the skull models (formerly written into {@code SkullBlockRenderer.SKIN_BY_TYPE} by hand) */
  private static final Map<TinkerHeadType,Identifier> HEAD_TEXTURES = new EnumMap<>(TinkerHeadType.class);
  static {
    // vanilla entity textures moved into subfolders in 26.1, blaze.png is now blaze/blaze.png
    HEAD_TEXTURES.put(TinkerHeadType.BLAZE, Identifier.withDefaultNamespace("textures/entity/blaze/blaze.png"));
    HEAD_TEXTURES.put(TinkerHeadType.ENDERMAN, TConstruct.getResource("textures/entity/skull/enderman.png"));
    HEAD_TEXTURES.put(TinkerHeadType.STRAY, TConstruct.getResource("textures/entity/skull/stray.png"));
    HEAD_TEXTURES.put(TinkerHeadType.HUSK, Identifier.withDefaultNamespace("textures/entity/zombie/husk.png"));
    HEAD_TEXTURES.put(TinkerHeadType.DROWNED, TConstruct.getResource("textures/entity/skull/drowned.png"));
    HEAD_TEXTURES.put(TinkerHeadType.SPIDER, Identifier.withDefaultNamespace("textures/entity/spider/spider.png"));
    HEAD_TEXTURES.put(TinkerHeadType.CAVE_SPIDER, Identifier.withDefaultNamespace("textures/entity/spider/cave_spider.png"));
    HEAD_TEXTURES.put(TinkerHeadType.PIGLIN_BRUTE, Identifier.withDefaultNamespace("textures/entity/piglin/piglin_brute.png"));
    HEAD_TEXTURES.put(TinkerHeadType.ZOMBIFIED_PIGLIN, Identifier.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png"));
  }

  @SubscribeEvent
  static void addResourceListener(AddClientReloadListenersEvent event) {
    for (FoliageType type : FoliageType.values()) {
      event.addListener(TConstruct.getResource("slime_color/" + type.getSerializedName()), new SlimeColorReloadListener(type));
    }
  }

  @SubscribeEvent
  static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    event.registerSpecial(TinkerWorld.skySlimeParticle.get(), new SlimeParticle.Factory(SlimeType.SKY));
    event.registerSpecial(TinkerWorld.enderSlimeParticle.get(), new SlimeParticle.Factory(SlimeType.ENDER));
    event.registerSpecial(TinkerWorld.terracubeParticle.get(), new SlimeParticle.Factory(Items.CLAY_BALL));
  }

  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    // TODO: do we really need a separate copy of each head for each mob, or can we reuse them?
    Supplier<LayerDefinition> normalHead = Lazy.of(SkullModel::createMobHeadLayer);
    Supplier<LayerDefinition> customHead = Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 32, 16));
    Supplier<LayerDefinition> headOverlayCustom = Lazy.of(() -> SkullModelHelper.createHeadHatLayer(0, 16, 32, 32));
    registerLayerDefinition(event, TinkerHeadType.BLAZE, normalHead);
    registerLayerDefinition(event, TinkerHeadType.ENDERMAN, customHead);
    registerLayerDefinition(event, TinkerHeadType.STRAY, headOverlayCustom);

    // zombie
    registerLayerDefinition(event, TinkerHeadType.HUSK, Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 64, 64)));
    registerLayerDefinition(event, TinkerHeadType.DROWNED, headOverlayCustom);

    // spiders
    Supplier<LayerDefinition> spiderHead = Lazy.of(() -> SkullModelHelper.createHeadLayer(32, 4, 64, 32));
    registerLayerDefinition(event, TinkerHeadType.SPIDER, spiderHead);
    registerLayerDefinition(event, TinkerHeadType.CAVE_SPIDER, spiderHead);

    // piglin
    Supplier<LayerDefinition> piglinHead = Lazy.of(() -> LayerDefinition.create(PiglinHeadModel.createHeadModel(), 64, 64));
    registerLayerDefinition(event, TinkerHeadType.PIGLIN_BRUTE, piglinHead);
    registerLayerDefinition(event, TinkerHeadType.ZOMBIFIED_PIGLIN, piglinHead);

    // crafted
    event.registerLayerDefinition(SkullModelHelper.FLUID_CANNON, headOverlayCustom);
  }

  @SubscribeEvent
  static void registerSkullModels(EntityRenderersEvent.CreateSkullModels event) {
    // registers both the model and the skull texture, NeoForge stores the texture in SkullBlockRenderer.SKIN_BY_TYPE
    SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> {
      if (type.isPiglin()) {
        event.registerSkullModel(type, layer, PiglinHeadModel::new, HEAD_TEXTURES.get(type));
      } else {
        event.registerSkullModel(type, layer, SkullModel::new, HEAD_TEXTURES.get(type));
      }
    });
  }

  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(TinkerWorld.skySlimeEntity.get(), TinkerSlimeRenderer.SKY_SLIME_FACTORY);
    event.registerEntityRenderer(TinkerWorld.enderSlimeEntity.get(), TinkerSlimeRenderer.ENDER_SLIME_FACTORY);
    event.registerEntityRenderer(TinkerWorld.terracubeEntity.get(), TerracubeRenderer::new);
  }

  @SuppressWarnings("removal")
  @SubscribeEvent
  static void clientSetup(FMLClientSetupEvent event) {
    // slimeskull head models, skull block textures are registered in registerSkullModels
    event.enqueueWork(() -> {
      registerHeadModel(TinkerHeadType.BLAZE, MaterialIds.blaze);
      registerHeadModel(TinkerHeadType.ENDERMAN, MaterialIds.enderPearl);
      SlimeskullArmorModel.registerHeadModel(MaterialIds.dragonScale, modelSet -> new DragonSkullModel(modelSet.bakeLayer(ModelLayers.DRAGON_SKULL)), Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png"));
      SlimeskullArmorModel.registerHeadModel(MaterialIds.gunpowder, ModelLayers.CREEPER_HEAD, Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png"));
      // skeleton
      SlimeskullArmorModel.registerHeadModel(MaterialIds.bone, ModelLayers.SKELETON_SKULL, Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png"));
      SlimeskullArmorModel.registerHeadModel(MaterialIds.necroticBone, ModelLayers.WITHER_SKELETON_SKULL, Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png"));
      registerHeadModel(TinkerHeadType.STRAY, MaterialIds.ice);
      // zombies
      SlimeskullArmorModel.registerHeadModel(MaterialIds.leather, ModelLayers.ZOMBIE_HEAD, Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"));
      registerHeadModel(TinkerHeadType.HUSK, MaterialIds.iron);
      registerHeadModel(TinkerHeadType.DROWNED, MaterialIds.copper);
      // spider
      registerHeadModel(TinkerHeadType.SPIDER, MaterialIds.string);
      registerHeadModel(TinkerHeadType.CAVE_SPIDER, MaterialIds.darkthread);
      // piglins
      SlimeskullArmorModel.registerPiglinHeadModel(MaterialIds.gold, ModelLayers.PIGLIN_HEAD, Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png"));
      registerPiglinHeadModel(TinkerHeadType.PIGLIN_BRUTE, MaterialIds.roseGold);
      registerPiglinHeadModel(TinkerHeadType.ZOMBIFIED_PIGLIN, MaterialIds.pigIron);
      // crafted
      SlimeskullArmorModel.registerHeadModel(MaterialIds.knightmetal, SkullModelHelper.FLUID_CANNON, TConstruct.getResource("textures/entity/skull/fluid_cannon.png"));
    });
  }

  @SubscribeEvent
  static void registerBlockColorHandlers(RegisterColorHandlersEvent.BlockTintSources event) {
    // slime plants - blocks
    for (FoliageType type : FoliageType.values()) {
      event.register(
        List.of(new SlimeFoliageTint(type, null)),
        TinkerWorld.vanillaSlimeGrass.get(type), TinkerWorld.earthSlimeGrass.get(type), TinkerWorld.skySlimeGrass.get(type),
        TinkerWorld.enderSlimeGrass.get(type), TinkerWorld.ichorSlimeGrass.get(type));
      event.register(
        List.of(new SlimeFoliageTint(type, SlimeColorizer.LOOP_OFFSET)),
        TinkerWorld.slimeLeaves.get(type));
      event.register(
        List.of(new SlimeFoliageTint(type, null)),
        TinkerWorld.slimeFern.get(type), TinkerWorld.slimeTallGrass.get(type), TinkerWorld.pottedSlimeFern.get(type));
    }

    // vines
    event.register(
      List.of(new SlimeFoliageTint(FoliageType.SKY, SlimeColorizer.LOOP_OFFSET)),
      TinkerWorld.skySlimeVine.get());
    event.register(
      List.of(new SlimeFoliageTint(FoliageType.ENDER, SlimeColorizer.LOOP_OFFSET)),
      TinkerWorld.enderSlimeVine.get());
    // item colors are no longer registered in code: since 1.21.4 item tints come from the client item JSON,
    // the slime grass, leaves, fern, tall grass and vine items use a minecraft:constant tint with FoliageType#getColor (datagen)
  }

  /**
   * Block colors for a slime type
   * @param pos   Block position
   * @param type  Slime foliage color
   * @param add   Offset position
   * @return  Color for the given position, or the default if position is null
   */
  private static int getSlimeColorByPos(@Nullable BlockPos pos, FoliageType type, @Nullable BlockPos add) {
    if (pos == null) {
      return type.getColor();
    }
    if (add != null) {
      pos = pos.offset(add);
    }

    return SlimeColorizer.getColorForPos(pos, type);
  }

  /**
   * Tint source for slime foliage, replaces the block color lambdas.
   * Tints are multiplied with the vertex color including alpha since 26.x, so they must be opaque.
   * @param type  Foliage type
   * @param add   Position offset, or null for none
   */
  private record SlimeFoliageTint(FoliageType type, @Nullable BlockPos add) implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return ARGB.opaque(getSlimeColorByPos(null, type, add));
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      return ARGB.opaque(getSlimeColorByPos(pos, type, add));
    }
  }

  /** Registers a skull with the slimeskull renderer, the texture goes to the skull block renderer in {@link #registerSkullModels} */
  private static void registerHeadModel(TinkerHeadType skull, MaterialId materialId) {
    SlimeskullArmorModel.registerHeadModel(materialId, SkullModelHelper.HEAD_LAYERS.get(skull), HEAD_TEXTURES.get(skull));
  }

  /** Registers a skull with the slimeskull renderer, the texture goes to the skull block renderer in {@link #registerSkullModels} */
  private static void registerPiglinHeadModel(TinkerHeadType skull, MaterialId materialId) {
    SlimeskullArmorModel.registerPiglinHeadModel(materialId, SkullModelHelper.HEAD_LAYERS.get(skull), HEAD_TEXTURES.get(skull));
  }

  /** Register a head layer definition with forge */
  private static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event, TinkerHeadType head, Supplier<LayerDefinition> supplier) {
    event.registerLayerDefinition(SkullModelHelper.HEAD_LAYERS.get(head), supplier);
  }
}
