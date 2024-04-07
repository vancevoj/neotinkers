package slimeknights.tconstruct.tools.modifiers.ability.tool;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolModuleHooks;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.ability.armor.UseFluidOnHitModifier;

import javax.annotation.Nullable;

import static slimeknights.tconstruct.library.tools.helper.ModifierUtil.asLiving;

/** Modifier to handle spilling recipes */
public class SpillingModifier extends UseFluidOnHitModifier implements EntityInteractionModifierHook, OnAttackedModifierHook, MeleeHitModifierHook {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, TinkerHooks.ENTITY_INTERACT, TinkerHooks.ON_ATTACKED, TinkerHooks.MELEE_HIT);
  }

  @Override
  public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
    if (damageDealt > 0 && context.isFullyCharged()) {
      FluidStack fluid = tank.getFluid(tool);
      if (!fluid.isEmpty()) {
        FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
        if (recipe.hasEntityEffects()) {
          LivingEntity living = context.getAttacker();
          Player player = context.getPlayerAttacker();
          int consumed = recipe.applyToEntity(fluid, modifier.getEffectiveLevel(), new FluidEffectContext.Entity(living.level, living, player, null, context.getTarget(), context.getLivingTarget()), FluidAction.EXECUTE);
          if (consumed > 0 && (player == null || !player.isCreative())) {
            spawnParticles(context.getTarget(), fluid);
            fluid.shrink(consumed);
            tank.setFluid(tool, fluid);
          }
        }
      }
    }
  }

  @Override
  public InteractionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, Player player, Entity target, InteractionHand hand, InteractionSource source) {
    // melee items get spilling via attack, non melee interact to use it
    // TODO: reconsider whether this shouldn't be a separate modifier
    if (source != InteractionSource.ARMOR && !tool.hasTag(TinkerTags.Items.MELEE) && tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
      FluidStack fluid = tank.getFluid(tool);
      if (!fluid.isEmpty()) {
        FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
        if (recipe.hasEntityEffects()) {
          if (!player.level.isClientSide) {
            // for the main target, consume fluids
            float level = modifier.getEffectiveLevel();
            int numTargets = 0;
            int consumed = recipe.applyToEntity(fluid, level, new FluidEffectContext.Entity(player.level, player, player, null, target, asLiving(target)), FluidAction.EXECUTE);
            if (consumed > 0) {
              numTargets++;
              spawnParticles(target, fluid);
            }

            // expanded logic, they do not consume fluid, you get some splash for free
            float range = 1 + tool.getModifierLevel(TinkerModifiers.expanded.get());
            float rangeSq = range * range;
            for (Entity aoeTarget : player.level.getEntitiesOfClass(Entity.class, target.getBoundingBox().inflate(range, 0.25, range))) {
              if (aoeTarget != player && aoeTarget != target && !(aoeTarget instanceof ArmorStand stand && stand.isMarker()) && target.distanceToSqr(aoeTarget) < rangeSq) {
                int aoeConsumed = recipe.applyToEntity(fluid, level, new FluidEffectContext.Entity(player.level, player, player, null, aoeTarget, asLiving(aoeTarget)), FluidAction.EXECUTE);
                if (aoeConsumed > 0) {
                  numTargets++;
                  spawnParticles(aoeTarget, fluid);
                  // consume the largest amount requested from any entity
                  if (aoeConsumed > consumed) {
                    consumed = aoeConsumed;
                  }
                }
              }
            }

            // consume the fluid last, if any target used fluid
            if (!player.isCreative() && consumed > 0) {
              fluid.shrink(consumed);
              tank.setFluid(tool, fluid);
            }

            // damage the tool, we charge for the multiplier and for the number of targets hit
            ToolDamageUtil.damageAnimated(tool, Mth.ceil(numTargets * level), player, hand);
          }

          // cooldown based on attack speed/draw speed. both are on the same scale and default to 1, we don't care which one the tool uses
          player.getCooldowns().addCooldown(tool.getItem(), (int)(20 / (tool.getStats().get(ToolStats.ATTACK_SPEED) * ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.DRAW_SPEED))));
          return InteractionResult.SUCCESS;
        }
      }
    }
    return InteractionResult.PASS;
  }

  @Override
  public FluidEffectContext.Entity createContext(LivingEntity self, @Nullable Player player, @Nullable Entity attacker) {
    assert attacker != null;
    return new FluidEffectContext.Entity(self.level, self, player, null, attacker, asLiving(attacker));
  }

  @Override
  protected boolean doesTrigger(DamageSource source, boolean isDirectDamage) {
    return source.getEntity() != null && isDirectDamage;
  }

  @Override
  public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
    useFluid(tool, modifier, context, slotType, source, isDirectDamage);
  }
}
