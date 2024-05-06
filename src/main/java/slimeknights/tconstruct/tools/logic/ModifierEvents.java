package slimeknights.tconstruct.tools.logic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.armor.EffectImmunityModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorStatModule;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.helper.ModifierLootingHandler;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.data.ModifierIds;
import slimeknights.tconstruct.tools.modules.ranged.RestrictAngleModule;

/** Events to implement modifier specific behaviors, such as those defined by {@link TinkerDataKeys}. General hooks will typically be in {@link ToolEvents} */
@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.FORGE)
public class ModifierEvents {
  @SubscribeEvent
  static void onKnockback(LivingKnockBackEvent event) {
    event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
      float knockback = data.get(TinkerDataKeys.KNOCKBACK, 0f);
      if (knockback != 0) {
        // adds +20% knockback per level
        event.setStrength(event.getStrength() * (1 + knockback));
      }
      // apply crystalbound bonus
      int crystalbound = data.get(TinkerDataKeys.CRYSTALSTRIKE, 0);
      if (crystalbound > 0) {
        RestrictAngleModule.onKnockback(event, crystalbound);
      }
    });
  }

  /** Reduce fall distance for fall damage */
  @SubscribeEvent
  static void onLivingFall(LivingFallEvent event) {
    LivingEntity entity = event.getEntity();
    float boost = ArmorStatModule.getStat(entity, TinkerDataKeys.JUMP_BOOST);
    if (boost > 0) {
      event.setDistance(Math.max(event.getDistance() - boost, 0));
    }
  }

  /** Called on jumping to boost the jump height of the entity */
  @SubscribeEvent
  public static void onLivingJump(LivingJumpEvent event) {
    LivingEntity entity = event.getEntity();
    float boost = ArmorStatModule.getStat(entity, TinkerDataKeys.JUMP_BOOST);
    if (boost > 0) {
      entity.setDeltaMovement(entity.getDeltaMovement().add(0, boost * 0.1, 0));
    }
  }

  /** Prevents effects on the entity */
  @SubscribeEvent
  static void isPotionApplicable(MobEffectEvent.Applicable event) {
    event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
      if (data.computeIfAbsent(EffectImmunityModule.EFFECT_IMMUNITY).contains(event.getEffectInstance().getEffect())) {
        event.setResult(Result.DENY);
      }
    });
  }


  /* Experience */

  /** Multiplier for experience drops from events */
  private static final TinkerDataKey<Float> PROJECTILE_EXPERIENCE = TConstruct.createKey("projectile_experience");

  /**
   * Boosts the original based on the level
   * @param original  Original amount
   * @param bonus     Bonus percent
   * @return  Boosted XP
   */
  private static int boost(int original, float bonus) {
    return (int) (original  * (1 + bonus));
  }

  @SubscribeEvent
  static void beforeBlockBreak(BreakEvent event) {
    float bonus = ArmorStatModule.getStat(event.getPlayer(), TinkerDataKeys.EXPERIENCE);
    if (bonus != 0) {
      event.setExpToDrop(boost(event.getExpToDrop(), bonus));
    }
  }

  @SubscribeEvent
  static void onEntityKilled(LivingDeathEvent event) {
    // if a projectile kills the target, mark the projectile level
    DamageSource source = event.getSource();
    if (source != null && source.getDirectEntity() instanceof Projectile projectile) {
      ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(projectile);
      if (!modifiers.isEmpty()) {
        event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.put(PROJECTILE_EXPERIENCE, modifiers.getEntry(ModifierIds.experienced).getEffectiveLevel()));
      }
    }
  }

  @SubscribeEvent
  static void onExperienceDrop(LivingExperienceDropEvent event) {
    // always add armor boost, unfortunately no good way to stop shield stuff here
    float armorBoost = 0;
    Player player = event.getAttackingPlayer();
    if (player != null) {
      armorBoost = ArmorStatModule.getStat(player, TinkerDataKeys.EXPERIENCE);
    }
    // if the target was killed by an experienced arrow, use that level
    float projectileBoost = event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).resolve().map(data -> data.get(PROJECTILE_EXPERIENCE)).orElse(-1f);
    if (projectileBoost > 0) {
      event.setDroppedExperience(boost(event.getDroppedExperience(), projectileBoost * 0.5f + armorBoost));
      // experienced being zero means it was our arrow but it was not modified, do not check the held item in that case
    } else if (projectileBoost != 0 && player != null) {
      // not an arrow, just use the player's experienced level
      ToolStack tool = Modifier.getHeldTool(player, ModifierLootingHandler.getLootingSlot(player));
      float boost = (tool != null ? tool.getModifier(ModifierIds.experienced).getEffectiveLevel() : 0) * 0.5f + armorBoost;
      if (boost > 0) {
        event.setDroppedExperience(boost(event.getDroppedExperience(), boost));
      }
    }
  }
}
