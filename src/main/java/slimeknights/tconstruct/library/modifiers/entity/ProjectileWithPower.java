package slimeknights.tconstruct.library.modifiers.entity;

import net.minecraft.world.entity.projectile.Projectile;

/** Interface for a projectile with a power getter and setter, used by {@link slimeknights.tconstruct.library.modifiers.modules.combat.ConditionalPowerModule} */
public interface ProjectileWithPower {
  /** Gets the current power */
  float getPower();

  /** Gets the amount of damage this projectile will deal. Used by {@link slimeknights.tconstruct.library.tools.helper.ModifierUtil#getProjectileDamage(Projectile)} */
  default float getDamage() {
    return getPower();
  }

  /** Sets the power to the new value */
  void setPower(float power);
}
