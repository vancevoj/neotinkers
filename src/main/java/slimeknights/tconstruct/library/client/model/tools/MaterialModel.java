package slimeknights.tconstruct.library.client.model.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Model for an item with material texture variants, such as tool parts. Used only for single material items, {@link ToolModel} is used for multi-material items.
 * The model JSON uses the loader {@code tconstruct:material} with the texture {@code texture}, while the client item JSON uses the type {@code tconstruct:material} ({@link Unbaked})
 * to swap in the material from the stack. When used as a plain model, it bakes the static material or the unknown material.
 */
public class MaterialModel extends AbstractUnbakedModel {
  /** Shared loader instance */
  public static final UnbakedModelLoader<MaterialModel> LOADER = MaterialModel::deserialize;

  /** If null, uses dynamic material */
  @Nullable
  private final MaterialVariantId material;
  /** Tint index and index of part in tool */
  private final int index;
  /** Transform matrix to apply to child parts */
  private final Vec2 offset;

  public MaterialModel(StandardModelParameters parameters, @Nullable MaterialVariantId material, int index, Vec2 offset) {
    super(parameters);
    this.material = material;
    this.index = index;
    this.offset = offset;
  }

  /**
   * Checks that all unique material textures for the given part exist, logs any that are missing via the sprite getter function.
   * @param spriteGetter  Sprite getter, logs missing sprites
   * @param texture       Base texture
   * @param material      Material to check, if null checks all materials
   */
  public static void validateMaterialTextures(Function<Material, TextureAtlasSprite> spriteGetter, Material texture, @Nullable MaterialVariantId material) {
    // if the texture is missing, stop here with a warning for the root
    if (!MissingTextureAtlasSprite.getLocation().equals(texture.sprite())) {
      // if no specific material is set, load all materials as dependencies. If just one material, use just that one
      if (material == null) {
        MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().forEach(info -> info.getSprite(texture, spriteGetter));
      } else {
        MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material).ifPresent(info -> info.getSprite(texture, spriteGetter));
      }
    }
  }

  /**
   * Gets the tinted sprite info for the given material
   * @param spriteGetter  Sprite getter instance
   * @param texture       Base texture
   * @param material      Material variant
   * @return  Tinted sprite or fallback
   */
  @SuppressWarnings("OptionalIsPresent")
  public static TintedSprite getMaterialSprite(Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material) {
    // if the base material is non-null, try to find the sprite for that material
    // first, find a render info
    Optional<MaterialRenderInfo> optional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
    if (optional.isPresent()) {
      return optional.get().getSprite(texture, spriteGetter);
    }
    return new TintedSprite(spriteGetter.apply(texture), -1, 0);
  }

  /**
   * Gets quads for the given material variant of the texture
   * @param spriteGetter    Sprite getter instance
   * @param texture         Base texture
   * @param material        Material variant
   * @param tintIndex       Tint index for quads
   * @param transformation  Transformation to apply
   * @param pixels          Pixels to prevent z-fighting for multiple layers
   * @return  Quad list
   */
  public static List<BakedQuad> getQuadsForMaterial(Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material, int tintIndex, Transformation transformation, @Nullable ItemLayerPixels pixels) {
    TintedSprite sprite = getMaterialSprite(spriteGetter, texture, material);
    return MantleItemLayerModel.getQuadsForSprite(sprite.color(), tintIndex, sprite.sprite(), transformation, sprite.emissivity(), pixels);
  }

  /** Gets the transformation for the given offset */
  private static Transformation getTransform(Vec2 offset) {
    // TODO: figure out forge transforms, can I use them here?
    if (Vec2.ZERO.equals(offset)) {
      return Transformation.IDENTITY;
    }
    // divide by 16 to convert from pixels to base values
    // negate Y as positive is up for transforms but down for pixels
    return new Transformation(new Vector3f(offset.x / 16, -offset.y / 16, 0), null, null, null);
  }

  /**
   * Bakes the quads for the given material
   * @param spriteGetter  Sprite getter function
   * @param texture       Base texture
   * @param transform     Transform to apply to the quad fetching. Should not include rotation or it will look wrong in UIs
   * @param material      Material used
   * @param index         Tint index to use if tinted sprite is used
   * @return  Baked quads and the particle sprite
   */
  private static QuadCollection bakeInternal(Function<Material, TextureAtlasSprite> spriteGetter, Material texture, Transformation transform, MaterialVariantId material, int index) {
    TintedSprite materialSprite = getMaterialSprite(spriteGetter, texture, material);
    QuadCollection.Builder builder = new QuadCollection.Builder();
    // TODO: let material choose its render type
    for (BakedQuad quad : MantleItemLayerModel.getQuadsForSprite(materialSprite.color(), index, materialSprite.sprite(), transform, materialSprite.emissivity())) {
      builder.addUnculledFace(quad);
    }
    return builder.build();
  }

  @Override
  public UnbakedGeometry geometry() {
    return (ExtendedUnbakedGeometry) this::bakeStatic;
  }

  /** Bakes the static geometry, using the static material if set or the unknown material */
  private QuadCollection bakeStatic(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName name, ContextMap additionalProperties) {
    MaterialBaker materials = baker.materials();
    Function<Material,TextureAtlasSprite> spriteGetter = mat -> materials.get(mat, name).sprite();
    Material texture = getTexture(textureSlots);
    if (Config.CLIENT.logMissingMaterialTextures.get()) {
      validateMaterialTextures(spriteGetter, texture, material);
    }
    Transformation transform = getTransform(offset).compose(MantleItemLayerModel.getItemTransform(ModelHelper.applyRootTransform(modelState, additionalProperties)));
    return bakeInternal(spriteGetter, texture, transform, Objects.requireNonNullElse(material, MaterialId.UNKNOWN), index);
  }

  /** Gets the texture from the given slots */
  private static Material getTexture(TextureSlots slots) {
    Material texture = slots.getMaterial("texture");
    return texture != null ? texture : new Material(MissingTextureAtlasSprite.getLocation());
  }

  /**
   * Gets the render properties for a material or tool item model. Like in 1.21.1, these models take their particle from their textures and rarely set one,
   * while {@link ModelRenderProperties#fromResolvedModel(ModelBaker, ResolvedModel, TextureSlots)} reports a missing particle reference.
   * @param baker     Model baker
   * @param resolved  Resolved model
   * @param slots     Texture slots of the model
   * @param particle  Particle to use when the model has no particle texture
   * @return  Render properties
   */
  static ModelRenderProperties renderProperties(ModelBaker baker, ResolvedModel resolved, TextureSlots slots, Material particle) {
    if (slots.getMaterial("particle") != null) {
      return ModelRenderProperties.fromResolvedModel(baker, resolved, slots);
    }
    return new ModelRenderProperties(resolved.getTopGuiLight().lightLikeBlock(), baker.materials().get(particle, resolved), resolved.getTopTransforms());
  }


  /* Helpers */

  /** Loads a material model from JSON */
  public static MaterialModel deserialize(JsonObject json, JsonDeserializationContext context) {
    // need tint index for tool models, doubles as part index
    int index = GsonHelper.getAsInt(json, "index", 0);

    // static material can be defined, if unset uses dynamic material
    MaterialVariantId material = null;
    if (json.has("material")) {
      material = MaterialVariantId.fromJson(json, "material");
    }

    Vec2 offset = Vec2.ZERO;
    if (json.has("offset")) {
      offset = getVec2(json, "offset");
    }

    return new MaterialModel(StandardModelParameters.parse(json, context), material, index, offset);
  }

  /**
   * Converts a JSON float array to the specified object
   * @param json    JSON object
   * @param name    Name of the array in the object to fetch
   * @return  Vector3f of data
   * @throws JsonParseException  If there is no array or the length is wrong
   */
  public static Vec2 getVec2(JsonObject json, String name) {
    JsonArray array = GsonHelper.getAsJsonArray(json, name);
    if (array.size() != 2) {
      throw new JsonParseException("Expected " + 2 + " " + name + " values, found: " + array.size());
    }
    float[] vec = new float[2];
    for(int i = 0; i < 2; ++i) {
      vec[i] = GsonHelper.convertToFloat(array.get(i), name + "[" + i + "]");
    }
    return new Vec2(vec[0], vec[1]);
  }


  /* Item model */

  /** Codec for a material variant ID as a string */
  private static final Codec<MaterialVariantId> MATERIAL_CODEC = Codec.STRING.comapFlatMap(text -> {
    MaterialVariantId id = MaterialVariantId.tryParse(text);
    return id != null ? DataResult.success(id) : DataResult.error(() -> "Invalid material variant ID " + text);
  }, MaterialVariantId::toString);
  /** Default model for transforms when no model is set */
  private static final Identifier DEFAULT_MODEL = Identifier.fromNamespaceAndPath("neoforge", "item/default");

  /**
   * Item model type for material items in client item JSON: {@code {"type": "tconstruct:material", "model": "tconstruct:item/pick_head"}}, where the model uses the loader {@code tconstruct:material}.
   * The other fields override the values from the model, and allow using this without a model JSON ({@code texture} is then required, transforms come from {@code neoforge:item/default}).
   * @param model           Model location, should use the {@code tconstruct:material} loader
   * @param texture         Texture override
   * @param index           Tint index override
   * @param offsetX         X offset override in pixels
   * @param offsetY         Y offset override in pixels
   * @param material        Static material override
   * @param transformation  Extra transformation to apply to the model
   */
  public record Unbaked(Optional<Identifier> model, Optional<Identifier> texture, Optional<Integer> index, Optional<Float> offsetX, Optional<Float> offsetY, Optional<MaterialVariantId> material, Optional<Transformation> transformation) implements ItemModel.Unbaked {
    /** ID of this item model type */
    public static final Identifier ID = TConstruct.getResource("material");
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.<Unbaked>mapCodec(inst -> inst.group(
      Identifier.CODEC.optionalFieldOf("model").forGetter(Unbaked::model),
      Identifier.CODEC.optionalFieldOf("texture").forGetter(Unbaked::texture),
      Codec.INT.optionalFieldOf("index").forGetter(Unbaked::index),
      Codec.FLOAT.optionalFieldOf("offset_x").forGetter(Unbaked::offsetX),
      Codec.FLOAT.optionalFieldOf("offset_y").forGetter(Unbaked::offsetY),
      MATERIAL_CODEC.optionalFieldOf("material").forGetter(Unbaked::material),
      Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation)
    ).apply(inst, Unbaked::new)).validate(unbaked -> unbaked.model.isPresent() || unbaked.texture.isPresent() ? DataResult.success(unbaked) : DataResult.error(() -> "Material item model requires either a model or a texture"));

    /** Creates an instance for the given model */
    public Unbaked(Identifier model) {
      this(Optional.of(model), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model.orElse(DEFAULT_MODEL));
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
      ModelBaker baker = context.blockModelBaker();
      ResolvedModel resolved = baker.getModel(model.orElse(DEFAULT_MODEL));
      TextureSlots textureSlots = resolved.getTopTextureSlots();
      Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);

      // find model data, defaulting to the overrides
      MaterialModel materialModel = SimpleBlockModel.findModel(resolved, MaterialModel.class);
      if (materialModel == null && model.isPresent() && texture.isEmpty()) {
        TConstruct.LOG.warn("Model {} used with item model type {} does not use the material loader, materials will not display", model.get(), ID);
      }
      Material textureMaterial = texture.map(Material::new).orElseGet(() -> getTexture(textureSlots));
      // the baked model swaps the particle for the material sprite, the base texture covers the model wide properties
      ModelRenderProperties properties = renderProperties(baker, resolved, textureSlots, textureMaterial);
      int index = this.index.orElse(materialModel != null ? materialModel.index : 0);
      Vec2 modelOffset = materialModel != null ? materialModel.offset : Vec2.ZERO;
      Vec2 offset = new Vec2(offsetX.orElse(modelOffset.x), offsetY.orElse(modelOffset.y));
      MaterialVariantId staticMaterial = material.orElse(materialModel != null ? materialModel.material : null);

      // validate textures
      String modelName = resolved.debugName();
      ModelDebugName debugName = () -> modelName;
      MaterialBaker materials = ModelHelper.dynamicBaker(baker).materials();
      Function<Material,TextureAtlasSprite> spriteGetter = mat -> materials.get(mat, debugName).sprite();
      if (Config.CLIENT.logMissingMaterialTextures.get()) {
        validateMaterialTextures(spriteGetter, textureMaterial, staticMaterial);
      }

      // create transforms from offset
      Transformation transforms = getTransform(offset);
      Baked baked = new Baked(spriteGetter, textureMaterial, transforms, index, properties, modelTransform);
      // if the material is already set, no need to swap materials
      if (staticMaterial != null) {
        return baked.bakeDynamic(staticMaterial);
      }
      return baked;
    }
  }

  /**
   * Dynamic item model to swap in the material texture
   */
  private static final class Baked implements ItemModel {
    // contains all the baked models since they'll never change, cleared automatically as the baked model is discarded
    private final Map<MaterialVariantId, ItemModel> cache = new ConcurrentHashMap<>();

    // parameters needed for rebaking
    private final Function<Material, TextureAtlasSprite> spriteGetter;
    private final Material texture;
    private final Transformation itemTransform;
    private final int index;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    private Baked(Function<Material, TextureAtlasSprite> spriteGetter, Material texture, Transformation itemTransform, int index, ModelRenderProperties properties, Matrix4fc transformation) {
      this.spriteGetter = spriteGetter;
      this.texture = texture;
      this.itemTransform = itemTransform;
      this.index = index;
      this.properties = properties;
      this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      // fetch the material from the stack
      MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
      // cache all baked material models, they will not need to be recreated as materials will not change
      cache.computeIfAbsent(material, this::bakeDynamic).update(output, stack, resolver, displayContext, level, owner, seed);
    }

    /**
     * Bakes a copy of this model using the given material
     * @param material  New material for the model
     * @return  Baked model
     */
    private ItemModel bakeDynamic(MaterialVariantId material) {
      QuadCollection quads = bakeInternal(spriteGetter, texture, itemTransform, material, index);
      // particle matches the material
      TintedSprite materialSprite = getMaterialSprite(spriteGetter, texture, material);
      ModelRenderProperties materialProperties = new ModelRenderProperties(properties.usesBlockLight(), new Material.Baked(materialSprite.sprite(), false), properties.transforms());
      return new CuboidItemModelWrapper(List.of(), quads, materialProperties, transformation);
    }
  }
}
