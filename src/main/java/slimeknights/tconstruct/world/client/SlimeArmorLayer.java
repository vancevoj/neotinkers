package slimeknights.tconstruct.world.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Generics do not match to use the vanilla armor layer, so this is a reimplementation of some of {@link HumanoidArmorLayer} and {@link CustomHeadLayer}.
 * Requires the renderer to fill {@link ArmoredSlimeRenderState} using {@link #extractHelmet(Slime, ArmoredSlimeRenderState)}.
 */
public class SlimeArmorLayer<S extends ArmoredSlimeRenderState, M extends EntityModel<? super S>> extends RenderLayer<S,M> {
  /** Armor model for the head slot, posed by {@link ArmoredSlimeRenderState#armorState} */
  private final HumanoidModel<HumanoidRenderState> armorModel;
  private final EquipmentLayerRenderer equipmentRenderer;
  private final Function<SkullBlock.Type,SkullModelBase> skullModels;
  private final PlayerSkinRenderCache playerSkinRenderCache;
  private final boolean lavaSlime;
  public SlimeArmorLayer(RenderLayerParent<S,M> pRenderer, EntityRendererProvider.Context context, boolean lavaSlime) {
    super(pRenderer);
    this.armorModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_ARMOR.head()));
    this.equipmentRenderer = context.getEquipmentRenderer();
    EntityModelSet modelSet = context.getModelSet();
    this.skullModels = Util.memoize(type -> SkullBlockRenderer.createModel(modelSet, type));
    this.playerSkinRenderCache = context.getPlayerSkinRenderCache();
    this.lavaSlime = lavaSlime;
  }

  /**
   * Extracts the helmet for rendering. Skulls and other head items are already extracted by the living entity renderer
   * ({@code wornHeadType}, {@code headItem}).
   */
  public static void extractHelmet(Slime entity, ArmoredSlimeRenderState state) {
    ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
    state.helmet = helmet.isEmpty() ? ItemStack.EMPTY : helmet.copy();
  }

  @Override
  public void submit(PoseStack matrices, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot) {
    ItemStack helmet = state.helmet;
    if (!helmet.isEmpty()) {
      matrices.pushPose();
      if (lavaSlime) {
        float squish = state.squish;
        if (squish < 0) {
          squish = 0;
        }
        matrices.translate(0, 1.5 - 0.425 * squish, 0);
      } else {
        matrices.translate(0, 1.5, 0);
      }
      matrices.scale(0.9f, 0.9f, 0.9f);

      // helmet renderer, based on humanoid armor layer
      Equippable equippable = helmet.get(DataComponents.EQUIPPABLE);
      boolean isHelmet = equippable != null && equippable.slot() == EquipmentSlot.HEAD;
      if (isHelmet && IClientItemExtensions.of(helmet) instanceof ArmorModelDispatcher dispatcher) {
        // tinkers armor renders itself, formerly through the armor model hook
        state.armorState.outlineColor = state.outlineColor;
        dispatcher.submitArmor(matrices, collector, packedLight, state.armorState, helmet, EquipmentSlot.HEAD, armorModel);
      } else if (isHelmet && equippable.assetId().isPresent()) {
        // equipment layers handle the dyed layer, glint and trims
        equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.HUMANOID, equippable.assetId().get(), armorModel, state.armorState, helmet, matrices, collector, packedLight, state.outlineColor);
      } else if (state.wornHeadType != null) {
        // block model renderer, based on custom head layer
        // skull block rendering, the old rotation and flips cancel out to a plain scale
        SkullModelBase skullModel = this.skullModels.apply(state.wornHeadType);
        if (skullModel != null) {
          matrices.scale(1.1875F, 1.1875F, 1.1875F);
          SkullBlockRenderer.submitSkull(state.wornHeadAnimationPos, matrices, collector, packedLight, skullModel, getSkullRenderType(state.wornHeadType, state.wornHeadProfile), state.outlineColor, null);
        }
      } else if (!state.headItem.isEmpty()) {
        // standard rendering
        CustomHeadLayer.translateToHead(matrices, CustomHeadLayer.Transforms.DEFAULT);
        state.headItem.submit(matrices, collector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
      }
      matrices.popPose();
    }
  }

  /** Gets the render type for the given skull, using the player skin for player heads */
  private RenderType getSkullRenderType(SkullBlock.Type type, @Nullable ResolvableProfile profile) {
    if (type == SkullBlock.Types.PLAYER && profile != null) {
      return playerSkinRenderCache.getOrDefault(profile).renderType();
    }
    return SkullBlockRenderer.getSkullRenderType(type, null);
  }
}
