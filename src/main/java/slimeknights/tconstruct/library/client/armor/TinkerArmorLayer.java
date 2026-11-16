package slimeknights.tconstruct.library.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Render layer drawing Tinkers armor, added next to every vanilla {@link HumanoidArmorLayer}.
 * <p>
 * 26.3: NeoForge's {@code getGenericArmorModel} hook can no longer render multiple textures from one model (models are drawn later
 * with a single render type each), so this layer replaces it. It reuses the armor models of the vanilla layer it is added next to,
 * and hands every armor stack with an {@link ArmorModelDispatcher} client extension to {@link ArmorModelDispatcher#submitArmor}.
 * The vanilla layer still sees the stack but draws nothing for it, see {@link ArmorModelDispatcher#getArmorLayerTintColor}.
 */
public class TinkerArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
  private final ArmorModelSet<A> modelSet;
  private final ArmorModelSet<A> babyModelSet;

  public TinkerArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet, ArmorModelSet<A> babyModelSet) {
    super(renderer);
    this.modelSet = modelSet;
    this.babyModelSet = babyModelSet;
  }

  public TinkerArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet) {
    this(renderer, modelSet, modelSet);
  }

  @Override
  public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
    // same order as the vanilla armor layer
    submitArmorPiece(poseStack, collector, state.chestEquipment, EquipmentSlot.CHEST, lightCoords, state);
    submitArmorPiece(poseStack, collector, state.legsEquipment, EquipmentSlot.LEGS, lightCoords, state);
    submitArmorPiece(poseStack, collector, state.feetEquipment, EquipmentSlot.FEET, lightCoords, state);
    submitArmorPiece(poseStack, collector, state.headEquipment, EquipmentSlot.HEAD, lightCoords, state);
  }

  /** Submits a single armor piece if its client extension is a Tinkers armor dispatcher */
  private void submitArmorPiece(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot, int lightCoords, S state) {
    if (!stack.isEmpty() && IClientItemExtensions.of(stack) instanceof ArmorModelDispatcher dispatcher) {
      Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && equippable.slot() == slot) {
        // baby armor models use their own texture layout since 26.x, which Tinkers armor textures do not have
        // vanilla shows nothing for equipment assets lacking baby layers, so do the same. Small armor stands use an adult layout
        if (state.isBaby && state.entityType != EntityTypes.ARMOR_STAND) {
          return;
        }
        A model = (state.isBaby ? this.babyModelSet : this.modelSet).get(slot);
        dispatcher.submitArmor(poseStack, collector, lightCoords, state, stack, slot, model);
      }
    }
  }


  /* Registration */

  /** Adds this layer to every entity renderer that has a vanilla armor layer. Call from {@link EntityRenderersEvent.AddLayers}. */
  public static void addLayers(EntityRenderersEvent.AddLayers event) {
    for (EntityType<?> type : event.getEntityTypes()) {
      addLayer(event.<Entity, EntityRenderer<Entity, ?>>getRenderer(type));
    }
    for (PlayerModelType skin : event.getSkins()) {
      addLayer(event.getPlayerRenderer(skin));
      addLayer(event.getMannequinRenderer(skin));
    }
  }

  /** Adds the layer to the given renderer if it renders vanilla armor */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void addLayer(@Nullable EntityRenderer<?,?> renderer) {
    if (renderer instanceof LivingEntityRenderer<?,?,?> living) {
      // copy as we are adding to the list
      for (RenderLayer<?,?> layer : List.copyOf(living.layers)) {
        if (layer instanceof HumanoidArmorLayer<?,?,?> armor) {
          // the vanilla layer's generics match the renderer, raw types since we lost them
          living.addLayer(new TinkerArmorLayer((LivingEntityRenderer) living, armor.modelSet, armor.babyModelSet));
          return;
        }
      }
    }
  }
}
