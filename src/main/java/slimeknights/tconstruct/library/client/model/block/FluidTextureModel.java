package slimeknights.tconstruct.library.client.model.block;

import com.google.common.collect.ImmutableSet;
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
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4fc;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.RetexturedModel.RetexturedContext;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.GeometryContextWrapper;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.model.ModelProperties;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model that replaces fluid textures with the fluid from model data, and optionally retextures parts with the block from model data.
 * The model JSON uses the loader {@code tconstruct:fluid_texture}, the blockstate needs the type {@code tconstruct:fluid_texture} ({@link #BLOCK_STATE_MODEL}) to swap the fluid,
 * and the client item JSON uses the type {@code tconstruct:fluid_texture} ({@link #ITEM_MODEL}) to retexture items. Without those types, the model renders statically.
 */
public class FluidTextureModel extends ColoredBlockModel {
  /** Loader instance */
  public static final UnbakedModelLoader<FluidTextureModel> LOADER = FluidTextureModel::deserialize;
  /** ID of the blockstate and item model types */
  public static final Identifier ID = TConstruct.getResource("fluid_texture");

  private final Set<String> fluids;
  private final Set<String> retextured;

  public FluidTextureModel(ColoredBlockModel model, Set<String> fluids, Set<String> retextured) {
    super(model);
    this.fluids = fluids;
    this.retextured = retextured;
  }

  /** Finds the model in the given model chain */
  @Nullable
  public static FluidTextureModel find(@Nullable ResolvedModel model) {
    return SimpleBlockModel.findModel(model, FluidTextureModel.class);
  }

  /**
   * Creates the logic to rebake this model with fluids and textures
   * @param baker     Model baker
   * @param resolved  Resolved model containing this model
   * @param state     Model state, without the root transform
   * @return  Rebake logic
   */
  private Rebaker createRebaker(ModelBaker baker, ResolvedModel resolved, ModelState state) {
    ModelBaker dynamicBaker = ModelHelper.dynamicBaker(baker);
    String debugName = resolved.debugName();
    GeometryContextWrapper materials = new GeometryContextWrapper(dynamicBaker, resolved.getTopTextureSlots(), () -> debugName);

    // determine which block parts are fluids
    Set<String> fluidTextures = this.fluids.isEmpty() ? Collections.emptySet() : RetexturedModel.getAllRetextured(resolved, this.fluids);
    List<CuboidModelElement> elements = getElements(resolved);
    int size = elements.size();
    BitSet fluidParts = new BitSet(size);
    if (!fluidTextures.isEmpty()) {
      for (int i = 0; i < size; i++) {
        CuboidModelElement part = elements.get(i);
        long fluidFaces = part.faces().values().stream()
                              .map(CuboidFace::texture)
                              .filter(texture -> fluidTextures.contains(ModelHelper.stripReference(texture)))
                              .count();
        // for simplicity, each part is either a fluid or not. If for some reason it contains both we mark it as a fluid, meaning it may get colored
        // if this is undesired, just use separate elements
        if (fluidFaces > 0) {
          if (fluidFaces < part.faces().size()) {
            TConstruct.LOG.warn("Mixed fluid and non-fluid elements in model {}, may cause unexpected results", debugName);
          }
          fluidParts.set(i);
        }
      }
    }
    Set<String> retextured = this.retextured.isEmpty() ? Collections.emptySet() : RetexturedModel.getAllRetextured(resolved, this.retextured);
    return new Rebaker(dynamicBaker, elements, getColorData(), materials, ModelHelper.applyRootTransform(state, resolved.getTopAdditionalProperties()),
                       fluidTextures, fluidParts, retextured, resolved.getTopAmbientOcclusion());
  }

  /** Logic to rebake the model with the given fluid and texture */
  private record Rebaker(ModelBaker baker, List<CuboidModelElement> elements, List<ColorData> colorData, GeometryContextWrapper materials, ModelState transform,
                         Set<String> fluids, BitSet fluidParts, Set<String> retextured, boolean ambientOcclusion) {
    /** Retextures a model for the given fluid and texture */
    private BlockStateModelPart bake(FluidStack fluid, @Nullable Material.Baked texture) {
      // if textured, retexture. Its fine to nest these configurations
      GeometryContextWrapper textured = materials;
      if (texture != null) {
        textured = new RetexturedContext(textured, retextured, texture);
      }

      // get fluid details if needed
      int luminosity = 0;
      int color = -1;
      if (!fluid.isEmpty()) {
        FluidSprites sprites = FluidSprites.of(fluid);
        color = sprites.color();
        luminosity = fluid.getFluid().getFluidType().getLightLevel(fluid);
        textured = new RetexturedContext(textured, fluids, new Material.Baked(sprites.still(), false));
      }

      // start baking, fluid parts use the fluid color and light while other parts use their own color data
      QuadCollection.Builder builder = new QuadCollection.Builder();
      int size = elements.size();
      for (int i = 0; i < size; i++) {
        CuboidModelElement element = elements.get(i);
        ColorData colors = ColoredBlockModel.getColorData(colorData, i);
        if (fluidParts.get(i)) {
          colors = new ColorData(color, luminosity, colors.uvlock());
        }
        ColoredBlockModel.bakePart(builder, baker, element, colors, textured, transform);
      }
      return new SimpleModelWrapper(builder.build(), ambientOcclusion, textured.getMaterial("particle"));
    }
  }

  /** Cache key for a fluid and texture */
  private record BakedCacheKey(@Nullable Fluid fluid, @Nullable DataComponentPatch components, @Nullable Material.Baked texture) {}

  /** Block state model swapping fluid and textures */
  private static class Baked implements DynamicBlockStateModel {
    private final Map<BakedCacheKey,BlockStateModelPart> cache = new ConcurrentHashMap<>();
    private final BlockStateModelPart base;
    private final Rebaker rebaker;

    private Baked(BlockStateModelPart base, Rebaker rebaker) {
      this.base = base;
      this.rebaker = rebaker;
    }

    /** Gets the fluid from model data */
    private FluidStack getFluid(ModelData data) {
      if (rebaker.fluids.isEmpty()) {
        return FluidStack.EMPTY;
      }
      FluidStack fluid = data.get(ModelProperties.FLUID_STACK);
      return fluid == null ? FluidStack.EMPTY : fluid;
    }

    /** Gets the texture block from model data */
    @Nullable
    private Block getBlock(ModelData data) {
      return rebaker.retextured.isEmpty() ? null : data.get(RetexturedHelper.BLOCK_PROPERTY);
    }

    /** Gets the part for the given level position */
    private BlockStateModelPart getPart(BlockAndTintGetter level, BlockPos pos) {
      ModelData data = level.getModelData(pos);
      FluidStack fluid = getFluid(data);
      Block block = getBlock(data);
      if (!fluid.isEmpty() || block != null) {
        Material.Baked texture = block != null ? ModelHelper.getParticleMaterial(block) : null;
        BakedCacheKey key = fluid.isEmpty() ? new BakedCacheKey(null, null, texture) : new BakedCacheKey(fluid.getFluid(), fluid.getComponentsPatch(), texture);
        return cache.computeIfAbsent(key, k -> rebaker.bake(fluid, texture));
      }
      return base;
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      ModelData data = level.getModelData(pos);
      FluidStack fluid = getFluid(data);
      return new GeometryKey(this, fluid.isEmpty() ? null : fluid.getFluid(), fluid.isEmpty() ? null : fluid.getComponentsPatch(), getBlock(data));
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
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      return getPart(level, pos).particleMaterial();
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
  private record GeometryKey(Baked model, @Nullable Fluid fluid, @Nullable DataComponentPatch components, @Nullable Block texture) {}

  /** Unbaked block state model, parsed from blockstate JSON as a variant with {@code "type": "tconstruct:fluid_texture"} */
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
      FluidTextureModel model = find(resolved);
      if (model == null) {
        TConstruct.LOG.warn("Model {} used with blockstate type {} does not use the fluid texture loader, fluids will not display", variant.modelLocation(), ID);
        return new SingleVariant(base);
      }
      // model was rejected by vanilla, no sense retexturing the missing model
      if (base == baker.missingBlockModelPart()) {
        return new SingleVariant(base);
      }
      return new Baked(base, model.createRebaker(baker, resolved, state));
    }
  }
  /** Codec for the block state model type */
  public static final MapCodec<? extends CustomUnbakedBlockStateModel> BLOCK_STATE_MODEL = BlockStateUnbaked.CODEC;

  /**
   * Unbaked item model retexturing the model using the texture on the stack, used as {@code {"type": "tconstruct:fluid_texture", "model": "tconstruct:block/smeltery/io/drain"}}
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
      FluidTextureModel fluidModel = find(resolved);
      if (fluidModel == null || fluidModel.retextured.isEmpty()) {
        if (fluidModel == null) {
          TConstruct.LOG.warn("Model {} used with item model type {} does not use the fluid texture loader, it will not be retextured", model, ID);
        }
        return base;
      }
      return new RetexturedItem(base, fluidModel.createRebaker(baker, resolved, BlockModelRotation.IDENTITY), properties, modelTransform);
    }
  }
  /** Codec for the item model type */
  public static final MapCodec<? extends ItemModel.Unbaked> ITEM_MODEL = ItemUnbaked.MAP_CODEC;

  /** Item model swapping the texture in from the stack */
  private static class RetexturedItem implements ItemModel {
    private final Map<Material.Baked,ItemModel> cache = new ConcurrentHashMap<>();
    private final ItemModel base;
    private final Rebaker rebaker;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    private RetexturedItem(ItemModel base, Rebaker rebaker, ModelRenderProperties properties, Matrix4fc transformation) {
      this.base = base;
      this.rebaker = rebaker;
      this.properties = properties;
      this.transformation = transformation;
    }

    /** Bakes the item model for the given texture */
    private ItemModel bake(Material.Baked texture) {
      BlockStateModelPart part = rebaker.bake(FluidStack.EMPTY, texture);
      QuadCollection.Builder builder = new QuadCollection.Builder();
      for (BakedQuad quad : part.getQuads(null)) {
        builder.addUnculledFace(quad);
      }
      for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
        for (BakedQuad quad : part.getQuads(direction)) {
          builder.addUnculledFace(quad);
        }
      }
      return new CuboidItemModelWrapper(List.of(), builder.build(), new ModelRenderProperties(properties.usesBlockLight(), part.particleMaterial(), properties.transforms()), transformation);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      // get the block first, ensuring its valid
      Block block = RetexturedHelper.getTexture(stack);
      ItemModel model = base;
      if (block != Blocks.AIR) {
        model = cache.computeIfAbsent(ModelHelper.getParticleMaterial(block), this::bake);
      }
      model.update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }

  /** Deserializes this model from JSON */
  public static FluidTextureModel deserialize(JsonObject json, JsonDeserializationContext context) {
    ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
    Set<String> fluids = Collections.emptySet();
    if (json.has("fluids")) {
      fluids = ImmutableSet.copyOf(JsonHelper.parseList(json, "fluids", GsonHelper::convertToString));
    }
    Set<String> retextured = Collections.emptySet();
    if (json.has("retextured")) {
      retextured = ImmutableSet.copyOf(JsonHelper.parseList(json, "retextured", GsonHelper::convertToString));
    }
    return new FluidTextureModel(model, fluids, retextured);
  }
}
