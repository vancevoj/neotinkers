package slimeknights.tconstruct.tools.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.util.CombatHelper;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithKnockback;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithPower;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.tools.TinkerTools;

/** Fishing hook that deals damage and can be used as a grappling hook */
public class CombatFishingHook extends FishingHook implements ProjectileWithKnockback, ProjectileWithPower {
  private static final float PI = (float) Math.PI;

  /** Damage dealt by the fishing hook */
  @Getter @Setter
  private float power = 0;
  /** Extra power for pulling entities towards ourself */
  private float knockback = 0;
  /** Velocity at the time the projectile hit the entity, used for damage calculations */
  private double impactVelocity = 1;

  public CombatFishingHook(EntityType<? extends FishingHook> pEntityType, Level pLevel) {
    super(pEntityType, pLevel);
  }

  // set velocity to 0.6 for vanilla behavior
  public CombatFishingHook(Player player, Level level, int luck, int lure, float velocity, float inaccuracy) {
    super(TinkerTools.fishingHook.get(), level, luck, lure);
    this.setOwner(player);
    float xRot = player.getXRot();
    float yRot = player.getYRot();
    float yAngle = (-yRot * PI / 180f) - PI;
    float dz = Mth.cos(yAngle);
    float dx = Mth.sin(yAngle);
    // position
    this.moveTo(
      player.getX() - dx * 0.3,
      player.getEyeY(),
      player.getZ() - dz * 0.3,
      yRot, xRot);
    // speed
    float xAngle = -xRot * (PI / 180F);
    float yCos = -Mth.cos(xAngle);
    float ySin = Mth.sin(xAngle);
    Vec3 deltaMovement = new Vec3(-dx, Mth.clamp(-ySin / yCos, -5f, 5f), -dz);
    double length = deltaMovement.length();
    // TODO: inaccuracy
    double maxRandom = 0.0103365 * inaccuracy;
    deltaMovement = deltaMovement.multiply(
      velocity / length + this.random.triangle(0.5, maxRandom),
      velocity / length + this.random.triangle(0.5, maxRandom),
      velocity / length + this.random.triangle(0.5, maxRandom));
    this.setDeltaMovement(deltaMovement);
    this.setYRot((float)(Mth.atan2(deltaMovement.x, deltaMovement.z) * (180 / PI)));
    this.setXRot((float)(Mth.atan2(deltaMovement.y, deltaMovement.horizontalDistance()) * (180 / PI)));
    this.yRotO = this.getYRot();
    this.xRotO = this.getXRot();
  }

  @Override
  public void addKnockback(float amount) {
    this.knockback += amount;
  }


  /* Damage and knockback */

  @Override
  protected void onHitEntity(EntityHitResult result) {
    super.onHitEntity(result);
    // store the impact velocity to scale our damage later
    impactVelocity = this.getDeltaMovement().length();
  }

  @Override
  protected void pullEntity(Entity target) {
    Entity owner = this.getOwner();
    if (owner != null) {
      // TODO: probably want a modifier that prevents damage
      // TODO: consider a tag for the immune entities instead of just the instance of check
      if (power > 0 && !(target instanceof ItemEntity)) {
        // mark target hurt
        if (owner instanceof LivingEntity living) {
          living.setLastHurtMob(target);
        }
        // setup damage
        int damage = Mth.ceil(Mth.clamp(this.impactVelocity * this.power, 0, Integer.MAX_VALUE));
        DamageSource source = CombatHelper.damageSource(TinkerDamageTypes.FISHING_HOOK, this, owner);
        LivingEntity targetLiving = target instanceof LivingEntity l ? l : null;
        // don't want to apply default knockback, we will apply our own later in the opposite direction
        AttributeInstance knockback = ToolAttackUtil.disableKnockback(targetLiving);
        // actually hurt the entity
        if (target.hurt(source, damage)) {
          if (!this.level().isClientSide && owner instanceof LivingEntity ownerLiving) {
            if (targetLiving != null) {
              EnchantmentHelper.doPostHurtEffects(targetLiving, owner);
            }
            EnchantmentHelper.doPostDamageEffects(ownerLiving, target);
          }
        }
        ToolAttackUtil.enableKnockback(knockback);
      }
      // pull the target, bonus pulling if we have punch
      Vec3 knockback = new Vec3(owner.getX() - this.getX(), owner.getY() - this.getY(), owner.getZ() - this.getZ()).scale(0.1 + 0.05 * this.knockback);
      target.setDeltaMovement(target.getDeltaMovement().add(knockback));
    }
  }
}
