package slimeknights.tconstruct.library.client.model.tools;

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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4fc;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.RetexturedModel.RetexturedContext;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel.ColorData;
import slimeknights.mantle.client.model.util.GeometryContextWrapper;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Model that handles dynamic materials using the block model elements style.
 * When used for tools, notably does not handle modifier models, just materials are considered.
 * The model JSON uses the loader {@code tconstruct:material_block}, the blockstate needs the type {@code tconstruct:material_block} ({@link #BLOCK_STATE_MODEL}) and the
 * client item JSON the type {@code tconstruct:material_block} ({@link #ITEM_MODEL}) to swap in materials. Without them the model renders statically.
 * @see MaterialModel
 * @see ToolModel
 */
public class MaterialBlockModel extends SimpleBlockModel {
  /** ID of the blockstate and item model types */
  public static final Identifier ID = TConstruct.getResource("material_block");
  /** Loadable for the list of material textures */
  private static final Loadable<Set<String>> MATERIAL = StringLoadable.DEFAULT.set(ArrayLoadable.COMPACT);
  /** Loadable for the list of parts, each a list of material textures */
  private static final Loadable<List<Set<String>>> PARTS = StringLoadable.DEFAULT.set(ArrayLoadable.COMPACT_OR_EMPTY).list(1);
  /** Shared loader instance */
  public static final UnbakedModelLoader<MaterialBlockModel> LOADER = MaterialBlockModel::deserialize;

  public enum ModelType { TOOL, PART, ANVIL }

  /** List of parts, each is a set of materials to retexture */
  private final List<Set<String>> parts;
  /** Type of model to create. */
  private final ModelType type;

  /**
   * Creates a new model
   * @param model  Block model to retexture. Expected each element is a single material or not a material, no multiple or mixed.
   * @param parts  List of parts, each is a set of materials to retexture
   * @param type   Type of model to create
   */
  public MaterialBlockModel(SimpleBlockModel model, List<Set<String>> parts, ModelType type) {
    super(model);
    this.parts = parts;
    this.type = type;
  }

  /** Loads a material block model from JSON */
  public static MaterialBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);

    if (json.has("retextured")) {
      return new MaterialBlockModel(model, List.of(MATERIAL.getIfPresent(json, "retextured")), ModelType.ANVIL);
    }
    if (json.has("material")) {
      return new MaterialBlockModel(model, List.of(MATERIAL.getIfPresent(json, "material")), ModelType.PART);
    }
    return new MaterialBlockModel(model, PARTS.getIfPresent(json, "parts"), ModelType.TOOL);
  }

  /** Finds the model in the given chain */
  @Nullable
  public static MaterialBlockModel find(@Nullable ResolvedModel model) {
    return SimpleBlockModel.findModel(model, MaterialBlockModel.class);
  }

  /**
   * Creates the logic to rebake this model
   * @param baker     Model baker
   * @param resolved  Resolved model containing this model
   * @param state     Model state without the root transform
   * @param base      Model with no materials
   * @return  Rebake logic
   */
  private Rebaker createRebaker(ModelBaker baker, ResolvedModel resolved, ModelState state, BlockStateModelPart base) {
    ModelBaker dynamicBaker = ModelHelper.dynamicBaker(baker);
    String debugName = resolved.debugName();
    TextureSlots slots = resolved.getTopTextureSlots();
    List<Set<String>> parts = this.parts.stream().map(part -> RetexturedModel.getAllRetextured(resolved, part)).toList();
    boolean particleRetextured = parts.stream().anyMatch(set -> set.contains("particle"));
    return new Rebaker(dynamicBaker, getElements(resolved), slots, new GeometryContextWrapper(dynamicBaker, slots, () -> debugName), () -> debugName,
                       ModelHelper.applyRootTransform(state, resolved.getTopAdditionalProperties()), parts, particleRetextured, resolved.getTopAmbientOcclusion(), base);
  }

  /** Logic to rebake the model with materials */
  private record Rebaker(ModelBaker baker, List<CuboidModelElement> elements, TextureSlots slots, GeometryContextWrapper materials, net.minecraft.client.resources.model.ModelDebugName name,
                         ModelState transform, List<Set<String>> parts, boolean particleRetextured, boolean ambientOcclusion, BlockStateModelPart base) {
    /** Fetches a single material */
    private void fetchMaterial(MaterialVariantId material, Set<String> retextured, Function<Material,TextureAtlasSprite> spriteGetter, Map<String,Material.Baked> replacements, Map<String,TintedSprite> tints) {
      Optional<MaterialRenderInfo> optional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
      if (optional.isPresent()) {
        MaterialRenderInfo info = optional.get();

        Map<Material,TintedSprite> seen = new HashMap<>();
        for (String name : retextured) {
          Material texture = slots.getMaterial(name);
          if (texture == null) {
            continue;
          }
          TintedSprite tinted = seen.get(texture);
          if (tinted == null) {
            tinted = info.getSprite(texture, spriteGetter);
            seen.put(texture, tinted);
          }
          replacements.put(name, new Material.Baked(tinted.sprite(), texture.forceTranslucent()));
          if (tinted.color() != -1 || tinted.emissivity() > 0) {
            tints.put(name, tinted);
          }
        }
      }
    }

    /**
     * Retextures with the given materials list
     * @param materials  Material for each part, missing materials skip the part
     * @return  Baked part
     */
    private BlockStateModelPart bakeWith(List<MaterialVariantId> materials) {
      Map<String,Material.Baked> replacements = new HashMap<>();
      Map<String,TintedSprite> tints = new HashMap<>();
      MaterialBaker materialBaker = baker.materials();
      Function<Material,TextureAtlasSprite> spriteGetter = mat -> materialBaker.get(mat, name).sprite();
      for (int i = 0; i < parts.size() && i < materials.size(); i++) {
        fetchMaterial(materials.get(i), parts.get(i), spriteGetter, replacements, tints);
      }

      if (replacements.isEmpty()) {
        return base;
      }

      GeometryContextWrapper context = new GeometryContextWrapper(texture -> {
        Material.Baked replacement = replacements.get(texture);
        return replacement != null ? replacement : this.materials.getMaterial(texture);
      }, texture -> replacements.containsKey(texture) || this.materials.hasMaterial(texture));
      QuadCollection quads;
      if (tints.isEmpty()) {
        quads = SimpleBlockModel.bakeModel(baker, elements, context, transform);
      } else {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (CuboidModelElement part : elements) {
          TintedSprite tint = null;
          for (CuboidFace face : part.faces().values()) {
            TintedSprite faceTint = tints.get(ModelHelper.stripReference(face.texture()));
            if (faceTint != null) {
              tint = faceTint;
              break;
            }
          }
          if (tint != null) {
            ColoredBlockModel.bakePart(builder, baker, part, new ColorData(tint.color(), tint.emissivity(), null), context, transform);
          } else {
            SimpleBlockModel.bakePart(builder, baker, part, context, transform);
          }
        }
        quads = builder.build();
      }
      return new SimpleModelWrapper(quads, ambientOcclusion, context.getMaterial("particle"));
    }

    /** Rebakes the model with a specific texture. See also {@link RetexturedModel} */
    private BlockStateModelPart bakeWithBlock(Material.Baked texture) {
      RetexturedContext context = new RetexturedContext(materials, parts.getFirst(), texture);
      return new SimpleModelWrapper(SimpleBlockModel.bakeModel(baker, elements, context, transform), ambientOcclusion, context.getMaterial("particle"));
    }
  }

  /** Caches for the various model types, shared between block and item models */
  private static class ModelCache {
    /** Cache of materials to baked model */
    private final Cache<MaterialIdNBT,BlockStateModelPart> toolCache = CacheBuilder
      .newBuilder()
      .maximumSize(Math.max(MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().size() * 3L / 2, 64))
      .build();
    /** Cache of material to baked model */
    private final Map<MaterialVariantId,BlockStateModelPart> materialCache = new ConcurrentHashMap<>();
    /** Cache of block texture to baked model */
    private final Map<Material.Baked,BlockStateModelPart> blockCache = new ConcurrentHashMap<>();
    private final Rebaker rebaker;
    private final ModelType type;

    private ModelCache(Rebaker rebaker, ModelType type) {
      this.rebaker = rebaker;
      this.type = type;
    }

    /** Gets the model for the given material */
    public BlockStateModelPart getCachedModel(MaterialVariantId material) {
      if (MaterialId.UNKNOWN.equals(material)) {
        return rebaker.base;
      }
      return materialCache.computeIfAbsent(material, mat -> rebaker.bakeWith(List.of(mat)));
    }

    /** Gets the model for the given materials */
    public BlockStateModelPart getCachedModel(MaterialIdNBT materials) {
      // tools with a single part use the single material cache
      if (rebaker.parts.size() == 1) {
        return getCachedModel(materials.getMaterial(0));
      }
      if (!materials.getMaterials().isEmpty()) {
        try {
          return toolCache.get(materials, () -> rebaker.bakeWith(materials.getMaterials()));
        } catch (ExecutionException e) {
          TConstruct.LOG.error("Failed to get tool model from cache", e);
        }
      }
      return rebaker.base;
    }

    /** Gets the cached model for the given block. */
    public BlockStateModelPart getCachedModel(Block block) {
      return blockCache.computeIfAbsent(ModelHelper.getParticleMaterial(block), rebaker::bakeWithBlock);
    }
  }


  /* Block state model */

  /** Block state model swapping in the materials from model data */
  private static class Baked implements DynamicBlockStateModel {
    private final BlockStateModelPart base;
    private final ModelCache cache;

    private Baked(BlockStateModelPart base, ModelCache cache) {
      this.base = base;
      this.cache = cache;
    }

    /** Gets the key object from model data, which is the block, material, or materials depending on the type */
    @Nullable
    private Object getKey(ModelData data) {
      if (cache.type == ModelType.ANVIL) {
        Block block = data.get(RetexturedHelper.BLOCK_PROPERTY);
        if (block != null) {
          return block;
        }
      }
      if (cache.type == ModelType.TOOL) {
        return data.get(ModelProperties.MATERIALS);
      }
      return data.get(ModelProperties.MATERIAL);
    }

    /** Gets the part for the given key */
    private BlockStateModelPart getPart(@Nullable Object key) {
      return switch (key) {
        case Block block -> cache.getCachedModel(block);
        case MaterialIdNBT materials -> cache.getCachedModel(materials);
        case MaterialVariantId material -> cache.getCachedModel(material);
        case null, default -> base;
      };
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      return new GeometryKey(this, getKey(level.getModelData(pos)));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
      parts.add(getPart(getKey(level.getModelData(pos))));
    }

    @Override
    public Material.Baked particleMaterial() {
      return base.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      if (cache.rebaker.particleRetextured) {
        return getPart(getKey(level.getModelData(pos))).particleMaterial();
      }
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
      return getPart(getKey(level.getModelData(pos))).materialFlags();
    }
  }

  /** Key for geometry caching */
  private record GeometryKey(Baked model, @Nullable Object key) {}

  /** Unbaked block state model, parsed from blockstate JSON as a variant with {@code "type": "tconstruct:material_block"} */
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
      MaterialBlockModel model = find(resolved);
      if (model == null) {
        TConstruct.LOG.warn("Model {} used with blockstate type {} does not use the material block loader, materials will not display", variant.modelLocation(), ID);
        return new SingleVariant(base);
      }
      // model was rejected by vanilla, no sense retexturing the missing model
      if (base == baker.missingBlockModelPart()) {
        return new SingleVariant(base);
      }
      return new Baked(base, new ModelCache(model.createRebaker(baker, resolved, state, base), model.type));
    }
  }
  /** Codec for the block state model type */
  public static final MapCodec<? extends CustomUnbakedBlockStateModel> BLOCK_STATE_MODEL = BlockStateUnbaked.CODEC;


  /* Item model */

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

  /**
   * Unbaked item model swapping in materials from the stack, used as {@code {"type": "tconstruct:material_block", "model": "tconstruct:block/table/tinkers_anvil"}}
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
      MaterialBlockModel materialModel = find(resolved);
      if (materialModel == null) {
        TConstruct.LOG.warn("Model {} used with item model type {} does not use the material block loader, materials will not display", model, ID);
        return base;
      }
      BlockStateModelPart basePart = SimpleModelWrapper.bake(baker, resolved, BlockModelRotation.IDENTITY);
      return new MaterialItem(base, basePart, new ModelCache(materialModel.createRebaker(baker, resolved, BlockModelRotation.IDENTITY, basePart), materialModel.type), properties, modelTransform);
    }
  }
  /** Codec for the item model type */
  public static final MapCodec<? extends ItemModel.Unbaked> ITEM_MODEL = ItemUnbaked.MAP_CODEC;

  /** Item model swapping in materials from the stack */
  private static class MaterialItem implements ItemModel {
    private final Map<BlockStateModelPart,ItemModel> itemCache = new ConcurrentHashMap<>();
    private final ItemModel base;
    private final BlockStateModelPart basePart;
    private final ModelCache cache;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    private MaterialItem(ItemModel base, BlockStateModelPart basePart, ModelCache cache, ModelRenderProperties properties, Matrix4fc transformation) {
      this.base = base;
      this.basePart = basePart;
      this.cache = cache;
      this.properties = properties;
      this.transformation = transformation;
    }

    /** Gets the part for the given stack */
    private BlockStateModelPart getPart(ItemStack stack) {
      if (cache.type == ModelType.ANVIL) {
        Block block = RetexturedHelper.getTexture(stack);
        if (block != Blocks.AIR) {
          return cache.getCachedModel(block);
        }
      }
      if (cache.type == ModelType.TOOL) {
        return cache.getCachedModel(MaterialIdNBT.from(stack));
      }
      return cache.getCachedModel(IMaterialItem.getMaterialFromStack(stack));
    }

    /** Converts a part into an item model */
    private ItemModel toItemModel(BlockStateModelPart part) {
      return new CuboidItemModelWrapper(List.of(), toItemQuads(part), new ModelRenderProperties(properties.usesBlockLight(), part.particleMaterial(), properties.transforms()), transformation);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      BlockStateModelPart part = getPart(stack);
      ItemModel model = part == basePart ? base : itemCache.computeIfAbsent(part, this::toItemModel);
      model.update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }
}
