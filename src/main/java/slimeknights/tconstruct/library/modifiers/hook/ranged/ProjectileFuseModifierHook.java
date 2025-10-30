package slimeknights.tconstruct.library.modifiers.hook.ranged;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;
import java.util.Collection;

/** Hook that runs when {@link slimeknights.tconstruct.tools.modules.ranged.ammo.ProjectileFuseModule} hits the expiry time. */
public interface ProjectileFuseModifierHook {
  /**
   * Called when {@link slimeknights.tconstruct.tools.modules.ranged.ammo.ProjectileFuseModule} is about to remove the projectile to trigger extra effects.
   * @param tool            Ammo tool instance.
   * @param modifier        Modifier being used
   * @param ammo            Ammo stack used to fire this projectile.
   * @param projectile      Acting projectile
   * @param arrow           Acting arrow
   * @param persistentData  Persistent data instance stored on the arrow to write arbitrary data.
   */
  void onProjectileFuseFinish(IToolStackView tool, ModifierEntry modifier, ItemStack ammo, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData);

  /** Merger running each hook in sequence */
  record AllMerger(Collection<ProjectileFuseModifierHook> modules) implements ProjectileFuseModifierHook {
    @Override
    public void onProjectileFuseFinish(IToolStackView tool, ModifierEntry modifier, ItemStack ammo, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData) {
      for (ProjectileFuseModifierHook module : modules) {
        module.onProjectileFuseFinish(tool, modifier, ammo, projectile, arrow, persistentData);
      }
    }
  }
}
