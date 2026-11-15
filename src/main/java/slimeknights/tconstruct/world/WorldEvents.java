package slimeknights.tconstruct.world;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.neoforge.event.entity.living.EndermanAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.world.item.EndermanHeadItem;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = TConstruct.MOD_ID)
public class WorldEvents {
  /* Heads */

  @SubscribeEvent
  static void livingVisibility(LivingVisibilityEvent event) {
    Entity lookingEntity = event.getLookingEntity();
    if (lookingEntity == null) {
      return;
    }
    ItemStack helmet = event.getEntity().getItemBySlot(EquipmentSlot.HEAD);
    Item item = helmet.getItem();
    if (item != Items.AIR && TinkerWorld.headItems.contains(item)) {
      if (lookingEntity.getType() == ((TinkerHeadType)((SkullBlock)((BlockItem)item).getBlock()).getType()).getType()) {
        event.modifyVisibility(0.5f);
      }
    }
  }

  /** Enderman heads act like a carved pumpkin, replaces the removed ender mask item hook */
  @SubscribeEvent
  static void endermanAnger(EndermanAngerEvent event) {
    if (EndermanHeadItem.isEnderMask(event.getPlayer())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  static void creeperKill(LivingDropsEvent event) {
    DamageSource source = event.getSource();
    if (source != null) {
      Entity entity = source.getEntity();
      // charged creepers drop one skull per explosion. Vanilla skulls come from the charged creeper loot table, which runs before this event
      if (entity instanceof Creeper creeper && creeper.isPowered() && !creeper.droppedSkulls) {
        LivingEntity dying = event.getEntity();
        TinkerHeadType headType = TinkerHeadType.fromEntityType(dying.getType());
        if (headType != null && Config.COMMON.headDrops.get(headType).get()) {
          creeper.droppedSkulls = true;
          // drops in the event are added to the world after the event, so just create the item entity
          ItemEntity drop = new ItemEntity(dying.level(), dying.getX(), dying.getY(), dying.getZ(), new ItemStack(TinkerWorld.heads.get(headType)));
          drop.setDefaultPickUpDelay();
          event.getDrops().add(drop);
        }
      }
    }
  }
}
