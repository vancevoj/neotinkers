package slimeknights.tconstruct.gadgets.item;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import slimeknights.mantle.item.TooltipItem;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.gadgets.capability.PiggybackCapability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class PiggyBackPackItem extends TooltipItem {
  private static final int MAX_ENTITY_STACK = 3; // how many entities can be carried at once
  public PiggyBackPackItem(Properties props) {
    super(props);
  }

  @Override
  public InteractionResult interactLivingEntity(ItemStack stack, Player playerIn, LivingEntity target, InteractionHand hand) {
    // is the chest slot empty?
    ItemStack chestArmor = playerIn.getItemBySlot(EquipmentSlot.CHEST);

    // need enough space to exchange the chest armor
    if (chestArmor.getItem() != this && playerIn.getInventory().getFreeSlot() == -1) {
      // not enough inventory space
      return InteractionResult.PASS;
    }

    // try carrying the entity
    if (pickupEntity(playerIn, target)) {
      // unequip old armor
      if (chestArmor.getItem() != this) {
        playerIn.getInventory().placeItemBackInInventory(chestArmor, Prediction.SERVER_ONLY);
        chestArmor = ItemStack.EMPTY;
      }

      // we could pick it up just fine, check if we need to "equip" more of the item
      if (chestArmor.isEmpty()) {
        playerIn.setItemSlot(EquipmentSlot.CHEST, stack.split(1));
      } else if (chestArmor.getCount() < this.getEntitiesCarriedCount(playerIn)) {
        stack.split(1);
        chestArmor.grow(1);
      }
      // successfully picked up an entity
      return InteractionResult.SUCCESS;
    }

    return InteractionResult.CONSUME;
  }

  /**
   * Checks if the passed entity is a vehicle of the other
   * @param entity           Entity to query
   * @param possibleVehicle  Possible vehicle of the entity
   * @return  True if it's a vehicle, or a vehicle of a vehicle
   */
  private static boolean isVehicle(Entity entity, Entity possibleVehicle) {
    for (Entity vehicle = entity.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
      if (vehicle == possibleVehicle) {
        return true;
      }
    }
    return false;
  }

  private static boolean pickupEntity(Player player, Entity target) {
    if (player.level().isClientSide() || target.is(TinkerTags.EntityTypes.PIGGYBACKPACK_BLACKLIST)) {
      return false;
    }
    // silly players, clicking on entities they're already carrying or riding
    if (isVehicle(player, target) || isVehicle(target, player)) {
      return false;
    }

    int count = 0;
    Entity toRide = player;
    while (toRide.isVehicle() && count < MAX_ENTITY_STACK) {
      toRide = toRide.getPassengers().get(0);
      count++;
      // don't allow more than 1 player, that can easily cause endless loops with riding detection for some reason.
      if (toRide instanceof Player && target instanceof Player) {
        return false;
      }
    }

    // can only ride one entity each
    if (!toRide.isVehicle() && count < MAX_ENTITY_STACK) {
      // todo: possibly throw off all passengers of the target
      if (target.startRiding(toRide, true, true)) {
        if (player instanceof ServerPlayer) {
          TinkerNetwork.getInstance().sendVanillaPacket(player, new ClientboundSetPassengersPacket(player));
        }
        return true;
      }
    }
    return false;
  }

  private int getEntitiesCarriedCount(LivingEntity player) {
    int count = 0;
    Entity ridden = player;
    while (ridden.isVehicle()) {
      count++;
      ridden = ridden.getPassengers().get(0);
    }

    return count;
  }

  public void matchCarriedEntitiesToCount(LivingEntity player, int maxCount) {
    int count = 0;
    // get top rider
    Entity ridden = player;
    while (ridden.isVehicle()) {
      ridden = ridden.getPassengers().get(0);
      count++;

      if (count > maxCount) {
        ridden.stopRiding();
      }
    }
  }

  @Override
  public void inventoryTick(ItemStack stack, ServerLevel worldIn, Entity entityIn, @Nullable EquipmentSlot slot) {
    if (entityIn instanceof LivingEntity livingEntity && livingEntity.getItemBySlot(EquipmentSlot.CHEST) == stack && entityIn.isVehicle()) {
      int amplifier = this.getEntitiesCarriedCount(livingEntity) - 1;
      livingEntity.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TinkerGadgets.carryEffect.get()), 2, amplifier, true, false, true));
    }
  }

  public static class CarryPotionEffect extends TinkerEffect {
    /** Identifier for the movement speed modifier applied by this effect */
    static final Identifier MODIFIER_ID = TConstruct.getResource("piggyback");

    public CarryPotionEffect() {
      super(MobEffectCategory.NEUTRAL, true);

      this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MODIFIER_ID, -0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true; // check every tick
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, @Nonnull LivingEntity livingEntityIn, int p_76394_2_) {
      ItemStack chestArmor = livingEntityIn.getItemBySlot(EquipmentSlot.CHEST);
      if (chestArmor.isEmpty() || chestArmor.getItem() != TinkerGadgets.piggyBackpack.get()) {
        TinkerGadgets.piggyBackpack.get().matchCarriedEntitiesToCount(livingEntityIn, 0);
      } else {
        TinkerGadgets.piggyBackpack.get().matchCarriedEntitiesToCount(livingEntityIn, chestArmor.getCount());
        if (livingEntityIn instanceof Player player) {
          PiggybackCapability.get(player).updatePassengers(player);
        }
      }
      return true;
    }

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
      consumer.accept(new IClientMobEffectExtensions() {
        /** Mob effect textures are GUI sprites now, textures/mob_effect/carry.png is the sprite tconstruct:mob_effect/carry */
        private static final Identifier[] ICONS = {
          TConstruct.getResource("mob_effect/carry"),
          TConstruct.getResource("mob_effect/carry_2"),
          TConstruct.getResource("mob_effect/carry_3")
        };

        /** Common logic to render the icon */
        private void renderIcon(MobEffectInstance effect, GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
          int amplifier = effect.getAmplifier();
          if (amplifier > 2) {
            amplifier = 2;
          }
          graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICONS[amplifier], x, y, width, height, color);
        }

        // positions are already aligned to the icon by NeoForge, unlike the old render hooks
        @Override
        public boolean extractInventoryIcon(MobEffectInstance effect, AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
          renderIcon(effect, graphics, x, y, width, height, color);
          return true;
        }

        @Override
        public boolean extractHudIcon(MobEffectInstance effect, Hud hud, GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
          renderIcon(effect, graphics, x, y, width, height, color);
          return true;
        }
      });
    }
  }
}
