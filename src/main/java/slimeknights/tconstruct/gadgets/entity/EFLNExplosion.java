package slimeknights.tconstruct.gadgets.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Custom explosion logic for EFLNs, more spherical and less random, plus works underwater and more control over damage amount.
 * Loosely, the distinction between this and a regular explosion is this simply breaks all blocks within range if the power is high enough.
 * A normal explosion does a bunch of ray casts from the center, and those ray casts can be interrupted by blocks with high enough resistance (such as water).
 */
public class EFLNExplosion extends Explosion {
  /** Damage multiplier; setting to 7 will match the vanilla explosion. */
  private final double damage;
  public EFLNExplosion(Level world, @Nullable Entity entity, double damage, @Nullable DamageSource source, double x, double y, double z, float size, boolean causesFire, BlockInteraction mode) {
    super(world, entity, source, null, x, y, z, size, causesFire, mode);
    this.damage = damage;
  }

  @Override
  public void explode() {
    this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));

    // we do a sphere of a certain radius, and check if the blockpos is inside the radius
    float radius = this.radius * this.radius;
    int range = (int) radius + 1;

    Set<BlockPos> set = new HashSet<>();
    for (int x = -range; x < range; ++x) {
      for (int y = -range; y < range; ++y) {
        for (int z = -range; z < range; ++z) {
          int distance = x * x + y * y + z * z;
          // inside the sphere?
          if (distance <= radius) {
            BlockPos blockpos = new BlockPos(x, y, z).offset(Mth.floor(this.x), Mth.floor(this.y), Mth.floor(this.z));
            // no air blocks
            if (this.level.isEmptyBlock(blockpos)) {
              continue;
            }

            // explosion "strength" at the current position
            float strength = this.radius * (1f - distance / (radius));
            BlockState blockstate = this.level.getBlockState(blockpos);

            FluidState fluid = this.level.getFluidState(blockpos);
            float power = Math.max(blockstate.getExplosionResistance(this.level, blockpos, this), fluid.getExplosionResistance(this.level, blockpos, this));
            if (this.source != null) {
              power = this.source.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluid, power);
            }

            strength -= (power + 0.3F) * 0.3F;

            if (strength > 0.0F && (this.source == null || this.source.shouldBlockExplode(this, this.level, blockpos, blockstate, strength))) {
              set.add(blockpos);
            }
          }
        }
      }
    }
    this.toBlow.addAll(set);

    // damage and blast back entities
    float diameter = this.radius * 2;
    List<Entity> list = this.level.getEntities(
      this.source,
      new AABB(Math.floor(this.x - diameter - 1),
               Math.floor(this.y - diameter - 1),
               Math.floor(this.z - diameter - 1),
               Math.floor(this.x + diameter + 1),
               Math.floor(this.y + diameter + 1),
               Math.floor(this.z + diameter + 1)),
      entity -> entity != null && !entity.ignoreExplosion() && !entity.isSpectator() && entity.isAlive());
    ForgeEventFactory.onExplosionDetonate(this.level, this, list, diameter);

    // start pushing entities
    Vec3 center = new Vec3(this.x, this.y, this.z);
    for (Entity entity : list) {
      Vec3 dir = entity.position().subtract(center);
      double length = dir.length();
      double distance = length / diameter;
      if (distance <= 1) {
        // non-TNT uses eye height for explosion direction
        if (!(entity instanceof PrimedTnt)) {
          dir = dir.add(0, entity.getEyeY() - entity.getY(), 0);
          length = dir.length();
        }
        if (length > 1.0E-4D) {
          double strength = (1.0D - distance) * getSeenPercent(center, entity);
          entity.hurt(this.getDamageSource(), (int)((strength * strength + strength) / 2 * damage * diameter + 1));

          // apply enchantment
          double reducedStrength = strength;
          if (entity instanceof LivingEntity living) {
            reducedStrength = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, strength);
          }
          Vec3 velocity = dir.scale(reducedStrength / length);
          entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));
          if (entity instanceof Player player) {
            if (!player.isCreative() || !player.getAbilities().flying) {
              this.getHitPlayers().put(player, velocity);
            }
          }
        }
      }
    }
  }

  /** Syncs this explosion to the client */
  public void syncToClient(Level level) {
    if (!level.isClientSide && level instanceof ServerLevel server) {
      List<BlockPos> toBlow = interactsWithBlocks() ? getToBlow() : List.of();
      Map<Player,Vec3> hitPlayers = getHitPlayers();
      for (ServerPlayer player : server.players()) {
        if (player.distanceToSqr(getPosition()) < 4096.0D) {
          player.connection.send(new ClientboundExplodePacket(x, y, z, radius, toBlow, hitPlayers.get(player)));
        }
      }
    }
  }
}
