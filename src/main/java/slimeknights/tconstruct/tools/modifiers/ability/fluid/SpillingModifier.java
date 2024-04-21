package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.fluid.TankModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import static slimeknights.tconstruct.tools.modifiers.ability.fluid.UseFluidOnHitModifier.spawnParticles;

/** Modifier applying fluid effects on melee hit */
public class SpillingModifier extends Modifier implements MeleeHitModifierHook {
  protected TankModule tank;

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    tank = new TankModule(FluidType.BUCKET_VOLUME, true);
    hookBuilder.addModule(tank);
    hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
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
}
