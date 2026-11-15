package slimeknights.tconstruct.library.client.modifiers.model;

import com.google.common.collect.Interners;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.model.FluidContainerModel;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Model for a fluid in a tool. */
public record FluidModifierModel(@Nullable Material small, @Nullable Material large, ToolTankHelper tankHelper) implements SimpleModifierModel {
  public static final RecordLoadable<FluidModifierModel> LOADER = RecordLoadable.create(
    ModifierModel.MATERIAL_LOADABLE.nullableField("mask", FluidModifierModel::small),
    ModifierModel.MATERIAL_LOADABLE.nullableField("mask_large", FluidModifierModel::large),
    ToolTankHelper.LOADABLE.defaultField("tank_helper", ToolTankHelper.TANK_HELPER, false, FluidModifierModel::tankHelper),
    FluidModifierModel::new);

  /**
   * The vanilla model bakery uses an orgin of 0.5,0.5,0.5, and forges dynamic fluid code uses the vanilla model bakery. (see{@link net.minecraft.client.resources.model.cuboid.FaceBakery} {@code #rotateVertexBy()} for vanilla bakery)
   * However, item layer wants an origin of 0,0,0, which is what we expect in our tool models. So cancel out the origin.
   */
  private static final Vector3f ORIGIN = new Vector3f(-0.5f, -0.5f, -0.5f);

  /** Instance with default tank helper */
  public FluidModifierModel(@Nullable Material small, @Nullable Material large) {
    this(small, large, ToolTankHelper.TANK_HELPER);
  }

  /** Cache key for {@link #getCacheKey(IToolStackView, ModifierEntry)} */
  private record CacheKey(Fluid fluid, DataComponentPatch components) {}

  @Nullable
  @Override
  public Object getCacheKey(IToolStackView tool, ModifierEntry modifier) {
    FluidStack fluid = tankHelper().getFluid(tool);
    if (!fluid.isEmpty()) {
      // components may change the fluid tint (such as potions), so they are part of the key
      return new CacheKey(fluid.getFluid(), fluid.getComponentsPatch());
    }
    return null;
  }

  @Override
  public RecordLoadable<FluidModifierModel> getLoader() {
    return LOADER;
  }

  @Override
  public void addQuads(IToolStackView tool, ModifierEntry modifier, Function<Material, TextureAtlasSprite> spriteGetter, Transformation transforms, boolean isLarge, int startTintIndex, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels) {
    // ensure template exists
    Material template = isLarge ? large() : small();
    if (template != null) {
      // ensure we have fluid
      FluidStack fluid = tankHelper().getFluid(tool);
      if (!fluid.isEmpty()) {
        addQuads(fluid, template, spriteGetter, transforms, quadConsumer);
      }
    }
  }

  /** Adds quads for the given fluid */
  public static void addQuads(FluidStack fluid, Material template, Function<Material,TextureAtlasSprite> spriteGetter, Transformation transforms, Consumer<Collection<BakedQuad>> quadConsumer) {
    // must have texture for the proper state
    // fluid properties, 26.3 moved fluid textures and tints to the fluid model
    FluidSprites sprites = FluidRenderer.getFluidSprites(fluid);
    Material.Baked fluidSprite = new Material.Baked(sprites.still(), false);

    // build fluid like the forge dynamic container model
    // TODO: is there anything that can be done about the fluid? to prevent weird offsets?
    ModelState state = new ComposedModelState(BlockModelRotation.IDENTITY, transforms.applyOrigin(ORIGIN).compose(FluidContainerModel.FLUID_TRANSFORM));
    List<BakedQuad> maskQuads = UnbakedElementsHelper.bakeItemMaskQuads(MaskQuadBaker.INSTANCE, -1, new Material.Baked(spriteGetter.apply(template), false), fluidSprite, state).getAll(); // Use template as mask

    // apply fluid sprite, brightness and color. The side faces use the mask sprite in 26.3, while they used the fluid in 1.21.1
    int luminosity = fluid.getFluid().getFluidType().getLightLevel(fluid);
    int color = sprites.color();
    MutableQuad mutable = new MutableQuad();
    List<BakedQuad> fluidQuads = new ArrayList<>(maskQuads.size());
    for (BakedQuad quad : maskQuads) {
      mutable.setFrom(quad);
      if (quad.materialInfo().sprite() != fluidSprite.sprite()) {
        mutable.setSpriteAndMoveUv(fluidSprite);
      }
      if (luminosity > 0) {
        mutable.setLightEmission(luminosity);
      }
      if (color != -1) {
        mutable.setColor(color);
      }
      fluidQuads.add(mutable.toBakedQuad());
    }
    quadConsumer.accept(fluidQuads);
  }

  /** Minimal model baker for baking item mask quads after model loading. Only supports interning quad data, used by {@link UnbakedElementsHelper#bakeItemMaskQuads(ModelBaker, int, Material.Baked, Material.Baked, ModelState)} */
  private enum MaskQuadBaker implements ModelBaker {
    INSTANCE;

    private final com.google.common.collect.Interner<Vector3fc> vectors = Interners.newWeakInterner();
    private final com.google.common.collect.Interner<BakedQuad.MaterialInfo> materialInfos = Interners.newWeakInterner();
    private final ModelBaker.Interner interner = new ModelBaker.Interner() {
      @Override
      public Vector3fc vector(Vector3fc vector) {
        return vectors.intern(vector);
      }

      @Override
      public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
        return materialInfos.intern(material);
      }
    };

    @Override
    public ResolvedModel getModel(Identifier location) {
      throw new UnsupportedOperationException("Cannot resolve models when baking fluid quads");
    }

    @Override
    public BlockStateModelPart missingBlockModelPart() {
      throw new UnsupportedOperationException("Cannot fetch block model parts when baking fluid quads");
    }

    @Override
    public MaterialBaker materials() {
      throw new UnsupportedOperationException("Cannot bake materials when baking fluid quads");
    }

    @Override
    public ModelBaker.Interner interner() {
      return interner;
    }

    @Override
    public <T> T compute(SharedOperationKey<T> key) {
      return key.compute(this);
    }
  }
}
