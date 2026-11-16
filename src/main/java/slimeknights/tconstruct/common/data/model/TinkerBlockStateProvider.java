package slimeknights.tconstruct.common.data.model;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import slimeknights.mantle.client.model.builder.ColoredModelBuilder;
import slimeknights.mantle.client.model.builder.ConnectedModelBuilder;
import slimeknights.mantle.client.model.builder.MantleItemLayerBuilder;
import slimeknights.mantle.client.model.builder.VariantBlockStateModelBuilder;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.world.TinkerWorld;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.minecraft.client.data.models.BlockModelGenerators.NOP;
import static net.minecraft.client.data.models.BlockModelGenerators.ROTATIONS_COLUMN_WITH_FACING;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_180;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_270;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_90;
import static net.minecraft.client.data.models.BlockModelGenerators.condition;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;
import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Generates block states and block models, along with the matching item model files under {@code models/item}.
 * Since 1.21.4 NeoForge's block state provider is gone, so this uses the vanilla model generators and templates.
 * Item model definitions ({@code items/*.json}) are not generated here, those come from the client item provider.
 * Render types are no longer written: since 26.1 the chunk layer comes from the texture transparency.
 */
@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
public class TinkerBlockStateProvider extends ModelProvider {
  /** Folder for block models and textures */
  private static final String BLOCK_FOLDER = "block";
  /** Folder for item models and textures */
  private static final String ITEM_FOLDER = "item";
  /** Texture slot for pane faces */
  private static final TextureSlot PANE = TextureSlot.create("pane");
  /** Texture slot for pane edges */
  private static final TextureSlot EDGE = TextureSlot.create("edge");
  /** Item model template for basic generated items */
  private static final ModelTemplate GENERATED = ModelTemplates.FLAT_ITEM;

  /** Block state output, set while generating */
  private Consumer<BlockModelDefinitionGenerator> blockStateOutput;
  /** Model output, set while generating */
  private BiConsumer<Identifier, ModelInstance> modelOutput;

  public TinkerBlockStateProvider(PackOutput output) {
    super(output, TConstruct.MOD_ID);
  }

  @Override
  public String getName() {
    return "Neo Tinkers Block State Models";
  }

  /* Not every block is handled here and client items come from another provider, so skip the vanilla validation */

  @Override
  protected Stream<? extends Holder<Block>> getKnownBlocks() {
    return Stream.empty();
  }

  @Override
  protected Stream<? extends Holder<Item>> getKnownItems() {
    return Stream.empty();
  }

  @Override
  protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
    this.blockStateOutput = blockModels.blockStateOutput;
    this.modelOutput = blockModels.modelOutput;
    registerStatesAndModels();
  }

  /** Registers all block states and models */
  protected void registerStatesAndModels() {
    addFenceBuildingBlock(TinkerMaterials.blazewood, "block/wood/blazewood/", "planks", blockTexture("wood/blazewood"));
    addFenceBuildingBlock(TinkerMaterials.nahuatl, "block/wood/nahuatl/", "planks", blockTexture("wood/nahuatl"));
    addWood(TinkerWorld.greenheart, false);
    addWood(TinkerWorld.skyroot, true);
    addWood(TinkerWorld.bloodshroom, true);
    addWood(TinkerWorld.enderbark, true);
    basicBlock(TinkerWorld.enderbarkRoots.get(), template(Identifier.withDefaultNamespace("block/mangrove_roots"), TextureSlot.SIDE, TextureSlot.TOP).create(
      getResource("block/wood/enderbark/roots/empty"),
      new TextureMapping().put(TextureSlot.SIDE, material(blockTexture("wood/enderbark/roots"))).put(TextureSlot.TOP, material(blockTexture("wood/enderbark/roots_top"))),
      modelOutput));
    TinkerWorld.slimyEnderbarkRoots.forEach((type, block) -> {
      String name = type.getSerializedName();
      cubeColumn(block, "block/wood/enderbark/roots/" + name, blockTexture("wood/enderbark/roots/" + name), blockTexture("wood/enderbark/roots/" + name + "_top"));
    });

    // clear glass
    glassBlock(TinkerCommons.clearGlass.get(), TinkerCommons.clearGlassPane.get(), "clear_glass/", getResource("block/clear_glass"), -1, true);
    Identifier clearStainedGlass = getResource("block/clear_stained_glass");
    for (GlassColor color : GlassColor.values()) {
      glassBlock(TinkerCommons.clearStainedGlass.get(color), TinkerCommons.clearStainedGlassPane.get(color), "clear_glass/" + color.getSerializedName() + "/", clearStainedGlass, 0xFF000000 | color.getColor(), false);
    }
    glassBlock(TinkerCommons.soulGlass.get(), TinkerCommons.soulGlassPane.get(), "soul_glass/", getResource("block/soul_glass"), -1, false);
    // smeltery glass - share a common top texture
    glassBlock(TinkerSmeltery.searedGlass.get(),     TinkerSmeltery.searedGlassPane.get(),     "smeltery/glass/", getResource("block/smeltery/seared_glass"), -1, true);
    glassBlock(TinkerSmeltery.searedSoulGlass.get(), TinkerSmeltery.searedSoulGlassPane.get(), "smeltery/soul_glass/",
               getResource("block/smeltery/soul_glass"), getResource("block/smeltery/seared_glass_top"), -1, true);
    glassBlock(TinkerSmeltery.scorchedGlass.get(),     TinkerSmeltery.scorchedGlassPane.get(),     "foundry/glass/", getResource("block/foundry/glass"), -1, true);
    glassBlock(TinkerSmeltery.scorchedSoulGlass.get(), TinkerSmeltery.scorchedSoulGlassPane.get(), "foundry/soul_glass/",
               getResource("block/foundry/soul_glass"), getResource("block/foundry/glass_top"), -1, true);
    // obsidian pane
    Identifier obsidian = Identifier.withDefaultNamespace("block/obsidian");
    paneBlock(TinkerCommons.obsidianPane.get(), "obsidian_pane/", obsidian, obsidian, false, -1, false);

    // shards
    bud(TinkerWorld.steelCluster.get(), "block/geode/steel_cluster", blockTexture("geode/steel_cluster"));
    bud(TinkerWorld.cobaltCluster.get(), "block/geode/cobalt_cluster", blockTexture("geode/cobalt_cluster"));
    bud(TinkerWorld.knightmetalCluster.get(), "block/geode/knightmetal_cluster", blockTexture("geode/knightmetal_cluster"));

    // geodes
    geode(TinkerWorld.earthGeode, "earth");
    geode(TinkerWorld.skyGeode,   "sky");
    geode(TinkerWorld.ichorGeode, "ichor");
    geode(TinkerWorld.enderGeode, "ender");

    // smeltery
    basicBlock(TinkerSmeltery.searedLamp.get(), "block/smeltery/seared/lamp", blockTexture("smeltery/seared/lamp"));
    basicBlock(TinkerSmeltery.scorchedLamp.get(), "block/foundry/scorched/lamp", blockTexture("foundry/scorched/lamp"));

    // heads
    Identifier skull = Identifier.withDefaultNamespace("block/skull");
    TinkerWorld.heads.forEach(head -> simpleBlock(head, skull));
    TinkerWorld.wallHeads.forEach(head -> simpleBlock(head, skull));
  }


  /* Helpers */

  /** Creates a texture in the block folder */
  protected Identifier blockTexture(String path) {
    return getResource(BLOCK_FOLDER + "/" + path);
  }

  /** Creates a texture in the block folder */
  protected Identifier itemTexture(String path) {
    return getResource(ITEM_FOLDER + "/" + path);
  }

  /** Creates a model location under this mod from the given path */
  protected Identifier modelLocation(String location) {
    return getResource(location);
  }

  /** Creates a material for the given texture */
  protected static Material material(Identifier texture) {
    return new Material(texture);
  }

  /** Creates a template with the given parent and texture slots */
  protected static ModelTemplate template(Identifier parent, TextureSlot... slots) {
    return new ModelTemplate(Optional.of(parent), Optional.empty(), slots);
  }

  /** Adds a block state */
  protected void blockState(BlockModelDefinitionGenerator generator) {
    blockStateOutput.accept(generator);
  }

  /** Creates all models for a building block object */
  protected void addBuildingBlock(BuildingBlockObject block, String folder, String name, Identifier texture) {
    Identifier blockModel = basicBlock(block.get(), folder + name, texture);
    slab(block.getSlab(), folder + "slab", blockModel, texture, texture, texture);
    stairs(block.getStairs(), folder + "stairs", texture, texture, texture);
  }

  /** Creates all models for a building block object */
  protected void addFenceBuildingBlock(FenceBuildingBlockObject block, String folder, String name, Identifier texture) {
    addBuildingBlock(block, folder, name, texture);
    fence(block.getFence(), folder + "fence/", texture);
  }

  /** Creates all models for the given wood block object */
  protected void addWood(WoodBlockObject wood, boolean trapdoorOrientable) {
    String plankPath = wood.getId().getPath();
    String name = plankPath.substring(0, plankPath.length() - "_planks".length());
    String folder = "block/wood/" + name + "/";
    // helper to get textures for wood, since we put them in a nice folder
    Function<String,Identifier> texture = suffix -> blockTexture("wood/" + name + "/" + suffix);
    Identifier planks = texture.apply("planks");
    Identifier log = texture.apply("log");
    Identifier stripped = texture.apply("stripped_log");

    // planks and fences
    addFenceBuildingBlock(wood, folder, "planks", planks);
    fenceGate(wood.getFenceGate(), folder + "fence/gate", planks);
    // logs
    axisBlock(wood.getLog(),          folder + "log/log",           log,      true);
    axisBlock(wood.getStrippedLog(),  folder + "log/stripped",      stripped, true);
    axisBlock(wood.getWood(),         folder + "log/wood",          log,      false);
    axisBlock(wood.getStrippedWood(), folder + "log/wood_stripped", stripped, false);
    // doors
    door(wood.getDoor(), folder, texture.apply("door_bottom"), texture.apply("door_top"));
    basicItem(wood.getDoor(), "wood/");
    trapdoor(wood.getTrapdoor(), folder + "trapdoor_", texture.apply("trapdoor"), trapdoorOrientable);
    // redstone
    pressurePlate(wood.getPressurePlate(), folder + "pressure_plate", planks);
    button(wood.getButton(), folder + "button", planks);
    // sign
    Identifier sign = ModelTemplates.PARTICLE_ONLY.create(modelLocation(folder + "sign"), TextureMapping.particle(material(planks)), modelOutput);
    simpleBlock(wood.getSign(), sign);
    simpleBlock(wood.getWallSign(), sign);
    basicItem(wood.getSign(), "wood/");
    Identifier hangingSign = ModelTemplates.PARTICLE_ONLY.create(modelLocation(folder + "hanging_sign"), TextureMapping.particle(material(stripped)), modelOutput);
    simpleBlock(wood.getHangingSign(), hangingSign);
    simpleBlock(wood.getWallHangingSign(), hangingSign);
    basicItem(wood.getHangingSign(), "wood/");
  }


  /* Registry key helpers */

  /** Gets the resource location key for a block */
  private Identifier itemKey(ItemLike item) {
    return BuiltInRegistries.ITEM.getKey(item.asItem());
  }

  /** Gets the item model location for the given item */
  private Identifier itemModel(ItemLike item) {
    return itemKey(item).withPrefix(ITEM_FOLDER + "/");
  }

  /** Creates a model for a generated item with 1 layer */
  protected Identifier basicItem(ItemLike item, String texturePrefix) {
    return basicItem(itemKey(item), texturePrefix);
  }

  /** Creates a model for a generated item with 1 layer */
  protected Identifier basicItem(Identifier item, String texturePrefix) {
    return generated(item, itemTexture(texturePrefix + item.getPath()));
  }

  /** Creates a model for a generated item with 1 layer */
  protected Identifier generated(Identifier item, Identifier texture) {
    return GENERATED.create(item.withPrefix(ITEM_FOLDER + "/"), TextureMapping.layer0(material(texture)), modelOutput);
  }

  /** Creates an item model with the given model as the parent */
  protected Identifier simpleBlockItem(ItemLike item, Identifier model) {
    return template(model).create(itemModel(item), new TextureMapping(), modelOutput);
  }

  /** Creates a simple block state with a single model */
  protected void simpleBlock(Block block, Identifier model) {
    blockState(BlockModelGenerators.createSimpleBlock(block, plainVariant(model)));
  }

  /**
   * Creates a block state with a simple model and an item model
   * @param block   Block
   * @param model   Model to use for the block and item
   * @return model location for the basic block
   */
  public Identifier basicBlock(Block block, Identifier model) {
    simpleBlock(block, model);
    simpleBlockItem(block, model);
    return model;
  }

  /**
   * Creates a model for a cube with the same texture on all sides and an item form
   * @param block     Block
   * @param location  Location for the block model
   * @param texture   Texture for all sides
   * @return model location for the basic block
   */
  public Identifier basicBlock(Block block, String location, Identifier texture) {
    return basicBlock(block, ModelTemplates.CUBE_ALL.create(modelLocation(location), TextureMapping.cube(material(texture)), modelOutput));
  }

  /**
   * Creates a model for a cube with the same texture on all sides and an item form
   * @param block     Block
   * @param location  Location for the block model
   * @param side      Texture for sides
   * @param top       Texture for top
   * @return model location for the basic block
   */
  public Identifier cubeColumn(Block block, String location, Identifier side, Identifier top) {
    return basicBlock(block, ModelTemplates.CUBE_COLUMN.create(modelLocation(location), TextureMapping.column(material(side), material(top)), modelOutput));
  }

  /**
   * Adds a block with axis textures
   * @param block        Block to add, expected to be instance of RotatedPillarBlock
   * @param location     Location for the model
   * @param texture      Side texture
   * @param horizontal   If true, makes a top texture by suffixing the side texture and includes a horizontal model.
   *                     If false, uses the side for the top
   */
  public void axisBlock(Block block, String location, Identifier texture, boolean horizontal) {
    Identifier endTexture = horizontal ? texture.withSuffix("_top") : texture;
    TextureMapping textures = TextureMapping.column(material(texture), material(endTexture));
    Identifier model = ModelTemplates.CUBE_COLUMN.create(modelLocation(location), textures, modelOutput);
    if (horizontal) {
      Identifier horizontalModel = ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(modelLocation(location + "_horizontal"), textures, modelOutput);
      blockState(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(block, plainVariant(model), plainVariant(horizontalModel)));
    } else {
      blockState(BlockModelGenerators.createAxisAlignedPillarBlock(block, plainVariant(model)));
    }
    simpleBlockItem(block, model);
  }

  /** Creates the texture mapping for slabs and stairs */
  private static TextureMapping slabTextures(Identifier sideTexture, Identifier bottomTexture, Identifier topTexture) {
    return new TextureMapping().put(TextureSlot.SIDE, material(sideTexture)).put(TextureSlot.BOTTOM, material(bottomTexture)).put(TextureSlot.TOP, material(topTexture));
  }

  /**
   * Creates block and item model for a slab
   * @param block           Slab block
   * @param location        Location for slab models, top slab will suffix top
   * @param doubleModel     Model for the double slab
   * @param sideTexture     Side texture
   * @param bottomTexture   Bottom texture
   * @param topTexture      Top texture
   */
  public void slab(SlabBlock block, String location, Identifier doubleModel, Identifier sideTexture, Identifier bottomTexture, Identifier topTexture) {
    TextureMapping textures = slabTextures(sideTexture, bottomTexture, topTexture);
    Identifier slab = ModelTemplates.SLAB_BOTTOM.create(modelLocation(location), textures, modelOutput);
    Identifier top = ModelTemplates.SLAB_TOP.create(modelLocation(location + "_top"), textures, modelOutput);
    blockState(BlockModelGenerators.createSlab(block, plainVariant(slab), plainVariant(top), plainVariant(doubleModel)));
    simpleBlockItem(block, slab);
  }

  /**
   * Creates block and item model for stairs
   * @param block           Stairs block
   * @param location        Location for stair models, inner and outer will suffix
   * @param sideTexture     Side texture
   * @param bottomTexture   Bottom texture
   * @param topTexture      Top texture
   */
  public void stairs(StairBlock block, String location, Identifier sideTexture, Identifier bottomTexture, Identifier topTexture) {
    TextureMapping textures = slabTextures(sideTexture, bottomTexture, topTexture);
    Identifier stairs = ModelTemplates.STAIRS_STRAIGHT.create(modelLocation(location), textures, modelOutput);
    Identifier inner = ModelTemplates.STAIRS_INNER.create(modelLocation(location + "_inner"), textures, modelOutput);
    Identifier outer = ModelTemplates.STAIRS_OUTER.create(modelLocation(location + "_outer"), textures, modelOutput);
    blockState(BlockModelGenerators.createStairs(block, plainVariant(inner), plainVariant(stairs), plainVariant(outer)));
    simpleBlockItem(block, stairs);
  }

  /**
   * Adds a fence block with an item model
   * @param block    Fence block
   * @param prefix   Prefix for block files
   * @param texture  Fence texture
   */
  public void fence(FenceBlock block, String prefix, Identifier texture) {
    TextureMapping textures = TextureMapping.defaultTexture(material(texture));
    Identifier post = ModelTemplates.FENCE_POST.create(modelLocation(prefix + "post"), textures, modelOutput);
    Identifier side = ModelTemplates.FENCE_SIDE.create(modelLocation(prefix + "side"), textures, modelOutput);
    blockState(BlockModelGenerators.createFence(block, plainVariant(post), plainVariant(side)));
    template(Identifier.withDefaultNamespace("block/fence_inventory"), TextureSlot.TEXTURE).create(itemModel(block), textures, modelOutput);
  }

  /**
   * Adds a fence gate block with an item model
   * @param block     Fence gate block
   * @param baseName  Location for the closed gate model, other models are suffixed
   * @param texture   Fence gate texture
   */
  public void fenceGate(FenceGateBlock block, String baseName, Identifier texture) {
    TextureMapping textures = TextureMapping.defaultTexture(material(texture));
    Identifier model = ModelTemplates.FENCE_GATE_CLOSED.create(modelLocation(baseName), textures, modelOutput);
    Identifier open = ModelTemplates.FENCE_GATE_OPEN.create(modelLocation(baseName + "_open"), textures, modelOutput);
    Identifier wall = ModelTemplates.FENCE_GATE_WALL_CLOSED.create(modelLocation(baseName + "_wall"), textures, modelOutput);
    Identifier wallOpen = ModelTemplates.FENCE_GATE_WALL_OPEN.create(modelLocation(baseName + "_wall_open"), textures, modelOutput);
    blockState(BlockModelGenerators.createFenceGate(block, plainVariant(open), plainVariant(model), plainVariant(wallOpen), plainVariant(wall), true));
    simpleBlockItem(block, model);
  }

  /**
   * Adds a door block without an item model
   * @param block           Door block
   * @param prefix          Prefix for model files
   * @param bottomTexture   Bottom door texture
   * @param topTexture      Top door texture
   */
  public void door(DoorBlock block, String prefix, Identifier bottomTexture, Identifier topTexture) {
    TextureMapping textures = TextureMapping.door(material(topTexture), material(bottomTexture));
    blockState(BlockModelGenerators.createDoor(
      block,
      plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT.create(     modelLocation(prefix + "door/bottom_left"),       textures, modelOutput)),
      plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.create(modelLocation(prefix + "door/bottom_left_open"),  textures, modelOutput)),
      plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT.create(    modelLocation(prefix + "door/bottom_right"),      textures, modelOutput)),
      plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.create(modelLocation(prefix + "door/bottom_right_open"), textures, modelOutput)),
      plainVariant(ModelTemplates.DOOR_TOP_LEFT.create(        modelLocation(prefix + "door/top_left"),          textures, modelOutput)),
      plainVariant(ModelTemplates.DOOR_TOP_LEFT_OPEN.create(   modelLocation(prefix + "door/top_left_open"),     textures, modelOutput)),
      plainVariant(ModelTemplates.DOOR_TOP_RIGHT.create(       modelLocation(prefix + "door/top_right"),         textures, modelOutput)),
      plainVariant(ModelTemplates.DOOR_TOP_RIGHT_OPEN.create(  modelLocation(prefix + "door/top_right_open"),    textures, modelOutput))
    ));
  }

  /**
   * Adds a trapdoor block with an item model
   * @param block    Trapdoor block
   * @param prefix   Model location prefix
   * @param texture  Trapdoor texture
   * @param orientable  If true, it's an oriented model.
   */
  public void trapdoor(TrapDoorBlock block, String prefix, Identifier texture, boolean orientable) {
    TextureMapping textures = TextureMapping.defaultTexture(material(texture));
    Identifier bottom, top, open;
    if (orientable) {
      bottom = ModelTemplates.ORIENTABLE_TRAPDOOR_BOTTOM.create(modelLocation(prefix + "bottom"), textures, modelOutput);
      top = ModelTemplates.ORIENTABLE_TRAPDOOR_TOP.create(modelLocation(prefix + "top"), textures, modelOutput);
      open = ModelTemplates.ORIENTABLE_TRAPDOOR_OPEN.create(modelLocation(prefix + "open"), textures, modelOutput);
      blockState(BlockModelGenerators.createOrientableTrapdoor(block, plainVariant(top), plainVariant(bottom), plainVariant(open)));
    } else {
      bottom = ModelTemplates.TRAPDOOR_BOTTOM.create(modelLocation(prefix + "bottom"), textures, modelOutput);
      top = ModelTemplates.TRAPDOOR_TOP.create(modelLocation(prefix + "top"), textures, modelOutput);
      open = ModelTemplates.TRAPDOOR_OPEN.create(modelLocation(prefix + "open"), textures, modelOutput);
      blockState(BlockModelGenerators.createTrapdoor(block, plainVariant(top), plainVariant(bottom), plainVariant(open)));
    }
    simpleBlockItem(block, bottom);
  }

  /**
   * Adds a pressure plate with item model
   * @param block     Pressure plate block
   * @param location  Location for the model, pressed will be the location suffixed with down
   * @param texture   Texture for the plate
   */
  public void pressurePlate(PressurePlateBlock block, String location, Identifier texture) {
    TextureMapping textures = TextureMapping.defaultTexture(material(texture));
    Identifier pressurePlate = ModelTemplates.PRESSURE_PLATE_UP.create(modelLocation(location), textures, modelOutput);
    Identifier down = ModelTemplates.PRESSURE_PLATE_DOWN.create(modelLocation(location + "_down"), textures, modelOutput);
    blockState(BlockModelGenerators.createPressurePlate(block, plainVariant(pressurePlate), plainVariant(down)));
    simpleBlockItem(block, pressurePlate);
  }

  /**
   * Adds a button with item model
   * @param block     Button block
   * @param location  Location for the model, pressed will be the location suffixed with down
   * @param texture   Texture for the button
   */
  public void button(ButtonBlock block, String location, Identifier texture) {
    TextureMapping textures = TextureMapping.defaultTexture(material(texture));
    Identifier button = ModelTemplates.BUTTON.create(modelLocation(location), textures, modelOutput);
    Identifier pressed = ModelTemplates.BUTTON_PRESSED.create(modelLocation(location + "_pressed"), textures, modelOutput);
    blockState(BlockModelGenerators.createButton(block, plainVariant(button), plainVariant(pressed)));
    template(Identifier.withDefaultNamespace("block/button_inventory"), TextureSlot.TEXTURE).create(itemModel(block), textures, modelOutput);
  }


  /* Geode */

  /** Adds a model with rotations for a bud block */
  public void bud(Block block, String location, Identifier texture) {
    Identifier bud = ModelTemplates.CROSS.create(modelLocation(location), TextureMapping.cross(material(texture)), modelOutput);
    blockState(MultiVariantGenerator.dispatch(block, plainVariant(bud)).with(ROTATIONS_COLUMN_WITH_FACING));
  }

  /** Adds all models for a geode */
  public void geode(GeodeItemObject geode, String type) {
    basicItem(geode, "materials/");
    for (BudSize size : BudSize.values()) {
      String name = size.getName();
      if (size != BudSize.CLUSTER) {
        name += "_bud";
      }
      Block bud = geode.getBud(size);
      Identifier texture = blockTexture("geode/" + type + '/' + name);
      bud(bud, "block/geode/" + type + '/' + name, texture);
      template(getResource("item/base/geode/" + name), TextureSlot.LAYER0).create(itemModel(bud), TextureMapping.layer0(material(texture)), modelOutput);
    }
    basicBlock(geode.getBlock(), "block/geode/" + type + "/block", blockTexture("geode/" + type + "/block"));
    basicBlock(geode.getBudding(), "block/geode/" + type + "/budding", blockTexture("geode/" + type + "/budding"));
  }


  /* Panes and glass */

  /** Creates a pane model using the TConstruct templates */
  private Identifier paneModel(String baseName, String variant, Identifier pane, @Nullable Identifier edge, boolean connected, int tint) {
    TextureMapping textures = new TextureMapping().put(PANE, material(pane));
    ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder()
      .parent(getResource("block/template/pane/" + variant))
      .requiredTextureSlot(PANE);
    if (edge != null) {
      textures.put(EDGE, material(edge));
      builder.requiredTextureSlot(EDGE);
    }
    if (connected) {
      builder.customLoader(ConnectedModelBuilder::new, loader -> {
        loader.connected("pane", "cornerless_full").setPredicate("pane");
        if (tint != -1) {
          loader.color(tint);
        }
      });
    } else if (tint != -1) {
      builder.customLoader(ColoredModelBuilder::new, loader -> loader.color(tint));
    }
    return builder.build().create(modelLocation(BLOCK_FOLDER + "/" + baseName + variant), textures, modelOutput);
  }

  /** Creates the block state variant for a pane model, connected models need the {@code mantle:connected} block state type */
  private static MultiVariant paneVariant(Identifier model, boolean connected, VariantMutator rotation) {
    MultiVariant variant = connected ? VariantBlockStateModelBuilder.connectedVariant(model) : plainVariant(model);
    return rotation == NOP ? variant : variant.with(rotation);
  }

  /** Gets the Y rotation mutator for the given angle */
  private static VariantMutator yRot(int angle) {
    return switch (angle) {
      case 90 -> Y_ROT_90;
      case 180 -> Y_ROT_180;
      case 270 -> Y_ROT_270;
      default -> NOP;
    };
  }

  /**
   * Creates a pane block state, recreation of NeoForge's pane block state builder from 1.21.1
   * @param noSideEdge  Model for the edge when no side connects, null to skip
   */
  private void paneBlockState(IronBarsBlock block, boolean connected, Identifier post, Identifier side, Identifier sideAlt, Identifier noSide, Identifier noSideAlt, @Nullable Identifier noSideEdge) {
    MultiPartGenerator builder = MultiPartGenerator.multiPart(block).with(paneVariant(post, connected, NOP));
    PipeBlock.PROPERTY_BY_DIRECTION.forEach((dir, value) -> {
      if (dir.getAxis().isHorizontal()) {
        boolean alt = dir == Direction.SOUTH;
        builder.with(condition().term(value, true), paneVariant(alt || dir == Direction.WEST ? sideAlt : side, connected, yRot(dir.getAxis() == Axis.X ? 90 : 0)));
        builder.with(condition().term(value, false), paneVariant(alt || dir == Direction.EAST ? noSideAlt : noSide, connected, yRot(dir == Direction.WEST ? 270 : dir == Direction.SOUTH ? 90 : 0)));
        if (noSideEdge != null) {
          BooleanProperty clockwise = PipeBlock.PROPERTY_BY_DIRECTION.get(dir.getClockWise());
          BooleanProperty counterClockwise = PipeBlock.PROPERTY_BY_DIRECTION.get(dir.getCounterClockWise());
          // the edge model is never connected
          builder.with(condition().term(value, false).term(clockwise, false).term(counterClockwise, false), paneVariant(noSideEdge, false, yRot((int)dir.getOpposite().toYRot())));
        }
      }
    });
    blockState(builder);
  }

  /** Creates a new pane block with all relevant models */
  public void paneBlock(IronBarsBlock block, String baseName, Identifier pane, Identifier edge, boolean connected, int tint, boolean solidEdge) {
    // build block models
    Identifier post      = paneModel(baseName, "post",       pane, edge, connected, tint);
    Identifier side      = paneModel(baseName, "side",       pane, edge, connected, tint);
    Identifier sideAlt   = paneModel(baseName, "side_alt",   pane, edge, connected, tint);
    Identifier noSide    = paneModel(baseName, "noside",     pane, null, connected, tint);
    Identifier noSideAlt = paneModel(baseName, "noside_alt", pane, null, connected, tint);
    Identifier noSideEdge = null;
    if (solidEdge && !pane.equals(edge)) {
      noSideEdge = paneModel(baseName, "noside_edge", pane, edge, false, tint);
    }
    paneBlockState(block, connected, post, side, sideAlt, noSide, noSideAlt, noSideEdge);
    // build item model
    ModelTemplate item = GENERATED;
    if (tint != -1) {
      item = GENERATED.extend().customLoader(MantleItemLayerBuilder::new, loader -> loader.color(tint)).build();
    }
    item.create(itemModel(block), TextureMapping.layer0(material(pane)), modelOutput);
  }

  /** Adds models for a glass block with a glass pane */
  public void glassBlock(Block glass, IronBarsBlock pane, String baseName, Identifier front, int tint, boolean solidEdge) {
    glassBlock(glass, pane, baseName, front, front.withSuffix("_top"), tint, solidEdge);
  }

  /** Adds models for a glass block with a glass pane */
  public void glassBlock(Block glass, IronBarsBlock pane, String baseName, Identifier front, Identifier edge, int tint, boolean solidEdge) {
    // make block model
    Identifier block = ModelTemplates.CUBE_ALL.extend().customLoader(ConnectedModelBuilder::new, loader -> {
      loader.connected("all", "cornerless_full");
      if (tint != -1) {
        loader.color(tint);
      }
    }).build().create(modelLocation(BLOCK_FOLDER + "/" + baseName + "block"), TextureMapping.cube(material(front)), modelOutput);
    // connected textures need the connected block state type
    blockState(BlockModelGenerators.createSimpleBlock(glass, VariantBlockStateModelBuilder.connectedVariant(block)));
    simpleBlockItem(glass, block);
    // make pane models
    paneBlock(pane, baseName + "pane_", front, edge, true, tint, solidEdge);
  }
}
