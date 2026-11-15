package slimeknights.tconstruct.library.client.model.tools;

import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.mapping.CompactLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.ReversedListBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.modifiers.IBakedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMap;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.ModifierSetWorktableRecipe;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model handling all tools, both multipart and non.
 * The model JSON uses the loader {@code tconstruct:tool} to define the parts, modifier models and ammo display, while the client item JSON uses the type
 * {@code tconstruct:tool} ({@link Unbaked}) referencing that model to render the tool for the stack. When used as a plain model, it bakes the parts with the unknown material.
 */
public class ToolModel extends AbstractUnbakedModel {
  /** Location used for baking dynamic models, name does not matter so just using a constant */
  private static final Identifier BAKE_LOCATION = TConstruct.getResource("dynamic_model_baking");
  /** Shared loader instance */
  public static final UnbakedModelLoader<ToolModel> LOADER = ToolModel::deserialize;
  /** Set of transform types that make tools render small */
  private static final BitSet SMALL_TOOL_TYPES = new BitSet();
  /** Empty tint array */
  private static final int[] EMPTY_TINTS = new int[0];

  /** Registers a new small tool transform type */
  public static synchronized void registerSmallTool(ItemDisplayContext type) {
    SMALL_TOOL_TYPES.set(type.ordinal());
  }

  /** Gets an offset from JSON, or the default value if absent */
  private static Vec2 getOffset(JsonObject parent, String key) {
    if (parent.has(key)) {
      return MaterialModel.getVec2(parent, key);
    }
    return Vec2.ZERO;
  }

  /** Deserializes the model from JSON */
  public static ToolModel deserialize(JsonObject json, JsonDeserializationContext context) {
    List<ToolPart> parts = Collections.emptyList();
    if (json.has("parts")) {
      parts = JsonHelper.parseList(json, "parts", ToolPart::read);
    }
    boolean isLarge = GsonHelper.getAsBoolean(json, "large", false);
    boolean showTraits = GsonHelper.getAsBoolean(json, "show_traits", false);
    Vec2 offset = getOffset(json, "large_offset");
    // modifier root fetching
    List<Identifier> modifierModels = List.of();
    if (json.has("modifier_maps")) {
      modifierModels = JsonHelper.parseList(json, "modifier_maps", Loadables.RESOURCE_LOCATION);
    }
    List<Identifier> smallModifierRoots = List.of();
    List<Identifier> largeModifierRoots = List.of();
    if (json.has("modifier_roots")) {
      // large model requires an object
      if (isLarge) {
        JsonObject modifierRoots = GsonHelper.getAsJsonObject(json, "modifier_roots");
        smallModifierRoots = JsonHelper.parseList(modifierRoots, "small", Loadables.RESOURCE_LOCATION);
        largeModifierRoots = JsonHelper.parseList(modifierRoots, "large", Loadables.RESOURCE_LOCATION);
      } else {
        // small requires an array
        smallModifierRoots = JsonHelper.parseList(json, "modifier_roots", Loadables.RESOURCE_LOCATION);
      }
    }
    // fetch data related to ammo display
    Identifier ammoKey = null;
    Vec2 smallAmmoOffset = Vec2.ZERO;
    Vec2 largeAmmoOffset = Vec2.ZERO;
    boolean flipAmmo = false;
    boolean leftAmmo = false;
    if (json.has("ammo")) {
      JsonObject ammo = GsonHelper.getAsJsonObject(json, "ammo");
      ammoKey = JsonHelper.getResourceLocation(ammo, "key");
      flipAmmo = GsonHelper.getAsBoolean(ammo, "flip");
      leftAmmo = GsonHelper.getAsBoolean(ammo, "left");
      // large has an offset for both models
      if (isLarge) {
        if (!ammo.has("small_offset") && !ammo.has("large_offset")) {
          throw new JsonSyntaxException("Ammo must either have a small or large offset provided");
        }
        smallAmmoOffset = getOffset(ammo, "small_offset");
        largeAmmoOffset = getOffset(ammo, "large_offset");
      } else {
        // no large is just a single offset, and is required
        smallAmmoOffset = MaterialModel.getVec2(ammo, "offset");
      }
    }

    // modifiers first
    List<FirstModifier> firstModifiers = FirstModifier.LOADABLE.getOrDefault(json, "first_modifiers", List.of());
    return new ToolModel(StandardModelParameters.parse(json, context), parts, isLarge, offset, modifierModels, smallModifierRoots, largeModifierRoots, firstModifiers, ammoKey, flipAmmo, leftAmmo, smallAmmoOffset, largeAmmoOffset, showTraits);
  }

  /** List of tool parts in this model */
  private final List<ToolPart> toolParts;
  /** If true, this is a large tool and uses double resolution textures in hand */
  private final boolean isLarge;
  /** Transform matrix to apply to child parts */
  private final Vec2 offset;
  /** List of modifier models to read for this tool. Separate from the tool model for the sake of reuse. */
  private final List<Identifier> modifierModels;
  /**
   * Location to fetch modifier textures for small variant.
   * @deprecated use {@link #modifierModels}
   */
  @Deprecated
  private final List<Identifier> smallModifierRoots;
  /**
   * Location to fetch modifier textures for large variant
   * @deprecated use {@link #modifierModels}
   */
  @Deprecated
  private final List<Identifier> largeModifierRoots;
  /** Modifiers that show first on tools, bypassing normal sort order */
  @Deprecated
  private final List<FirstModifier> firstModifiers;
  /** Location in tool NBT to find the ammo */
  @Nullable
  private final Identifier ammoKey;
  /** If true, flips the ammo horizontally, as most bows have opposite orientation from ammo */
  private final boolean flipAmmo;
  /** If true, left handed models should have the ammo shifted to the left */
  private final boolean leftAmmo;
  /** Offset to apply to ammo quads for the small model */
  private final Vec2 smallAmmoOffset;
  /** Offset to apply to ammo quads for the large model */
  private final Vec2 largeAmmoOffset;
  /** If true, traits are displayed on the tool model. If false, just modifiers. */
  private final boolean showTraits;

  private ToolModel(StandardModelParameters parameters, List<ToolPart> toolParts, boolean isLarge, Vec2 offset, List<Identifier> modifierModels, List<Identifier> smallModifierRoots, List<Identifier> largeModifierRoots,
                    List<FirstModifier> firstModifiers, @Nullable Identifier ammoKey, boolean flipAmmo, boolean leftAmmo, Vec2 smallAmmoOffset, Vec2 largeAmmoOffset, boolean showTraits) {
    super(parameters);
    this.toolParts = toolParts;
    this.isLarge = isLarge;
    this.offset = offset;
    this.modifierModels = modifierModels;
    this.smallModifierRoots = smallModifierRoots;
    this.largeModifierRoots = largeModifierRoots;
    this.firstModifiers = firstModifiers;
    this.ammoKey = ammoKey;
    this.flipAmmo = flipAmmo;
    this.leftAmmo = leftAmmo;
    this.smallAmmoOffset = smallAmmoOffset;
    this.largeAmmoOffset = largeAmmoOffset;
    this.showTraits = showTraits;
  }

  /** Gets the parts to use for this model, defaulting to a single part named tool without materials */
  private List<ToolPart> getParts() {
    if (toolParts.isEmpty()) {
      return ToolPart.DEFAULT_PARTS;
    }
    return toolParts;
  }

  /** Gets the given texture from the texture slots, using the missing texture if not present to match the old geometry context behavior */
  private static Material getMaterial(TextureSlots slots, String name) {
    Material material = slots.getMaterial(name);
    if (material == null) {
      return new Material(MissingTextureAtlasSprite.getLocation());
    }
    return material;
  }

  /**
   * adds quads for relevant modifiers
   * @param spriteGetter    Sprite getter instance
   * @param modifierModels  Map of modifier models
   * @param tool            Tool instance
   * @param quadConsumer    Consumer for finished quads
   * @param transforms      Transforms to apply
   * @param isLarge         If true, the quads are for a large tool
   */
  private static void addModifierQuads(Function<Material, TextureAtlasSprite> spriteGetter, ModifierModelMap modifierModels, List<FirstModifier> firstModifiers, boolean showTraits, IToolStackView tool, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels, Transformation transforms, boolean isLarge) {
    if (!modifierModels.isEmpty()) {
      // keep a running tint index so models know where they should start, currently starts at 0 as the main model does not use tint indexes
      int modelIndex = 0;
      // reversed order to ensure the pixels is updated correctly
      List<ModifierEntry> modifiers = (showTraits ? tool.getModifiers() : tool.getUpgrades()).getModifiers();
      // keep track of the entry for each first modifier, as that may impact how it renders
      ModifierEntry[] firsts = new ModifierEntry[firstModifiers.size()];
      if (!modifiers.isEmpty()) {
        // last, add all regular modifiers
        Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(tool.getPersistentData(), TConstruct.getResource("invisible_modifiers"));
        for (int i = modifiers.size() - 1; i >= 0; i--) {
          ModifierEntry entry = modifiers.get(i);
          ModifierId modifier = entry.getModifier().getId();
          int index = FirstModifier.indexOf(firstModifiers, modifier);
          if (index != -1) {
            // handle first modifiers later
            firsts[index] = entry;
          } else if (!hidden.contains(modifier)) {
            IBakedModifierModel model = modifierModels.get(modifier);
            if (model != null) {
              // if the modifier is in the list, delay adding its quads, but keep the expected tint index
              model.addQuads(tool, entry, spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
              modelIndex += model.getTintIndexes();
            }
          }
        }
      }
      // first, add the first modifiers
      for (int i = firsts.length - 1; i >= 0; i--) {
        ModifierEntry entry = firsts[i];
        FirstModifier first = firstModifiers.get(i);
        if (entry != null || first.forced) {
          IBakedModifierModel model = modifierModels.get(first.id);
          if (model != null) {
            if (entry == null) {
              entry = new ModifierEntry(first.id, 0);
            }
            model.addQuads(tool, entry, spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
            modelIndex += model.getTintIndexes();
          }
        }
      }
      // iterate the constant in order of key (already reverse alphabetical)
      for (ModifierModel model : modifierModels.sortedConstant()) {
        model.addQuads(tool, ModifierEntry.EMPTY, spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
        modelIndex += model.getTintIndexes();
      }
    }
  }

  /**
   * Computes the tint colors for all tint indexes used by modifier quads. Replaces the 1.21.1 item color handler, which item tint sources can no longer express.
   * Iterates the modifiers in the same order as {@link #addModifierQuads(Function, ModifierModelMap, List, boolean, IToolStackView, Consumer, ItemLayerPixels, Transformation, boolean)},
   * so the tint indexes match the indexes assigned to the quads.
   * @return  Array of tints, index is the tint index
   */
  private static int[] getModifierTints(ModifierModelMap modifierModels, List<FirstModifier> firstModifiers, boolean showTraits, IToolStackView tool) {
    if (modifierModels.isEmpty()) {
      return EMPTY_TINTS;
    }
    TintBuilder tints = new TintBuilder();
    List<ModifierEntry> modifiers = (showTraits ? tool.getModifiers() : tool.getUpgrades()).getModifiers();
    ModifierEntry[] firsts = new ModifierEntry[firstModifiers.size()];
    if (!modifiers.isEmpty()) {
      Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(tool.getPersistentData(), TConstruct.getResource("invisible_modifiers"));
      for (int i = modifiers.size() - 1; i >= 0; i--) {
        ModifierEntry entry = modifiers.get(i);
        ModifierId modifier = entry.getModifier().getId();
        int index = FirstModifier.indexOf(firstModifiers, modifier);
        if (index != -1) {
          firsts[index] = entry;
        } else if (!hidden.contains(modifier)) {
          IBakedModifierModel model = modifierModels.get(modifier);
          if (model != null) {
            tints.add(model, tool, entry);
          }
        }
      }
    }
    for (int i = firsts.length - 1; i >= 0; i--) {
      ModifierEntry entry = firsts[i];
      FirstModifier first = firstModifiers.get(i);
      if (entry != null || first.forced) {
        IBakedModifierModel model = modifierModels.get(first.id);
        if (model != null) {
          if (entry == null) {
            entry = new ModifierEntry(first.id, 0);
          }
          tints.add(model, tool, entry);
        }
      }
    }
    for (ModifierModel model : modifierModels.sortedConstant()) {
      tints.add(model, tool, ModifierEntry.EMPTY);
    }
    return tints.build();
  }

  /** Helper to build the tint array */
  private static class TintBuilder {
    private int[] tints = EMPTY_TINTS;
    private int size = 0;

    /** Adds all tints from the given model */
    private void add(IBakedModifierModel model, IToolStackView tool, ModifierEntry entry) {
      int count = model.getTintIndexes();
      if (count > 0) {
        if (size + count > tints.length) {
          tints = Arrays.copyOf(tints, size + count);
        }
        for (int i = 0; i < count; i++) {
          tints[size + i] = model.getTint(tool, entry, i);
        }
        size += count;
      }
    }

    /** Builds the final array */
    private int[] build() {
      return size == tints.length ? tints : Arrays.copyOf(tints, size);
    }
  }

  /** Modifier that may be forced */
  private record FirstModifier(ModifierId id, boolean forced) {
    private static final Loadable<List<FirstModifier>> LOADABLE = CompactLoadable.of(
      RecordLoadable.create(ModifierId.PARSER.requiredField("name", FirstModifier::id), BooleanLoadable.INSTANCE.defaultField("forced", false, false, FirstModifier::forced), FirstModifier::new),
      ModifierId.PARSER.map(id -> new FirstModifier(id, false), FirstModifier::id),
      f -> !f.forced).list(0);

    /** Gets the index of a modifier in the list */
    public static int indexOf(List<FirstModifier> list, ModifierId id) {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).id.equals(id)) {
          return i;
        }
      }
      return -1;
    }
  }


  /* Static geometry */

  @Override
  public UnbakedGeometry geometry() {
    return (ExtendedUnbakedGeometry) this::bakeStatic;
  }

  /** Bakes the static geometry, which is just the small parts using the unknown material */
  private QuadCollection bakeStatic(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName name, ContextMap additionalProperties) {
    MaterialBaker materials = baker.materials();
    Function<Material,TextureAtlasSprite> spriteGetter = material -> materials.get(material, name).sprite();
    Transformation transform = MantleItemLayerModel.getItemTransform(ModelHelper.applyRootTransform(modelState, additionalProperties));
    ReversedListBuilder<Collection<BakedQuad>> quads = new ReversedListBuilder<>();
    ItemLayerPixels pixels = new ItemLayerPixels();
    List<ToolPart> parts = getParts();
    for (int i = parts.size() - 1; i >= 0; i--) {
      ToolPart part = parts.get(i);
      Material texture = getMaterial(textureSlots, part.getName(false));
      if (part.hasMaterials()) {
        TintedSprite sprite = MaterialModel.getMaterialSprite(spriteGetter, texture, MaterialId.UNKNOWN);
        quads.add(MantleItemLayerModel.getQuadsForSprite(sprite.color(), -1, new Material.Baked(sprite.sprite(), false), transform, sprite.emissivity(), pixels, baker.interner()));
      } else {
        quads.add(MantleItemLayerModel.getQuadsForSprite(-1, -1, materials.get(texture, name), transform, 0, pixels, baker.interner()));
      }
    }
    QuadCollection.Builder builder = new QuadCollection.Builder();
    quads.build(list -> list.forEach(builder::addUnculledFace));
    return builder.build();
  }


  /* Item model */

  /**
   * Bakes the dynamic item model for this tool
   * @param baker           Baker from the item model baking context
   * @param model           Resolved model containing this tool model
   * @param properties      Render properties from the resolved model
   * @param transformation  Item model transformation
   * @return  Baked item model
   */
  private ItemModel bakeItem(ModelBaker baker, ResolvedModel model, ModelRenderProperties properties, Matrix4fc transformation) {
    String modelName = model.debugName();
    // warn on deprecated keys
    if (showTraits) {
      TConstruct.LOG.warn("Using deprecated key 'show_traits' in tool model {}, use 'constant' in modifier model maps with 'tconstruct:trait' instead", modelName);
    }
    if (!firstModifiers.isEmpty()) {
      TConstruct.LOG.warn("Using deprecated key 'first_modifiers' in tool model {}, use 'constant' in modifier model maps with 'tconstruct:crafted' and an early ID instead", modelName);
    }

    // the baker passed in is only valid during baking, dynamic tools bake long after that
    MaterialBaker materials = ModelHelper.dynamicBaker(baker).materials();
    ModelDebugName debugName = () -> modelName;
    Function<Material,TextureAtlasSprite> spriteGetter = material -> materials.get(material, debugName).sprite();

    // fetch all textures from the model
    TextureSlots slots = model.getTopTextureSlots();
    List<ToolPart> toolParts = getParts();
    Material[] smallTextures = new Material[toolParts.size()];
    Material[] largeTextures = new Material[toolParts.size()];
    for (int i = 0; i < toolParts.size(); i++) {
      ToolPart part = toolParts.get(i);
      smallTextures[i] = getMaterial(slots, part.getName(false));
      if (isLarge) {
        largeTextures[i] = getMaterial(slots, part.getName(true));
      }
    }

    // if requested, check all that all material textures exist
    if (Config.CLIENT.logMissingMaterialTextures.get()) {
      for (int i = 0; i < toolParts.size(); i++) {
        if (toolParts.get(i).hasMaterials()) {
          MaterialModel.validateMaterialTextures(spriteGetter, smallTextures[i], null);
          if (isLarge) {
            MaterialModel.validateMaterialTextures(spriteGetter, largeTextures[i], null);
          }
        }
      }
    }
    // load modifier models
    Identifier modelLocation = Identifier.tryParse(modelName);
    ModifierModelMap modifierModels = ModifierModelMapManager.INSTANCE.getModelsForTool(spriteGetter, this.modifierModels, smallModifierRoots, largeModifierRoots, modelLocation != null ? modelLocation : BAKE_LOCATION);

    // build transforms for various states
    // large tools are stretched in X and Y by 200%, and get a special offset
    Transformation largeTransforms = isLarge ? new Transformation(new Vector3f((offset.x - 8) / 32, (-offset.y - 8) / 32, 0), null, new Vector3f(2, 2, 1), null) : null;
    // arrow quads are flipped, and also shifted
    Transformation smallAmmoTransforms = null;
    Transformation largeAmmoTransforms = null;
    Transformation leftAmmoTransforms = null;
    if (ammoKey != null) {
      // flipping rotates it 180 degrees, but because the origin is 0,0 instead of 0.5,0,5 it gets shifted
      // I could do some composition to shift the orgin, but its faster to just correct for it below
      Quaternionf ammoRotation = flipAmmo ? Axis.YP.rotationDegrees(-180) : null;
      float flipOffset = flipAmmo ? 1 : 0;

      // left if requested is based on either small or right, reusing a variable allows us to keep the one that ended up used.
      Vector3f translation = new Vector3f(
        smallAmmoOffset.x / 16 + flipOffset,
        -smallAmmoOffset.y / 16,
        1f / 16 + flipOffset
      );
      smallAmmoTransforms = new Transformation(translation, ammoRotation, null, null);
      if (isLarge) {
        translation = new Vector3f(
          (offset.x/2 + largeAmmoOffset.x + 4f) / 16 + flipOffset,
          (-offset.y/2 - largeAmmoOffset.y + 4f) / 16,
          1f / 16 + flipOffset
        );
        largeAmmoTransforms = new Transformation(translation, ammoRotation, null, null);
      }
      // if we want left ammo, it copies whichever is the most recent from the two offsets
      if (leftAmmo) {
        translation = new Vector3f(translation);
        translation.z = -1f / 16 + flipOffset;
        leftAmmoTransforms = new Transformation(translation, ammoRotation, null, null);
      }
    }
    return new Baked(spriteGetter, properties, transformation, toolParts, smallTextures, largeTextures, firstModifiers, showTraits, largeTransforms, modifierModels,
                     ammoKey, smallAmmoTransforms, largeAmmoTransforms, leftAmmoTransforms);
  }

  /**
   * Item model type for tools in client item JSON: {@code {"type": "tconstruct:tool", "model": "tconstruct:item/pickaxe"}}, where the model uses the loader {@code tconstruct:tool}.
   * Property based model swaps (broken, blocking, charging) are expressed in the client item JSON using the vanilla condition and range dispatch types with {@link slimeknights.tconstruct.library.client.model.TinkerItemProperties}.
   * Extra keys (such as the copy of the model data Continuum places in the client item) are ignored, the model JSON is the source of the tool data.
   * @param model           Model location, should use the {@code tconstruct:tool} loader, either directly or in a parent
   * @param transformation  Extra transformation to apply to the model
   */
  public record Unbaked(Identifier model, Optional<Transformation> transformation) implements ItemModel.Unbaked {
    /** ID of this item model type */
    public static final Identifier ID = TConstruct.getResource("tool");
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
      Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation)
    ).apply(inst, Unbaked::new));

    public Unbaked(Identifier model) {
      this(model, Optional.empty());
    }

    @Override
    public MapCodec<Unbaked> type() {
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
      ToolModel toolModel = SimpleBlockModel.findModel(resolved, ToolModel.class);
      if (toolModel == null) {
        TConstruct.LOG.warn("Model {} used with item model type {} does not use the tool loader, tool materials and modifiers will not display", model, ID);
        return new CuboidItemModelWrapper(List.of(), resolved.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY), properties, modelTransform);
      }
      return toolModel.bakeItem(baker, resolved, properties, modelTransform);
    }
  }

  /** Quads for a single tool variant, rendered as a single layer. Fills the layer like {@link CuboidItemModelWrapper}, but with tints computed from the tool */
  private static class ToolQuads {
    private final ItemQuads quads;
    private final Supplier<Vector3fc[]> extents;
    private final boolean animated;
    private final ModelRenderProperties properties;

    private ToolQuads(QuadCollection quads, ModelRenderProperties properties) {
      List<BakedQuad> all = quads.getAll();
      this.quads = ItemQuads.split(all);
      this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(all));
      this.animated = quads.hasMaterialFlag(BakedQuad.FLAG_ANIMATED);
      this.properties = properties;
    }

    /** Adds this model to the render state */
    private void update(ItemStackRenderState output, ItemStack stack, ItemDisplayContext displayContext, Matrix4fc transformation, int[] tints) {
      output.appendModelIdentityElement(this);
      LayerRenderState layer = output.newLayer();
      if (stack.hasFoil()) {
        FoilType foilType = stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK) ? FoilType.SPECIAL : FoilType.STANDARD;
        layer.setFoilType(foilType);
        output.setAnimated();
        output.appendModelIdentityElement(foilType);
      }
      if (tints.length > 0) {
        IntList tintLayers = layer.tintLayers();
        for (int tint : tints) {
          tintLayers.add(tint);
          output.appendModelIdentityElement(tint);
        }
      }
      layer.setExtents(extents);
      layer.setLocalTransform(transformation);
      properties.applyToLayer(layer, displayContext);
      layer.setQuads(quads);
      if (animated) {
        output.setAnimated();
      }
    }
  }

  /** All variants of a tool for a single cache key. Left handed uses the right model, as ammo is rendered separately */
  private record Variants(ToolQuads right, ToolQuads small, ToolQuads gui) {}

  /** Renders the ammo of a tool using a fully resolved item render state for the ammo stack */
  private enum AmmoRenderer implements SpecialModelRenderer<ItemStackRenderState> {
    INSTANCE;

    /** Approximate extents of the ammo, as the ammo is a full item model */
    private static final Vector3fc[] EXTENTS = {
      new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1),
      new Vector3f(1, 1, 0), new Vector3f(1, 0, 1), new Vector3f(0, 1, 1), new Vector3f(1, 1, 1)
    };

    @Override
    public void submit(@Nullable ItemStackRenderState ammo, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
      if (ammo != null) {
        ammo.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
      }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
      for (Vector3fc extent : EXTENTS) {
        output.accept(extent);
      }
    }

    @Nullable
    @Override
    public ItemStackRenderState extractArgument(ItemStack stack) {
      return null;
    }
  }

  /**
   * Baked dynamic tool model, swapping in the material textures and modifier quads for the stack.
   * Replaces the 1.21.1 override handler, baked tool model wrapper and item color handler.
   */
  private static final class Baked implements ItemModel {
    /** The ammo render state applies the default item transform, which moves by half a block. Moves it back so the ammo transform alone positions the ammo, like the old ammo quads */
    private static final Matrix4fc AMMO_CENTER = new Matrix4f().translation(0.5f, 0.5f, 0.5f);

    // contains all the baked models since they'll never change, cleared automatically as the baked model is discarded
    private final Cache<ToolCacheKey, Variants> cache = CacheBuilder
      .newBuilder()
      // ensure we can display every single tool that shows in JEI, plus a couple extra
      .maximumSize(Math.max(MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().size() * 3L / 2, 64))
      .build();

    // parameters needed for rebaking
    private final Function<Material, TextureAtlasSprite> spriteGetter;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final List<ToolPart> toolParts;
    private final Material[] smallTextures;
    private final Material[] largeTextures;
    private final List<FirstModifier> firstModifiers;
    private final boolean showTraits;
    @Nullable
    private final Transformation largeTransforms;
    private final ModifierModelMap modifierModels;
    @Nullable
    private final Identifier ammoKey;
    @Nullable
    private final Transformation smallAmmoTransforms;
    @Nullable
    private final Transformation largeAmmoTransforms;
    @Nullable
    private final Transformation leftAmmoTransforms;
    /** Model with no materials or modifiers */
    private final Variants original;

    private Baked(Function<Material, TextureAtlasSprite> spriteGetter, ModelRenderProperties properties, Matrix4fc transformation, List<ToolPart> toolParts, Material[] smallTextures, Material[] largeTextures,
                  List<FirstModifier> firstModifiers, boolean showTraits, @Nullable Transformation largeTransforms, ModifierModelMap modifierModels,
                  @Nullable Identifier ammoKey, @Nullable Transformation smallAmmoTransforms, @Nullable Transformation largeAmmoTransforms, @Nullable Transformation leftAmmoTransforms) {
      this.spriteGetter = spriteGetter;
      this.properties = properties;
      this.transformation = transformation;
      this.toolParts = toolParts;
      this.smallTextures = smallTextures;
      this.largeTextures = largeTextures;
      this.firstModifiers = firstModifiers;
      this.showTraits = showTraits;
      this.largeTransforms = largeTransforms;
      this.modifierModels = modifierModels;
      this.ammoKey = ammoKey;
      this.smallAmmoTransforms = smallAmmoTransforms;
      this.largeAmmoTransforms = largeAmmoTransforms;
      this.leftAmmoTransforms = leftAmmoTransforms;
      // bake the original with no tool, meaning it will skip modifiers and materials
      this.original = bakeInternal(List.of(), null);
    }

    /**
     * Bakes the tool variants for the given materials and tool
     * @param materials  Materials to use for the parts
     * @param tool       Tool instance for modifier parsing, null skips modifiers
     * @return  Baked variants
     */
    private Variants bakeInternal(List<MaterialVariantId> materials, @Nullable IToolStackView tool) {
      Transformation smallTransforms = Transformation.IDENTITY;

      // TODO: would be nice to support render types per material/per modifier
      // small model is used in GUIs (though that will be filtered to just front faces) and in small tool contexts like casting blocks
      ReversedListBuilder<Collection<BakedQuad>> smallQuads = new ReversedListBuilder<>();
      ItemLayerPixels smallPixels = new ItemLayerPixels();
      // large model is used in all other places
      ReversedListBuilder<Collection<BakedQuad>> largeQuads = largeTransforms != null ? new ReversedListBuilder<>() : smallQuads;
      ItemLayerPixels largePixels = largeTransforms != null ? new ItemLayerPixels() : smallPixels;

      // add quads for all modifiers first, for the sake of the item layer pixels
      if (tool != null && !modifierModels.isEmpty()) {
        addModifierQuads(spriteGetter, modifierModels, firstModifiers, showTraits, tool, smallQuads::add, smallPixels, smallTransforms, false);
        // if we have a large model, that means we will fetch models twice, once for large then again for small
        if (largeTransforms != null) {
          addModifierQuads(spriteGetter, modifierModels, firstModifiers, showTraits, tool, largeQuads::add, largePixels, largeTransforms, true);
        }
      }

      // add quads for all parts
      TextureAtlasSprite particle = null;
      for (int i = toolParts.size() - 1; i >= 0; i--) {
        ToolPart part = toolParts.get(i);

        // part with materials
        if (part.hasMaterials()) {
          // start by fetching the material we are rendering at this position, should only be null on invalid tools or during the initial bake
          int index = part.index();
          MaterialVariantId material = index < materials.size() ? materials.get(index) : MaterialId.UNKNOWN;
          TintedSprite materialSprite = MaterialModel.getMaterialSprite(spriteGetter, smallTextures[i], material);
          particle = materialSprite.sprite();

          // need full quads for both as small is directly rendered in a few non-GUI cases
          smallQuads.add(MantleItemLayerModel.getQuadsForSprite(materialSprite.color(), -1, materialSprite.sprite(), smallTransforms, materialSprite.emissivity(), smallPixels));
          if (largeTransforms != null) {
            largeQuads.add(MaterialModel.getQuadsForMaterial(spriteGetter, largeTextures[i], material, -1, largeTransforms, largePixels));
          }
        } else {
          // part without materials
          particle = spriteGetter.apply(smallTextures[i]);
          // same drill as above
          smallQuads.add(MantleItemLayerModel.getQuadsForSprite(-1, -1, particle, smallTransforms, 0, smallPixels));
          if (largeTransforms != null) {
            largeQuads.add(MantleItemLayerModel.getQuadsForSprite(-1, -1, spriteGetter.apply(largeTextures[i]), largeTransforms, 0, largePixels));
          }
        }
      }
      // should never happen, but just in case prevents a NPE
      if (particle == null) {
        particle = spriteGetter.apply(new Material(MissingTextureAtlasSprite.getLocation()));
        TConstruct.LOG.error("Created tool model without a particle sprite, this means it somehow has no parts. This should not be possible");
      }
      // tools never used block light, matching the old model builder
      ModelRenderProperties renderProperties = new ModelRenderProperties(false, new Material.Baked(particle, false), properties.transforms());

      // time to bake the model. Opted for a small amount of redundant code to minimize operations as this is run dynamically
      QuadCollection.Builder smallBuilder = new QuadCollection.Builder();
      QuadCollection.Builder guiBuilder = new QuadCollection.Builder();
      smallQuads.build(quads -> quads.forEach(quad -> {
        smallBuilder.addUnculledFace(quad);
        if (quad.direction() == Direction.SOUTH) {
          guiBuilder.addUnculledFace(quad);
        }
      }));
      ToolQuads small = new ToolQuads(smallBuilder.build(), renderProperties);
      ToolQuads gui = new ToolQuads(guiBuilder.build(), renderProperties);

      // if no large, use small for right
      ToolQuads right;
      if (largeTransforms != null) {
        QuadCollection.Builder largeBuilder = new QuadCollection.Builder();
        largeQuads.build(quads -> quads.forEach(largeBuilder::addUnculledFace));
        right = new ToolQuads(largeBuilder.build(), renderProperties);
      } else {
        right = small;
      }
      return new Variants(right, small, gui);
    }

    /** Fetches the ammo stack for the given tool, or empty if no ammo is present */
    private ItemStack getAmmo(IToolStackView tool, @Nullable ClientLevel level, ImmutableList.Builder<Object> keyBuilder) {
      if (ammoKey != null) {
        ModDataNBT persistentData = tool.getPersistentData();
        if (persistentData.contains(ammoKey, Tag.TAG_COMPOUND)) {
          CompoundTag ammoTag = persistentData.getCompound(ammoKey);
          if (!ammoTag.isEmpty()) {
            ClientLevel clientLevel = level != null ? level : Minecraft.getInstance().level;
            if (clientLevel != null) {
              RegistryAccess registryAccess = clientLevel.registryAccess();
              ItemStack ammo = ItemStack.OPTIONAL_CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), ammoTag).result().orElse(ItemStack.EMPTY);
              if (!ammo.isEmpty()) {
                // 1.21: item NBT moved to components; the serialized ammo tag is the stable cache discriminator
                keyBuilder.add(ammo.getItem());
                keyBuilder.add(ammoTag);
              }
              return ammo;
            }
          }
        }
      }
      return ItemStack.EMPTY;
    }

    /** Gets the ammo transformation for the given display context, matching which of the old small, large and left models received ammo quads */
    @Nullable
    private Transformation getAmmoTransforms(ItemDisplayContext displayContext) {
      // GUI and small contexts use the small model, so they get the small ammo
      if (displayContext == ItemDisplayContext.GUI || (largeTransforms != null && SMALL_TOOL_TYPES.get(displayContext.ordinal()))) {
        return smallAmmoTransforms;
      }
      if (leftAmmoTransforms != null && displayContext.leftHand()) {
        return leftAmmoTransforms;
      }
      return largeAmmoTransforms != null ? largeAmmoTransforms : smallAmmoTransforms;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      // use material IDs for the sake of internal rendering materials
      List<MaterialVariantId> materialIds = MaterialIdNBT.from(stack).getMaterials();
      IToolStackView tool = ToolStack.from(stack);

      // if nothing unique, render original
      // TODO 1.21: with how forced modifiers work, this will probably be true way more often
      ModifierNBT modifiers = showTraits ? tool.getModifiers() : tool.getUpgrades();
      Variants variants = original;
      boolean unique = !materialIds.isEmpty() || !modifiers.isEmpty() || !modifierModels.sortedConstant().isEmpty();
      if (!unique) {
        for (FirstModifier modifier : firstModifiers) {
          if (modifier.forced) {
            unique = true;
            break;
          }
        }
      }
      if (unique) {
        // build the cache key for the modifiers, based on what the modifier requests
        // for many, it is just the modifier entry, but they can have more complex keys if needed
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(tool.getPersistentData(), TConstruct.getResource("invisible_modifiers"));
        ModifierEntry[] firstEntries = new ModifierEntry[firstModifiers.size()];
        for (ModifierEntry entry : modifiers) {
          ModifierId id = entry.getId();
          int index = FirstModifier.indexOf(firstModifiers, id);
          if (index != -1) {
            // handle all the first entries together, keeps their order consistent
            firstEntries[index] = entry;
          } else if (!hidden.contains(id)) {
            IBakedModifierModel model = modifierModels.get(id);
            if (model != null) {
              Object cacheKey = model.getCacheKey(tool, entry);
              if (cacheKey != null) {
                builder.add(cacheKey);
              }
            }
          }
        }
        for (int i = 0; i < firstModifiers.size(); i++) {
          FirstModifier modifier = firstModifiers.get(i);
          ModifierEntry entry = firstEntries[i];
          if (entry != null || modifier.forced) {
            IBakedModifierModel model = modifierModels.get(modifier.id);
            if (model != null) {
              if (entry == null) {
                entry = new ModifierEntry(modifier.id, 0);
              }
              Object cacheKey = model.getCacheKey(tool, entry);
              if (cacheKey != null) {
                builder.add(cacheKey);
              }
            }
          }
        }
        for (ModifierModel model : modifierModels.constant().values()) {
          Object cacheKey = model.getCacheKey(tool, ModifierEntry.EMPTY);
          if (cacheKey != null) {
            builder.add(cacheKey);
          }
        }

        // render special model
        try {
          variants = cache.get(new ToolCacheKey(materialIds, builder.build()), () -> bakeInternal(materialIds, tool));
        } catch (ExecutionException e) {
          TConstruct.LOG.error("Failed to get tool model from cache", e);
        }
      }

      // select the model for the context, left hand uses the right model as ammo is handled below
      ToolQuads model;
      if (displayContext == ItemDisplayContext.GUI) {
        model = variants.gui;
      } else if (variants.right != variants.small && SMALL_TOOL_TYPES.get(displayContext.ordinal())) {
        model = variants.small;
      } else {
        model = variants.right;
      }
      int[] tints = unique ? getModifierTints(modifierModels, firstModifiers, showTraits, tool) : EMPTY_TINTS;
      model.update(output, stack, displayContext, transformation, tints);

      // fetch ammo info from the stack, rendered on top of the tool using the ammo's own item model
      if (ammoKey != null) {
        Transformation ammoTransforms = getAmmoTransforms(displayContext);
        if (ammoTransforms != null) {
          ImmutableList.Builder<Object> ammoKeyBuilder = ImmutableList.builder();
          ItemStack ammo = getAmmo(tool, level, ammoKeyBuilder);
          if (!ammo.isEmpty()) {
            // resolve the ammo without an item transform, the tool transform applies to it
            ItemStackRenderState ammoState = new ItemStackRenderState();
            resolver.updateForTopItem(ammoState, ammo, ItemDisplayContext.NONE, level, owner, seed);
            if (!ammoState.isEmpty()) {
              output.appendModelIdentityElement(ammoKeyBuilder.build());
              output.appendModelIdentityElement(ammoTransforms);
              if (ammoState.isAnimated()) {
                output.setAnimated();
              }
              LayerRenderState layer = output.newLayer();
              layer.setupSpecialModel(AmmoRenderer.INSTANCE, ammoState);
              properties.applyToLayer(layer, displayContext);
              layer.setLocalTransform(new Matrix4f(transformation).mul(ammoTransforms.getMatrix()).mul(AMMO_CENTER));
            }
          }
        }
      }
    }
  }

  /**
   * Data class for a single tool part
   */
  private record ToolPart(String name, int index) {
    /** Default tool part instance for breakable textures */
    public static final ToolPart DEFAULT = new ToolPart("tool", -1);
    /** Default tool part list if one is not defined */
    public static final List<ToolPart> DEFAULT_PARTS = List.of(DEFAULT);

    /**
     * If true, this part has material variants
     */
    public boolean hasMaterials() {
      return index >= 0;
    }

    /**
     * Gets the name for this part
     * @param isLarge  If true, rendering a large tool
     * @return Texture name for this part
     */
    public String getName(boolean isLarge) {
      if (isLarge) {
        return "large_" + name;
      }
      return name;
    }

    /**
     * Reads a part from JSON
     */
    public static ToolPart read(JsonObject json) {
      String name = GsonHelper.getAsString(json, "name");
      int index = GsonHelper.getAsInt(json, "index", -1);
      return new ToolPart(name, index);
    }
  }

  /** Simple data class to cache built tool modifiers, contains everything unique in the textures */
  private record ToolCacheKey(List<MaterialVariantId> materials, List<Object> modifierData) {}
}
