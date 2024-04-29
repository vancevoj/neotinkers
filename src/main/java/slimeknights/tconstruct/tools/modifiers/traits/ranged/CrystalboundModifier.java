package slimeknights.tconstruct.tools.modifiers.traits.ranged;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.behavior.AttributeModule;
import slimeknights.tconstruct.library.modifiers.modules.unserializable.ArmorLevelModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;

public class CrystalboundModifier extends Modifier implements ProjectileLaunchModifierHook, ToolStatsModifierHook {
  public static final TinkerDataKey<Integer> LEVEL = TConstruct.createKey("crystalbound");

  @Override
  protected void registerHooks(Builder hookBuilder) {
    hookBuilder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.TOOL_STATS);
    hookBuilder.addModule(new ArmorLevelModule(LEVEL, false, TinkerTags.Items.HELD_ARMOR));
    hookBuilder.addModule(AttributeModule.builder(Attributes.ATTACK_SPEED, Operation.MULTIPLY_TOTAL)
                                         .uniqueFrom(TinkerModifiers.crystalbound.getId())
                                         .toolItem(ItemPredicate.tag(TinkerTags.Items.ARMOR))
                                         .eachLevel(0.025f));
  }

  @Override
  public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
    ToolStats.VELOCITY.add(builder, modifier.getLevel() * 0.1f);
  }

  /** Shared angle logic */
  @SuppressWarnings("SuspiciousNameCombination") // mojang uses the angle between X and Z, but parchment named atan2 as the angle between Y and X, makes IDEA mad as it thinks parameters should swap
  private static Vec3 clampDirection(Vec3 direction, int level, @Nullable Projectile projectile) {
    double oldAngle = Mth.atan2(direction.x, direction.z);
    int possibleDirections = Math.max(4, (int)Math.pow(2, 6 - level)); // don't let directions fall below 4, else you start seeing directional biases
    double radianIncrements = 2 * Math.PI / possibleDirections;
    double newAngle = Math.round(oldAngle / radianIncrements) * radianIncrements;
    direction = direction.yRot((float)(newAngle - oldAngle));
    if (projectile != null) {
      projectile.setDeltaMovement(direction);
      projectile.setYRot((float)(newAngle * 180f / Math.PI));
    }
    return direction;
  }

  @Override
  public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, Projectile projectile, @Nullable AbstractArrow arrow, NamespacedNBT persistentData, boolean primary) {
    clampDirection(projectile.getDeltaMovement(), modifier.getLevel(), projectile);
  }

  /** Called during the living knockback event to apply our effect */
  public static void onKnockback(LivingKnockBackEvent event, int level) {
    // start at 4 directions at level 1, then 32, 16, 8, and 4 by level 4, don't go below 4 directions
    Vec3 direction = clampDirection(new Vec3(event.getRatioX(), 0, event.getRatioZ()), level, null);
    event.setRatioX(direction.x);
    event.setRatioZ(direction.z);
  }
}
