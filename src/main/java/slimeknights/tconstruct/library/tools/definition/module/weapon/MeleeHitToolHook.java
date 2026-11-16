package slimeknights.tconstruct.library.tools.definition.module.weapon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/** Attack logic for a modifiable weapon */
public interface MeleeHitToolHook {
  /**
   * Deals damage using the tool
   * @param tool    Tool instance
   * @param context  Attack context
   * @param damage   Damage to deal
   */
  void afterMeleeHit(IToolStackView tool, ToolAttackContext context, float damage);

  /** Deals damage using the given tool, applying any post damage effects */
  static boolean dealDamage(IToolStackView tool, ToolAttackContext context, float damage) {
    // 1.21.2 made Entity#hurt return nothing, attacks are server side so use the server damage method
    Entity target = context.getTarget();
    boolean hit = target.level() instanceof ServerLevel server && target.hurtServer(server, context.makeDamageSource(), damage);
    if (hit) {
      tool.getHook(ToolHooks.MELEE_HIT).afterMeleeHit(tool, context, damage);
    }
    return hit;
  }

  /** Merger that runs all hooks */
  record AllMerger(Collection<MeleeHitToolHook> hooks) implements MeleeHitToolHook {
    @Override
    public void afterMeleeHit(IToolStackView tool, ToolAttackContext context, float damage) {
      for (MeleeHitToolHook hook : hooks) {
        hook.afterMeleeHit(tool, context, damage);
      }
    }
  }
}
