package slimeknights.tconstruct.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.logging.log4j.Logger;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.world.data.StructureRepalleter;
import slimeknights.tconstruct.world.worldgen.islands.IslandPiece;
import slimeknights.tconstruct.world.worldgen.islands.IslandStructure;
import slimeknights.tconstruct.world.worldgen.trees.ExtraRootVariantPlacer;
import slimeknights.tconstruct.world.worldgen.trees.LeaveVineDecorator;
import slimeknights.tconstruct.world.worldgen.trees.feature.SlimeFungusFeature;
import slimeknights.tconstruct.world.worldgen.trees.feature.SlimeTreeFeature;

/**
 * Contains any logic relevant to structure generation, including trees and islands
 */
@SuppressWarnings("unused")
public final class TinkerStructures extends TinkerModule {
  static final Logger log = Util.getLogger("tinker_structures");
  /** Features are data driven records, so the registry holds the codecs */
  private static final DeferredRegister<MapCodec<? extends Feature>> FEATURES = DeferredRegister.create(Registries.FEATURE_TYPE, TConstruct.MOD_ID);
  private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPE = DeferredRegister.create(Registries.STRUCTURE_TYPE, TConstruct.MOD_ID);
  private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE = DeferredRegister.create(Registries.STRUCTURE_PIECE, TConstruct.MOD_ID);
  private static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS = DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, TConstruct.MOD_ID);
  private static final DeferredRegister<RootPlacerType<?>> ROOT_PLACERS = DeferredRegister.create(Registries.ROOT_PLACER_TYPE, TConstruct.MOD_ID);


  public TinkerStructures() {
    IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
    FEATURES.register(bus);
    STRUCTURE_TYPE.register(bus);
    STRUCTURE_PIECE.register(bus);
    TREE_DECORATORS.register(bus);
    ROOT_PLACERS.register(bus);
  }


  /*
   * Misc
   */
  public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<LeaveVineDecorator>> leaveVineDecorator = TREE_DECORATORS.register("leave_vines", () -> new TreeDecoratorType<>(LeaveVineDecorator.CODEC));
  public static final DeferredHolder<RootPlacerType<?>, RootPlacerType<ExtraRootVariantPlacer>> extraRootVariantPlacer = ROOT_PLACERS.register("extra_root_variants", () -> new RootPlacerType<>(ExtraRootVariantPlacer.CODEC));

  /*
   * Features
   */
  /** Overworld variant of slimy trees, build with {@code new SlimeTreeFeature(config)} */
  public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SlimeTreeFeature>> slimeTree = FEATURES.register("slime_tree", () -> SlimeTreeFeature.CODEC);
  /** Nether variant of slimy trees, build with {@code new SlimeFungusFeature(config)} */
  public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SlimeFungusFeature>> slimeFungus = FEATURES.register("slime_fungus", () -> SlimeFungusFeature.CODEC);

  /* Greenheart trees */
  public static final ResourceKey<Feature> earthSlimeTree = key(Registries.FEATURE, "earth_slime_tree");
  public static final ResourceKey<Feature> earthSlimeIslandTree = key(Registries.FEATURE, "earth_slime_island_tree");
  /* Skyroot trees */
  public static final ResourceKey<Feature> skySlimeTree = key(Registries.FEATURE, "sky_slime_tree");
  public static final ResourceKey<Feature> skySlimeIslandTree = key(Registries.FEATURE, "sky_slime_island_tree");

  /* Enderslime trees */
  public static final ResourceKey<Feature> enderSlimeTree = key(Registries.FEATURE, "ender_slime_tree");
  public static final ResourceKey<Feature> enderSlimeTreeTall = key(Registries.FEATURE, "ender_slime_tree_tall");

  /* Bloodshroom trees */
  public static final ResourceKey<Feature> bloodSlimeFungus = key(Registries.FEATURE, "blood_slime_fungus");
  public static final ResourceKey<Feature> bloodSlimeIslandFungus = key(Registries.FEATURE, "blood_slime_island_fungus");

  /* Deprecated ichor tree */
  public static final ResourceKey<Feature> ichorSlimeFungus = key(Registries.FEATURE, "ichor_slime_fungus");

  /*
   * Structures
   */
  public static final DeferredHolder<StructurePieceType, StructurePieceType> islandPiece = STRUCTURE_PIECE.register("island", () -> IslandPiece::new);
  public static final DeferredHolder<StructureType<?>, StructureType<IslandStructure>> island = STRUCTURE_TYPE.register("island", () -> () -> IslandStructure.CODEC);


  // island structures - TODO 1.21: rename to better match placement?
  public static final ResourceKey<Structure> earthSlimeIsland = key(Registries.STRUCTURE, "earth_slime_island");
  public static final ResourceKey<Structure> skySlimeIsland = key(Registries.STRUCTURE, "sky_slime_island");
  public static final ResourceKey<Structure> oceanSkyslimeIsland = key(Registries.STRUCTURE, "ocean_skyslime_island");
  public static final ResourceKey<Structure> clayIsland = key(Registries.STRUCTURE, "clay_island");
  public static final ResourceKey<Structure> bloodIsland = key(Registries.STRUCTURE, "blood_island");
  public static final ResourceKey<Structure> endSlimeIsland = key(Registries.STRUCTURE, "end_slime_island");

  // island structure sets
  public static final ResourceKey<StructureSet> overworldOceanIsland = key(Registries.STRUCTURE_SET, "overworld_ocean_island");
  public static final ResourceKey<StructureSet> overworldSkyIsland = key(Registries.STRUCTURE_SET, "overworld_sky_island");
  public static final ResourceKey<StructureSet> netherOceanIsland = key(Registries.STRUCTURE_SET, "nether_ocean_island");
  public static final ResourceKey<StructureSet> endSkyIsland = key(Registries.STRUCTURE_SET, "end_sky_island");


  /** The single client data run generates both assets and data in 26.3, so server data providers also go on the client event */
  @SubscribeEvent
  void gatherData(final GatherDataEvent.Client event) {
    // structure templates are read from the mod data pack
    event.createProvider(packOutput -> new StructureRepalleter(packOutput, event.getResourceManager(PackType.SERVER_DATA)));
//    event.createProvider(packOutput -> new StructureUpdater(packOutput, event.getResourceManager(PackType.SERVER_DATA), TConstruct.MOD_ID, Target.DATA_PACK, "structures"));
//    event.createProvider(packOutput -> new StructureUpdater(packOutput, event.getResourceManager(PackType.CLIENT_RESOURCES), TConstruct.MOD_ID, Target.RESOURCE_PACK, "book/structures"));
  }
}
