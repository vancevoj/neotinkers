package slimeknights.tconstruct.shared.effect;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.shared.TinkerAttributes;

import java.util.function.IntFunction;

/** Logic for handling the antigravity effect */
public class AntigravityEffect extends TinkerEffect {
  /** Map of previous tick velocities for affected entities. Working under the assumption there are not many anti-gravity entities */
  private final Int2ObjectMap<LastVelocity> LAST_VELOCITY = new Int2ObjectArrayMap<>();
  /** Cached constructor as we call this every tick */
  private final IntFunction<LastVelocity> CONSTRUCTOR = i -> new LastVelocity();

  public AntigravityEffect() {
    super(MobEffectCategory.HARMFUL, 0xff970d, true);
    this.addAttributeModifier(Attributes.GRAVITY, TConstruct.getResource("effect.antigravity"), -2, Operation.ADD_MULTIPLIED_TOTAL);
    NeoForge.EVENT_BUS.addListener(this::onLivingJump);
    // since 1.21.2 effects only tick on the server, but the movement must also run on the client as players move themselves
    NeoForge.EVENT_BUS.addListener(EntityTickEvent.Pre.class, this::onClientEntityTick);
    // removeAttributeModifiers no longer receives the entity, so clear the tracker when the effect ends
    NeoForge.EVENT_BUS.addListener(MobEffectEvent.Remove.class, event -> onEffectEnded(event.getEntity(), event.getEffect()));
    NeoForge.EVENT_BUS.addListener(MobEffectEvent.Expired.class, event -> {
      MobEffectInstance instance = event.getEffectInstance();
      if (instance != null) {
        onEffectEnded(event.getEntity(), instance.getEffect());
      }
    });
  }

  /** Holder for this effect, used for self-checks */
  private Holder<MobEffect> selfHolder() {
    return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this);
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return true;
  }

  /** Tracks the last velocity for the entity */
  @ToString
  private static class LastVelocity {
    private double lastTick = 0;
    private double twoTicks = 0;

    /** Updates the last velocity */
    void update(double current) {
      twoTicks = lastTick;
      lastTick = current;
    }
  }

  /** Handles movement while under anti-gravity */
  @Override
  public boolean applyEffectTick(ServerLevel level, LivingEntity living, int amplifier) {
    // ensure we are actually under the effects of antigrav, might have a double negative
    if (living.getAttributeValue(Attributes.GRAVITY) < 0) {
      // 6100 meters is when it starts becoming hard to breathe, assuming world height is 320
      // really just need some arbitrarily big number to start damaging entities so you don't get entities falling up forever
      // max build height used to be exclusive, max Y is inclusive
      if (living.getY() > level.getMaxY() + 1 + 5780) {
        living.hurtServer(level, level.damageSources().fellOutOfWorld(), 4.0F);
      }
      applyMovement(level, living);
    }
    return true;
  }

  /** Runs the movement logic client side, as effects no longer tick on the client */
  private void onClientEntityTick(EntityTickEvent.Pre event) {
    if (event.getEntity() instanceof LivingEntity living && living.level().isClientSide() && living.hasEffect(selfHolder()) && living.getAttributeValue(Attributes.GRAVITY) < 0) {
      applyMovement(living.level(), living);
    }
  }

  /** Clears the velocity tracker for the given entity when this effect ends */
  private void onEffectEnded(LivingEntity living, Holder<MobEffect> effect) {
    if (effect.value() == this) {
      LAST_VELOCITY.remove(living.getId());
    }
  }

  /** Handles bouncing off ceilings, climbing and friction while under anti-gravity */
  private void applyMovement(Level level, LivingEntity living) {
    LastVelocity lastVelocity = LAST_VELOCITY.computeIfAbsent(living.getId(), CONSTRUCTOR);
    if (living.verticalCollision && !living.verticalCollisionBelow) {
      if (lastVelocity.twoTicks > 0.1f) {
        Vec3 velocity = living.getDeltaMovement();
        living.setDeltaMovement(velocity.x, lastVelocity.twoTicks, velocity.z);
        BlockPos above = BlockPos.containing(living.getX(), living.getBoundingBox().maxY + 0.1, living.getZ());
        BlockState hit = level.getBlockState(above);
        double height = lastVelocity.twoTicks * 10 - 3 - living.getAttributeValue(TinkerAttributes.SAFE_FALL_DISTANCE) - TinkerEffect.getLevel(living, MobEffects.JUMP_BOOST);
        if (height > 0.0) {
          hit.getBlock().fallOn(level, hit, above, living, height);
        }
        // 26.2 replaced updateEntityAfterFallOn with bounce restitution. A plain block stops the entity like the old default,
        // a bouncy block bounces it back away from the ceiling
        float restitution = hit.getBounceRestitution(level, above, living);
        Vec3 hitVelocity = living.getDeltaMovement();
        living.setDeltaMovement(hitVelocity.x, -hitVelocity.y * restitution, hitVelocity.z);
        if (restitution > 0) {
          hit.getBlock().bounceOn(level, hit, above, living, living.fallDistance);
        }
      }
      lastVelocity.update(0);
    } else {
      lastVelocity.update(living.getDeltaMovement().y);
    }

    // handle falling up ladders
    Vec3 velocity = living.getDeltaMovement();
    double y = velocity.y;
    if (living.onClimbable()) {
      // moving forwards or jumping means climb
      if (living.horizontalCollision || living.jumping) {
        y = -0.2;
      }
      // shift means stop moving
      else if (y > 0 && living.isSuppressingSlidingDownLadder() && living instanceof Player) {
        y = 0;
        // otherwise slow descent
      } else if (y > 0.15f) {
        y = 0.15f;
      }
    }
    // handle friction
    float friction = 1f;
    if (living.verticalCollision && !living.verticalCollisionBelow && !living.shouldDiscardFriction()) {
      BlockPos above = BlockPos.containing(living.getX(), living.getBoundingBox().maxY + 0.1, living.getZ());
      friction = level.getBlockState(above).getFriction(level, above, living);
    }
    // update speed based on ladders and friction
    living.setDeltaMovement(velocity.x * friction, y, velocity.z * friction);
  }

  /** Handles making the player jump down instead of up */
  private void onLivingJump(LivingJumpEvent event) {
    // handles jumping down instead of up
    LivingEntity entity = event.getEntity();
    if (entity.hasEffect(selfHolder()) && entity.getAttributeValue(Attributes.GRAVITY) < 0) {
      Vec3 movement = entity.getDeltaMovement();
      entity.setDeltaMovement(movement.x, -movement.y, movement.z);
    }
  }

  /** Called by controls to jump when the player is affected by negative gravity */
  public boolean antigravityJump(Player player) {
    // must be on the ground, not swimming, not on a ladder, and have antigravity to jump
    // jump reversal is handled in ModifierEvents to ensure ordering between that and the attribute boost
    if (player.verticalCollision && !player.verticalCollisionBelow && !player.isInWater()
      && player.hasEffect(selfHolder()) && player.getAttributeValue(Attributes.GRAVITY) < 0 && !player.onClimbable()) {
      player.jumpFromGround();
      return true;
    }
    return false;
  }
}
