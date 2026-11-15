package slimeknights.tconstruct.library.client.model.block;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4fc;
import slimeknights.mantle.client.model.RetexturedModel.RetexturedContext;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel.ColorData;
import slimeknights.mantle.client.model.util.GeometryContextWrapper;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.ModelTextureIteratable;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.client.model.UniqueGuiModel;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * This model contains a single scalable fluid that can either be statically rendered or rendered in the TESR. It also supports rendering fluids in the item model.
 * The model JSON uses the loader {@code tconstruct:tank}. To display the fluid in the block model, the blockstate needs the type {@code tconstruct:tank} ({@link #BLOCK_STATE_MODEL}),
 * and to display it on items the client item JSON needs the type {@code tconstruct:tank_fluid} ({@link #ITEM_MODEL}). Without them the model renders statically.
 */
public class TankModel extends SimpleBlockModel {
  /** Shared loader instance */
  public static final UnbakedModelLoader<TankModel> LOADER = TankModel::deserialize;
  /** ID of the block state model type */
  public static final Identifier BLOCK_STATE_ID = TConstruct.getResource("tank");
  /** ID of the item model type */
  public static final Identifier ITEM_ID = TConstruct.getResource("tank_fluid");

  @Nullable
  protected final SimpleBlockModel gui;
  protected final IncrementalFluidCuboid fluid;
  protected final boolean forceModelFluid;

  public TankModel(SimpleBlockModel model, @Nullable SimpleBlockModel gui, IncrementalFluidCuboid fluid, boolean forceModelFluid) {
    super(model);
    this.gui = gui;
    this.fluid = fluid;
    this.forceModelFluid = forceModelFluid;
  }

  @Override
  public void resolveDependencies(ResolvableModel.Resolver resolver) {
    super.resolveDependencies(resolver);
    // the nested model is not part of the parent chain, so its parent needs to be marked
    if (gui != null) {
      Identifier guiParent = gui.parent();
      if (guiParent != null) {
        resolver.markDependency(guiParent);
      }
      gui.resolveDependencies(resolver);
    }
  }

  /** Finds the tank model in the given chain */
  @Nullable
  public static TankModel find(@Nullable ResolvedModel model) {
    return SimpleBlockModel.findModel(model, TankModel.class);
  }

  /**
   * Creates the logic to bake this model with fluids
   * @param baker     Model baker
   * @param resolved  Resolved model containing this model
   * @param state     Model state without the root transform
   * @param useGui    If true, bakes the GUI model elements instead of the main elements
   * @return  Fluid baker
   */
  private FluidBaker createBaker(ModelBaker baker, ResolvedModel resolved, ModelState state, boolean useGui) {
    ModelBaker dynamicBaker = ModelHelper.dynamicBaker(baker);
    String debugName = resolved.debugName();
    List<CuboidModelElement> elements;
    TextureSlots slots;
    if (useGui && gui != null) {
      // gui textures fall back to the main model textures, matching the shared geometry context of the old model
      ModelDebugName guiName = () -> debugName + "#gui";
      ResolvedModel guiModel = baker.resolveInlineModel(gui, guiName);
      TextureSlots.Resolver resolver = new TextureSlots.Resolver();
      for (TextureSlots.Data data : ModelTextureIteratable.of(guiModel)) {
        resolver.addLast(data);
      }
      for (TextureSlots.Data data : ModelTextureIteratable.of(resolved)) {
        resolver.addLast(data);
      }
      slots = resolver.resolve(guiName);
      elements = gui.getElements(guiModel);
    } else {
      slots = resolved.getTopTextureSlots();
      elements = getElements(resolved);
    }
    return new FluidBaker(dynamicBaker, elements, new GeometryContextWrapper(dynamicBaker, slots, () -> debugName),
                          ModelHelper.applyRootTransform(state, resolved.getTopAdditionalProperties()), fluid, resolved.getTopAmbientOcclusion());
  }

  /** Cache key since fluids don't do equality over amount */
  private record CacheKey(Fluid fluid, DataComponentPatch components, int increments) {}

  /** Gets the cache key for the given fluid */
  private static CacheKey cacheKey(IncrementalFluidCuboid cuboid, FluidStack fluid, int capacity) {
    int increments = cuboid.getIncrements();
    return new CacheKey(fluid.getFluid(), fluid.getComponentsPatch(), Mth.clamp(fluid.getAmount() * increments / capacity, 1, increments));
  }

  /** Logic to bake the model with a fluid element */
  private record FluidBaker(ModelBaker baker, List<CuboidModelElement> elements, GeometryContextWrapper materials, ModelState transform, IncrementalFluidCuboid cuboid, boolean ambientOcclusion) {
    /** Bakes just the model elements without a fluid */
    private BlockStateModelPart bakeElements() {
      QuadCollection.Builder builder = new QuadCollection.Builder();
      for (CuboidModelElement element : elements) {
        SimpleBlockModel.bakePart(builder, baker, element, materials, transform);
      }
      return new SimpleModelWrapper(builder.build(), ambientOcclusion, materials.getMaterial("particle"));
    }

    /**
     * Bakes the model with the given fluid element
     * @param stack       Fluid to display
     * @param increments  Scaled fluid amount
     * @param useLight    If true, applies the fluid light level to the fluid. Disabled in the GUI model
     * @return  Baked model
     */
    private BlockStateModelPart bake(FluidStack stack, int increments, boolean useLight) {
      // fetch fluid data
      FluidSprites sprites = FluidSprites.of(stack);
      FluidType type = stack.getFluid().getFluidType();
      int luminosity = useLight ? type.getLightLevel(stack) : 0;
      GeometryContextWrapper textured = new RetexturedContext(new RetexturedContext(materials, Set.of("fluid"), new Material.Baked(sprites.still(), false)),
                                                              Set.of("flowing_fluid"), new Material.Baked(sprites.flowing(), false));

      QuadCollection.Builder builder = new QuadCollection.Builder();
      // first, add all regular elements
      for (CuboidModelElement element : elements) {
        SimpleBlockModel.bakePart(builder, baker, element, materials, transform);
      }
      // next, add in the fluid
      CuboidModelElement fluid = cuboid.getPart(increments, type.isLighterThanAir());
      ColoredBlockModel.bakePart(builder, baker, fluid, new ColorData(sprites.color(), luminosity, null), textured, transform);
      return new SimpleModelWrapper(builder.build(), ambientOcclusion, materials.getMaterial("particle"));
    }
  }

  /** Gets all quads from the part as unculled quads, for item models */
  private static QuadCollection toItemQuads(BlockStateModelPart part) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    for (BakedQuad quad : part.getQuads(null)) {
      builder.addUnculledFace(quad);
    }
    for (Direction direction : Direction.values()) {
      for (BakedQuad quad : part.getQuads(direction)) {
        builder.addUnculledFace(quad);
      }
    }
    return builder.build();
  }


  /* Block state model */

  /** Block state model adding the fluid part from model data */
  private static class Baked implements DynamicBlockStateModel {
    private final Cache<CacheKey,BlockStateModelPart> cache = CacheBuilder
      .newBuilder()
      .maximumSize(64)
      .build();
    private final BlockStateModelPart base;
    private final FluidBaker baker;
    private final boolean forceModelFluid;

    private Baked(BlockStateModelPart base, FluidBaker baker, boolean forceModelFluid) {
      this.base = base;
      this.baker = baker;
      this.forceModelFluid = forceModelFluid;
    }

    /** Gets the fluid to render from model data, or empty if no fluid should render */
    private FluidStack getFluid(ModelData data) {
      if (forceModelFluid || Config.CLIENT.tankFluidModel.get()) {
        FluidStack fluid = data.get(ModelProperties.FLUID_STACK);
        if (fluid != null && !fluid.isEmpty()) {
          return fluid;
        }
      }
      return FluidStack.EMPTY;
    }

    /** Gets the part to render for the given position */
    private BlockStateModelPart getPart(BlockAndTintGetter level, BlockPos pos) {
      ModelData data = level.getModelData(pos);
      FluidStack fluid = getFluid(data);
      if (!fluid.isEmpty()) {
        int capacity = Objects.requireNonNullElse(data.get(ModelProperties.TANK_CAPACITY), fluid.getAmount());
        CacheKey key = cacheKey(baker.cuboid, fluid, capacity);
        try {
          return cache.get(key, () -> baker.bake(fluid, key.increments, true));
        } catch (ExecutionException e) {
          TConstruct.LOG.error(e);
        }
      }
      return base;
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      ModelData data = level.getModelData(pos);
      FluidStack fluid = getFluid(data);
      if (fluid.isEmpty()) {
        return new GeometryKey(this, null);
      }
      int capacity = Objects.requireNonNullElse(data.get(ModelProperties.TANK_CAPACITY), fluid.getAmount());
      return new GeometryKey(this, cacheKey(baker.cuboid, fluid, capacity));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
      parts.add(getPart(level, pos));
    }

    @Override
    public Material.Baked particleMaterial() {
      return base.particleMaterial();
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags() {
      return base.materialFlags();
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      return getPart(level, pos).materialFlags();
    }
  }

  /** Key for geometry caching */
  private record GeometryKey(Baked model, @Nullable CacheKey fluid) {}

  /** Unbaked block state model, parsed from blockstate JSON as a variant with {@code "type": "tconstruct:tank"} */
  public record BlockStateUnbaked(Variant variant) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<BlockStateUnbaked> CODEC = Variant.MAP_CODEC.xmap(BlockStateUnbaked::new, BlockStateUnbaked::variant);

    @Override
    public MapCodec<BlockStateUnbaked> codec() {
      return CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      variant.resolveDependencies(resolver);
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
      ResolvedModel resolved = baker.getModel(variant.modelLocation());
      ModelState state = variant.modelState().asModelState();
      BlockStateModelPart base = SimpleModelWrapper.bake(baker, resolved, state);
      TankModel model = find(resolved);
      if (model == null) {
        TConstruct.LOG.warn("Model {} used with blockstate type {} does not use the tank loader, fluids will not display", variant.modelLocation(), BLOCK_STATE_ID);
        return new SingleVariant(base);
      }
      // model was rejected by vanilla, no sense adding fluids to the missing model
      if (base == baker.missingBlockModelPart()) {
        return new SingleVariant(base);
      }
      return new Baked(base, model.createBaker(baker, resolved, state, false), model.forceModelFluid);
    }
  }
  /** Codec for the block state model type */
  public static final MapCodec<? extends CustomUnbakedBlockStateModel> BLOCK_STATE_MODEL = BlockStateUnbaked.CODEC;


  /* Item model */

  /**
   * Unbaked item model adding the fluid from the tank item, used as {@code {"type": "tconstruct:tank_fluid", "model": "tconstruct:block/smeltery/seared/fuel_tank"}}.
   * Also swaps to the GUI model in the GUI if the tank model defines one.
   * @param model           Model location
   * @param transformation  Extra transformation
   */
  public record ItemUnbaked(Identifier model, Optional<Transformation> transformation) implements ItemModel.Unbaked {
    public static final MapCodec<ItemUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Identifier.CODEC.fieldOf("model").forGetter(ItemUnbaked::model),
      Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(ItemUnbaked::transformation)
    ).apply(inst, ItemUnbaked::new));

    @Override
    public MapCodec<ItemUnbaked> type() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
      ModelBaker baker = context.blockModelBaker();
      ResolvedModel resolved = baker.getModel(model);
      TextureSlots textureSlots = resolved.getTopTextureSlots();
      ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, textureSlots);
      Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
      ItemModel base = new CuboidItemModelWrapper(List.of(), resolved.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY), properties, modelTransform);
      TankModel tankModel = find(resolved);
      if (tankModel == null) {
        TConstruct.LOG.warn("Model {} used with item model type {} does not use the tank loader, fluids will not display", model, ITEM_ID);
        return base;
      }
      // bake the GUI model if present
      ItemModel gui = base;
      FluidBaker guiBaker = null;
      if (tankModel.gui != null) {
        guiBaker = tankModel.createBaker(baker, resolved, BlockModelRotation.IDENTITY, true);
        gui = new CuboidItemModelWrapper(List.of(), toItemQuads(guiBaker.bakeElements()), properties, modelTransform);
      }
      return new TankItemModel(base, gui, tankModel.createBaker(baker, resolved, BlockModelRotation.IDENTITY, false), guiBaker, properties, modelTransform);
    }
  }
  /** Codec for the item model type */
  public static final MapCodec<? extends ItemModel.Unbaked> ITEM_MODEL = ItemUnbaked.MAP_CODEC;

  /** Item model adding the fluid part to the item model */
  private static class TankItemModel implements ItemModel {
    private final Cache<CacheKey,ItemModel> cache = CacheBuilder
      .newBuilder()
      .maximumSize(64)
      .build();
    private final ItemModel base;
    private final ItemModel gui;
    private final FluidBaker baker;
    @Nullable
    private final FluidBaker guiBaker;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    private TankItemModel(ItemModel base, ItemModel gui, FluidBaker baker, @Nullable FluidBaker guiBaker, ModelRenderProperties properties, Matrix4fc transformation) {
      this.base = base;
      this.gui = gui;
      this.baker = baker;
      this.guiBaker = guiBaker;
      this.properties = properties;
      this.transformation = transformation;
    }

    /** Creates an item model for the given part */
    private ItemModel toItemModel(BlockStateModelPart part) {
      return new CuboidItemModelWrapper(List.of(), toItemQuads(part), properties, transformation);
    }

    /** Gets the model with the fluid part added */
    private ItemModel getModel(FluidStack fluid, CacheKey key) {
      ItemModel baked = toItemModel(baker.bake(fluid, key.increments, true));
      // if we have GUI, bake a GUI variant
      if (guiBaker != null) {
        baked = new UniqueGuiModel.Baked(baked, toItemModel(guiBaker.bake(fluid, key.increments, false)));
      }
      return baked;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      ItemModel model = displayContext == ItemDisplayContext.GUI ? gui : base;
      // determine fluid
      FluidStack fluid = TankItem.getFluid(stack);
      if (!fluid.isEmpty()) {
        CacheKey key = cacheKey(baker.cuboid, fluid, TankBlockEntity.getCapacity(stack.getItem()));
        try {
          model = cache.get(key, () -> getModel(fluid, key));
        } catch (ExecutionException e) {
          TConstruct.LOG.error(e);
        }
      }
      model.update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }


  /** Loader for this model */
  public static TankModel deserialize(JsonObject json, JsonDeserializationContext context) {
    SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
    SimpleBlockModel gui = null;
    if (json.has("gui")) {
      gui = SimpleBlockModel.deserialize(GsonHelper.getAsJsonObject(json, "gui"), context);
    }
    IncrementalFluidCuboid fluid = IncrementalFluidCuboid.fromJson(GsonHelper.getAsJsonObject(json, "fluid"));
    boolean forceModelFluid = GsonHelper.getAsBoolean(json, "render_fluid_in_model", false);
    return new TankModel(model, gui, fluid, forceModelFluid);
  }
}
