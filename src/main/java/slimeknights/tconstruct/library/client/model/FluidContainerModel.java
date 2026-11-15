/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package slimeknights.tconstruct.library.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extension of {@link net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel} with two additional features: baked tints and fluid stack sensitive models.
 * Does not handle covers as I have never seen a need for them, and it means less code duplication (plus the forge model does the whole cover is mask thing wrong compared to 1.18).
 * The model JSON uses the loader {@code tconstruct:fluid_container} to set the fluid and textures ({@code base}, {@code fluid}, {@code particle}), while the client item JSON
 * uses the type {@code tconstruct:fluid_container} ({@link Unbaked}). Fluid sprites only exist after the fluid models load, so the static geometry is just the base texture.
 */
public class FluidContainerModel extends AbstractUnbakedModel {
  public static final UnbakedModelLoader<FluidContainerModel> LOADER = FluidContainerModel::deserialize;

  /** Clone of same named field from {@link net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel} */
  public static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());
  /** Transformation flipping the model for gases */
  private static final Transformation FLIP_TRANSFORM = new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null);

  /** Fluid to display, if empty uses the contained fluid */
  private final FluidStack fluid;
  /** If true, lighter than air fluids flip the model */
  private final boolean flipGas;

  public FluidContainerModel(StandardModelParameters parameters, FluidStack fluid, boolean flipGas) {
    super(parameters);
    this.fluid = fluid;
    this.flipGas = flipGas;
  }

  /** Deserializes this model from JSON */
  public static FluidContainerModel deserialize(JsonObject json, JsonDeserializationContext context) {
    FluidStack fluidStack = FluidStack.EMPTY;
    // parse the fluid
    if (json.has("fluid")) {
      JsonElement fluidElement = json.get("fluid");
      Fluid fluid;
      if (fluidElement.isJsonObject()) {
        JsonObject fluidObject = fluidElement.getAsJsonObject();
        fluid = Loadables.FLUID.getIfPresent(fluidObject, "name");
        // 1.21: FluidStack is component-backed; the legacy 'nbt' tag on the model fluid is no longer supported
      } else {
        fluid = Loadables.FLUID.convert(fluidElement, "fluid");
      }
      fluidStack = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
    }
    boolean flipGas = GsonHelper.getAsBoolean(json, "flip_gas", true);
    return new FluidContainerModel(StandardModelParameters.parse(json, context), fluidStack, flipGas);
  }

  /** Gets the given material, or null if the texture is not present in the model */
  @Nullable
  private static Material.Baked getMaterial(TextureSlots slots, MaterialBaker materials, String key, ModelDebugName name) {
    Material material = slots.getMaterial(key);
    if (material != null) {
      return materials.get(material, name);
    }
    return null;
  }

  @Override
  public UnbakedGeometry geometry() {
    return (ExtendedUnbakedGeometry) FluidContainerModel::bakeStatic;
  }

  /** Bakes the static geometry, just the base texture */
  private static QuadCollection bakeStatic(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName name, ContextMap additionalProperties) {
    Material.Baked base = getMaterial(textureSlots, baker.materials(), "base", name);
    if (base != null) {
      return baker.compute(new ItemModelGenerator.ItemLayerKey(base, ModelHelper.applyRootTransform(modelState, additionalProperties), 0));
    }
    return QuadCollection.EMPTY;
  }

  /** Creates the material info for a fluid quad. Uses cutout like the NeoForge model to avoid blending issues between atlases, and light emission without switching to an unlit render type */
  private static BakedQuad.MaterialInfo fluidMaterial(TextureAtlasSprite sprite, int light) {
    if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
      return new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.SOLID, Sheets.cutoutBlockItemSheet(), Sheets.cutoutBlockItemGlintSheet(), Sheets.cutoutBlockItemGlintSpecialSheet(), -1, null, light, true);
    }
    return new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.SOLID, Sheets.cutoutItemSheet(), Sheets.cutoutItemGlintSheet(), Sheets.cutoutItemGlintSpecialSheet(), -1, null, light, true);
  }

  /**
   * Item model type for fluid containers in client item JSON: {@code {"type": "tconstruct:fluid_container", "model": "tconstruct:item/copper_can"}}, where the model uses the loader {@code tconstruct:fluid_container}.
   * @param model           Model location
   * @param transformation  Extra transformation to apply to the model
   */
  public record Unbaked(Identifier model, Optional<Transformation> transformation) implements ItemModel.Unbaked {
    /** ID of this item model type */
    public static final Identifier ID = TConstruct.getResource("fluid_container");
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
      TextureSlots slots = resolved.getTopTextureSlots();
      String modelName = resolved.debugName();
      ModelDebugName debugName = () -> modelName;
      Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);

      // fetch the fluid settings
      FluidContainerModel containerModel = SimpleBlockModel.findModel(resolved, FluidContainerModel.class);
      FluidStack fluid = FluidStack.EMPTY;
      boolean flipGas = true;
      if (containerModel != null) {
        fluid = containerModel.fluid;
        flipGas = containerModel.flipGas;
      } else {
        TConstruct.LOG.warn("Model {} used with item model type {} does not use the fluid container loader, using default settings", model, ID);
      }

      // fetch textures, the fluid ones are baked later as the fluid models are not loaded yet
      MaterialBaker materials = baker.materials();
      Material.Baked base = getMaterial(slots, materials, "base", debugName);
      Material.Baked fluidMask = getMaterial(slots, materials, "fluid", debugName);
      Material.Baked particle = getMaterial(slots, materials, "particle", debugName);
      if (particle == null && base == null) {
        TConstruct.LOG.error("No valid particle sprite for fluid container model {}, you should supply either 'base' or 'particle'", modelName);
      }
      return new Baked(ModelHelper.dynamicBaker(baker), modelTransform, resolved.getTopTransforms(), fluid, flipGas, base, fluidMask, particle);
    }
  }

  /** Cache key for a fluid, includes components as they may change the tint */
  private record FluidKey(Fluid fluid, DataComponentPatch components) {
    private FluidKey(FluidStack stack) {
      this(stack.getFluid(), stack.getComponentsPatch());
    }
  }

  /** Item model handling swapping the model based on the contained fluid */
  private static final class Baked implements ItemModel {
    private static final ModelDebugName DEBUG_NAME = () -> "tconstruct:fluid_container_dynamic";

    // contains all the baked models since they'll never change
    private final Map<FluidKey, ItemModel> cache = new ConcurrentHashMap<>();

    private final ModelBaker baker;
    private final Matrix4fc transformation;
    private final ItemTransforms itemTransforms;
    private final FluidStack fluid;
    private final boolean flipGas;
    @Nullable
    private final Material.Baked base;
    @Nullable
    private final Material.Baked fluidMask;
    @Nullable
    private final Material.Baked particle;
    /** Model to use when no fluid is present */
    private final ItemModel empty;

    private Baked(ModelBaker baker, Matrix4fc transformation, ItemTransforms itemTransforms, FluidStack fluid, boolean flipGas,
                  @Nullable Material.Baked base, @Nullable Material.Baked fluidMask, @Nullable Material.Baked particle) {
      this.baker = baker;
      this.transformation = transformation;
      this.itemTransforms = itemTransforms;
      this.fluid = fluid;
      this.flipGas = flipGas;
      this.base = base;
      this.fluidMask = fluidMask;
      this.particle = particle;
      this.empty = bake(FluidStack.EMPTY);
    }

    /** Bakes the model for the given fluid */
    private ItemModel bake(FluidStack fluid) {
      // get basic sprites
      Material.Baked fluidSprite = null;
      int color = -1;
      if (!fluid.isEmpty()) {
        FluidSprites sprites = FluidSprites.of(fluid);
        fluidSprite = new Material.Baked(sprites.still(), false);
        color = sprites.color();
      }

      // determine particle
      Material.Baked particleSprite = particle;
      if (particleSprite == null) particleSprite = fluidSprite;
      if (particleSprite == null) particleSprite = base;
      if (particleSprite == null) {
        particleSprite = baker.materials().reportMissingReference("particle", DEBUG_NAME);
      }

      // if its a gas and we flipping, flip it
      ModelState modelState = BlockModelRotation.IDENTITY;
      if (flipGas && !fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir()) {
        modelState = new ComposedModelState(modelState, FLIP_TRANSFORM);
      }

      // start building the model
      QuadCollection.Builder builder = new QuadCollection.Builder();
      // add in the base
      if (base != null) {
        for (BakedQuad quad : baker.compute(new ItemModelGenerator.ItemLayerKey(base, modelState, 0)).getAll()) {
          builder.addUnculledFace(quad);
        }
      }

      // add in fluid
      if (fluidSprite != null && fluidMask != null) {
        // apply light: keep the fullbright lightmap so glowing fluids stay bright, but do NOT switch to the
        // emissive render type. Iris/Oculus shader packs frequently fail rendertype_entity_translucent_emissive
        // for items (missing Sampler2), which made glowing-fluid buckets (e.g. blazing blood) render as empty.
        int light = fluid.getFluid().getFluidType().getLightLevel(fluid);
        List<BakedQuad> quads = UnbakedElementsHelper.bakeItemMaskQuads(baker, fluidMask, fluidMaterial(fluidSprite.sprite(), light), new ComposedModelState(modelState, FLUID_TRANSFORM), ExtraFaceData.DEFAULT).getAll();
        // apply color, quads are immutable so build new ones
        if (color != -1) {
          quads = ColoredBlockModel.applyColor(quads, color);
        } else {
          quads = new ArrayList<>(quads);
        }
        // ensure light is set on every quad, the side faces use the mask material
        if (light > 0) {
          MutableQuad mutable = new MutableQuad();
          for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            if (quad.materialInfo().lightEmission() < light) {
              quads.set(i, mutable.setFrom(quad).setLightEmission(light).toBakedQuad());
            }
          }
        }
        for (BakedQuad quad : quads) {
          builder.addUnculledFace(quad);
        }
      }
      // We need to disable GUI 3D and block lighting for this to render properly
      return new CuboidItemModelWrapper(List.of(), builder.build(), new ModelRenderProperties(false, particleSprite, itemTransforms), transformation);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      // only do contained fluid if we did not set the fluid in the model properties
      FluidStack fluid = this.fluid;
      if (fluid.isEmpty()) {
        fluid = FluidUtil.getFirstStackContained(stack);
      }
      ItemModel model = empty;
      if (!fluid.isEmpty()) {
        FluidStack displayFluid = fluid;
        model = cache.computeIfAbsent(new FluidKey(fluid), key -> bake(displayFluid.copyWithAmount(FluidType.BUCKET_VOLUME)));
      }
      model.update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }
}
