package slimeknights.tconstruct.library.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.joml.Matrix4fc;
import slimeknights.mantle.client.model.util.ModelTextureIteratable;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Model providing a variant for the GUI. The model JSON uses the loader {@code tconstruct:gui} with a nested {@code gui} model, textures in the nested model may reference textures in the main model.
 * The main model is used as the block model, while the client item JSON type {@code tconstruct:gui} ({@link Unbaked}) swaps to the nested model in the GUI.
 */
public class UniqueGuiModel extends SimpleBlockModel {
  /** Shared loader instance */
  public static final UnbakedModelLoader<UniqueGuiModel> LOADER = UniqueGuiModel::deserialize;

  /** Model to use in the GUI */
  protected final UnbakedModel gui;

  public UniqueGuiModel(SimpleBlockModel model, UnbakedModel gui) {
    super(model);
    this.gui = gui;
  }

  @Override
  public void resolveDependencies(ResolvableModel.Resolver resolver) {
    super.resolveDependencies(resolver);
    // the nested model is not part of the parent chain, so its parent and dependencies need to be marked
    Identifier guiParent = gui.parent();
    if (guiParent != null) {
      resolver.markDependency(guiParent);
    }
    gui.resolveDependencies(resolver);
  }

  /**
   * Bakes the GUI model
   * @param baker  Model baker
   * @param model  Resolved model containing this model, used for texture fallbacks
   * @return  Baked quads for the GUI
   */
  private QuadCollection bakeGui(ModelBaker baker, ResolvedModel model) {
    String guiName = model.debugName() + "#gui";
    ModelDebugName debugName = () -> guiName;
    ResolvedModel guiModel = baker.resolveInlineModel(gui, debugName);
    // gui textures fall back to the main model textures, matching the shared geometry context of the old model
    TextureSlots.Resolver resolver = new TextureSlots.Resolver();
    for (TextureSlots.Data data : ModelTextureIteratable.of(guiModel)) {
      resolver.addLast(data);
    }
    for (TextureSlots.Data data : ModelTextureIteratable.of(model)) {
      resolver.addLast(data);
    }
    return guiModel.getTopGeometry().bake(resolver.resolve(debugName), baker, BlockModelRotation.IDENTITY, debugName);
  }

  /** Loader for this model */
  public static UniqueGuiModel deserialize(JsonObject json, JsonDeserializationContext context) {
    return new UniqueGuiModel(
      SimpleBlockModel.deserialize(json, context),
      context.deserialize(GsonHelper.getAsJsonObject(json, "gui"), UnbakedModel.class)
    );
  }

  /**
   * Item model type swapping the model for the GUI: {@code {"type": "tconstruct:gui", "model": "tconstruct:block/foundry/proxy_tank"}}, where the model uses the loader {@code tconstruct:gui}.
   * @param model           Model location
   * @param transformation  Extra transformation to apply to the model
   */
  public record Unbaked(Identifier model, Optional<Transformation> transformation) implements ItemModel.Unbaked {
    /** ID of this item model type */
    public static final Identifier ID = TConstruct.getResource("gui");
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
      ItemModel main = new CuboidItemModelWrapper(List.of(), resolved.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY), properties, modelTransform);
      UniqueGuiModel guiModel = SimpleBlockModel.findModel(resolved, UniqueGuiModel.class);
      if (guiModel == null) {
        TConstruct.LOG.warn("Model {} used with item model type {} does not use the GUI loader, GUI variant will not display", model, ID);
        return main;
      }
      return new Baked(main, new CuboidItemModelWrapper(List.of(), guiModel.bakeGui(baker, resolved), properties, modelTransform));
    }
  }

  /**
   * Item model that swaps the model for the GUI
   */
  public record Baked(ItemModel base, ItemModel gui) implements ItemModel {
    @Override
    public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      (displayContext == ItemDisplayContext.GUI ? gui : base).update(output, stack, resolver, displayContext, level, owner, seed);
    }
  }
}
