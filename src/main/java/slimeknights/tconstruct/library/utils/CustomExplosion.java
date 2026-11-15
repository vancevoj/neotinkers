package slimeknights.tconstruct.library.utils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Helper class for more control over explosions.
 * <p>
 * Explosions only exist on the server since 1.21.2, so this extends {@link ServerExplosion} and replaces its whole {@link #explode()},
 * as the vanilla steps are private. The client receives the explosion through {@link #syncToClient(int)}.
 */
public class CustomExplosion extends ServerExplosion {
  /** Size of the hollowed out cube determining the number of rays to cast */
  private static final int RAY_COUNT = 16;
  private static final int MAX_RAY = RAY_COUNT - 1;
  /** Max number of items merged into a single stack when dropping, matches vanilla */
  private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
  /** Default predicate for which entities to match */
  public static final Predicate<Entity> DEFAULT_ENTITY_PREDICATE = entity -> entity != null && entity.isAlive() && !entity.isSpectator();

  /* Copies of the private explosion fields, for use by subclasses */
  protected final ServerLevel level;
  protected final double x, y, z;
  protected final float radius;
  @Nullable
  protected final Entity source;
  protected final boolean fire;
  protected final ExplosionDamageCalculator damageCalculator;
  /** Blocks to destroy, filled by {@link #calculateHitBlocks()} */
  protected final List<BlockPos> toBlow = new ObjectArrayList<>();

  /** Maximum damage to deal; setting to 7*2*radius will match the vanilla explosion. */
  protected final float damage;
  /** Entity knockback scale. May be 0 to prevent knockback or negative to reverse knockback */
  protected final float knockback;
  /** Determines which entities are affected by this explosion */
  protected final Predicate<Entity> entityPredicate;
  /** If true, explosion damage bypasses the invulnerability time */
  protected final boolean bypassInvulnerableTime;

  public CustomExplosion(ServerLevel level, Vec3 location, float radius, @Nullable Entity sourceEntity, @Nullable Predicate<Entity> entityPredicate, float damage, @Nullable DamageSource damageSource, float knockback, @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire, BlockInteraction blockInteraction, boolean bypassInvulnerableTime) {
    super(level, sourceEntity, damageSource, damageCalculator, location, radius, placeFire, blockInteraction);
    this.level = level;
    this.x = location.x;
    this.y = location.y;
    this.z = location.z;
    this.radius = radius;
    this.source = sourceEntity;
    this.fire = placeFire;
    // same default as the vanilla explosion
    this.damageCalculator = damageCalculator != null ? damageCalculator : sourceEntity != null ? new EntityBasedExplosionDamageCalculator(sourceEntity) : new ExplosionDamageCalculator();
    this.entityPredicate = Objects.requireNonNullElse(entityPredicate, DEFAULT_ENTITY_PREDICATE);
    this.damage = damage;
    this.knockback = knockback;
    this.bypassInvulnerableTime = bypassInvulnerableTime;
  }

  public CustomExplosion(ServerLevel level, Vec3 location, float radius, @Nullable Entity sourceEntity, @Nullable Predicate<Entity> entityPredicate, float damage, @Nullable DamageSource damageSource, float knockback, @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire, BlockInteraction blockInteraction) {
    this(level, location, radius, sourceEntity, entityPredicate ,damage, damageSource, knockback, damageCalculator, placeFire, blockInteraction, false);
  }

  /** Checks if this explosion breaks blocks. Vanilla's version is private. */
  protected boolean interactsWithBlocks() {
    return getBlockInteraction() != BlockInteraction.KEEP;
  }

  /**
   * Runs the explosion: hits entities, then breaks blocks and places fire.
   * Replaces the 1.21.1 {@code explode()} plus {@code finalizeExplosion(boolean)}.
   * @return  Number of blocks hit
   */
  @Override
  public int explode() {
    this.level.gameEvent(this.source, GameEvent.EXPLODE, center());
    toBlow.clear();
    calculateHitBlocks();
    damageAndPushEntities();
    if (interactsWithBlocks()) {
      interactWithBlocks();
    }
    if (fire) {
      createFire();
    }
    return toBlow.size();
  }

  /** Calculates the list of blocks to hit, adding them to {@link #toBlow}; the actual block damage happens in {@link #explode()} after damaging entities */
  protected void calculateHitBlocks() {
    // optimization: if we are not interacting with blocks, no need to calculate blocks
    if (!interactsWithBlocks() && !fire) {
      return;
    }

    Set<BlockPos> set = new HashSet<>();
    // loop over a hollowed out 16x cube
    for (int rayX = 0; rayX < RAY_COUNT; rayX++) {
      for (int rayY = 0; rayY < RAY_COUNT; rayY++) {
        for (int rayZ = 0; rayZ < RAY_COUNT; rayZ++) {
          if (rayX == 0 || rayX == MAX_RAY || rayY == 0 || rayY == MAX_RAY || rayZ == 0 || rayZ == MAX_RAY) {
            // determine direction to go, then step in 0.3 unit vector increments
            double stepX = rayX * 2.0 / MAX_RAY - 1;
            double stepY = rayY * 2.0 / MAX_RAY - 1;
            double stepZ = rayZ * 2.0 / MAX_RAY - 1;
            double stepScale = 0.3f / Math.sqrt(stepX * stepX + stepY * stepY + stepZ * stepZ);
            stepX *= stepScale;
            stepY *= stepScale;
            stepZ *= stepScale;

            // keep moving in the direction of the ray until we run out of power; means blocks with high blast resistance shield those with less
            double targetX = this.x;
            double targetY = this.y;
            double targetZ = this.z;
            for (float power = this.radius * (0.7f + level.getRandom().nextFloat() * 0.6f); power > 0; power -= 0.225f) {
              BlockPos target = BlockPos.containing(targetX, targetY, targetZ);
              BlockState block = level.getBlockState(target);
              FluidState fluid = level.getFluidState(target);
              if (!level.isInWorldBounds(target)) {
                break;
              }

              // reduce power based on blast resistance
              Optional<Float> resistance = damageCalculator.getBlockExplosionResistance(this, level, target, block, fluid);
              if (resistance.isPresent()) {
                power -= (resistance.get() + 0.3f) * 0.3f;
              }

              // remove block if power is high enough
              // optimization: skip air if not placing fires to save network traffic
              if ((fire || !block.isAir()) && power > 0 && damageCalculator.shouldBlockExplode(this, level, target, block, power)) {
                set.add(target);
              }

              // vanilla difference - we moved the 0.3 multiplier to the original step variables to avoid needing to compute as often
              targetX += stepX;
              targetY += stepY;
              targetZ += stepZ;
            }
          }
        }
      }
    }
    toBlow.addAll(set);
  }

  /** Called to run the logic for damaging and blasting back entities in range */
  protected void damageAndPushEntities() {
    // skip running if we disabled both damage and knockback as there is nothing left to do
    if (damage <= 0 && knockback == 0) {
      return;
    }

    float diameter = this.radius * 2;
    // small behavior change: we filter the list of entities on fetch, meaning the forge event gets the filtered list
    List<Entity> list = this.level.getEntities(
      this.source,
      new AABB(Math.floor(this.x - diameter - 1),
               Math.floor(this.y - diameter - 1),
               Math.floor(this.z - diameter - 1),
               Math.floor(this.x + diameter + 1),
               Math.floor(this.y + diameter + 1),
               Math.floor(this.z + diameter + 1)),
      entityPredicate);
    EventHooks.onExplosionDetonate(this.level, this, list, toBlow);

    // start pushing entities
    // this logic is for the most part identical to vanilla, except taking better advantage of vec3
    Vec3 center = center();
    DamageSource damageSource = getDamageSource();
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
        // vanilla change: a bit of tolerance on the length check to match normalize
        if (length > 1.0E-4D) {
          double strength = (1 - distance) * getSeenPercent(center, entity);
          // vanilla change: instead of multiplying the damage by 7, we make that a parameter, which can be 0 for no damage
          if (damage > 0) {
            int toDeal = (int) ((strength * strength + strength) / 2 * damage + 1);
            if (bypassInvulnerableTime) {
              ToolAttackUtil.hurtNoInvulnerableTime(entity, damageSource, toDeal);
            } else {
              entity.hurtServer(this.level, damageSource, toDeal);
            }
          }

          // apply enchantment to reduce knockback
          if (knockback != 0) {
            double adjustedStrength = strength * knockback;
            // 1.21 parity: blast protection's explosion knockback dampening (formerly
            // ProtectionEnchantment.getExplosionKnockbackAfterDampener) is now data-driven via the
            // generic.explosion_knockback_resistance attribute granted by the enchantment, matching vanilla Explosion.
            if (entity instanceof LivingEntity living) {
              adjustedStrength *= 1 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            }
            Vec3 velocity = dir.scale(adjustedStrength / length);
            entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));
            if (entity instanceof Player player) {
              if (!player.isCreative() || !player.getAbilities().flying) {
                getHitPlayers().put(player, velocity);
              }
            }
          }
        }
      }
    }
  }

  /** Breaks the blocks in {@link #toBlow}, dropping their loot. Same as the private vanilla logic. */
  protected void interactWithBlocks() {
    List<StackCollector> stacks = new ArrayList<>();
    List<BlockPos> targets = new ArrayList<>(toBlow);
    net.minecraft.util.Util.shuffle(targets, this.level.getRandom());
    for (BlockPos pos : targets) {
      this.level.getBlockState(pos).onExplosionHit(this.level, pos, this, (stack, position) -> addOrAppendStack(stacks, stack, position));
    }
    for (StackCollector stack : stacks) {
      Block.popResource(this.level, stack.pos, stack.stack);
    }
  }

  /** Places fire on the blocks in {@link #toBlow}. Same as the private vanilla logic. */
  protected void createFire() {
    for (BlockPos pos : toBlow) {
      if (this.level.getRandom().nextInt(3) == 0 && this.level.getBlockState(pos).isAir() && this.level.getBlockState(pos.below()).isSolidRender()) {
        this.level.setBlockAndUpdate(pos, BaseFireBlock.getState(this.level, pos));
      }
    }
  }

  /** Merges the stack into an existing drop if possible */
  private static void addOrAppendStack(List<StackCollector> stacks, ItemStack stack, BlockPos pos) {
    for (StackCollector collector : stacks) {
      collector.tryMerge(stack);
      if (stack.isEmpty()) {
        return;
      }
    }
    stacks.add(new StackCollector(pos, stack));
  }

  /** Runs the logic on the server, syncing to the client. Based on {@link ServerLevel#explode(Entity, DamageSource, ExplosionDamageCalculator, double, double, double, float, boolean, Level.ExplosionInteraction)}*/
  public void handleServer() {
    // based on ServerLevel#explode
    if (!EventHooks.onExplosionStart(level, this)) {
      syncToClient(explode());
    }
  }

  /**
   * Runs the explosion logic. Explosions are server only since 1.21.2, so the client effects come from the explosion packet.
   * @param level           Level running the explosion, does nothing on the client
   * @param spawnParticles  If true, sends the explosion to the client for particles and sound
   */
  public void doDualSide(Level level, boolean spawnParticles) {
    if (!level.isClientSide() && !EventHooks.onExplosionStart(this.level, this)) {
      int blocks = explode();
      if (spawnParticles) {
        syncToClient(blocks);
      }
    }
  }

  /**
   * Syncs this explosion to the client
   * @param blockCount  Number of blocks hit, used by the client to pick the explosion particle
   */
  public void syncToClient(int blockCount) {
    Vec3 position = center();
    // matches the vanilla small and large explosion particles and the silent source check from ServerLevel#explode
    boolean playSound = source == null || !source.isSilent();
    for (ServerPlayer player : level.players()) {
      if (player.distanceToSqr(position) < 4096.0D) {
        player.connection.send(new ClientboundExplodePacket(position, radius, blockCount, Optional.ofNullable(getHitPlayers().get(player)),
                                                            isSmall() ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE, WeightedList.of(), playSound));
      }
    }
  }

  /** @deprecated use {@link #syncToClient(int)} */
  @Deprecated
  public void syncToClient() {
    syncToClient(interactsWithBlocks() ? toBlow.size() : 0);
  }

  /** Item stack being combined for explosion drops */
  private static class StackCollector {
    private final BlockPos pos;
    private ItemStack stack;

    private StackCollector(BlockPos pos, ItemStack stack) {
      this.pos = pos;
      this.stack = stack;
    }

    public void tryMerge(ItemStack input) {
      if (ItemEntity.areMergable(this.stack, input)) {
        this.stack = ItemEntity.merge(this.stack, input, MAX_DROPS_PER_COMBINED_STACK);
      }
    }
  }
}
