package slimeknights.tconstruct.tools.modifiers.defense;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.data.ModifierMaxLevel;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.tools.modifiers.defense.BlastProtectionModifier.BlastData;

public class BlastProtectionModifier extends AbstractProtectionModifier<BlastData> {
  /** Entity data key for the data associated with this modifier */
  private static final ComputableDataKey<BlastData> BLAST_DATA = TConstruct.createKey("blast_protection", BlastData::new);
  public BlastProtectionModifier() {
    super(BLAST_DATA);
    MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ExplosionEvent.Detonate.class, BlastProtectionModifier::onExplosionDetonate);
    MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingTickEvent.class, BlastProtectionModifier::livingTick);
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(ProtectionModule.source(DamageSourcePredicate.CAN_PROTECT, DamageSourcePredicate.EXPLOSION).eachLevel(2.5f));
  }

  @Override
  protected void reset(BlastData data, EquipmentChangeContext context) {
    data.wasKnockback = false;
  }

  /** On explosion, checks if any blast protected entity is involved, if so marks them for knockback update next tick */
  private static void onExplosionDetonate(ExplosionEvent.Detonate event) {
    Explosion explosion = event.getExplosion();
    Vec3 center = explosion.getPosition();
    float diameter = explosion.radius * 2;
    // search the entities for someone protection by blast protection
    for (Entity entity : event.getAffectedEntities()) {
      if (!entity.ignoreExplosion()) {
        entity.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
          // if the entity has blast protection and the blast protection level is bigger than vanilla, time to process
          BlastData blastData = data.get(BLAST_DATA);
          if (blastData != null && blastData.getMax() > 0) {
            // explosion is valid as long as the entity's eye is not directly on the explosion
            double x = entity.getX() - center.x;
            double z = entity.getZ() - center.z;
            if (x != 0 || z != 0 || (entity.getEyeY() - center.y) != 0) {
              // we need two numbers to calculate the knockback: distance to explosion and block density
              double y = entity.getY() - center.y;
              double distance = Mth.sqrt((float)(x * x + y * y + z * z)) / diameter;
              if (distance <= 1) {
                blastData.wasKnockback = true;
              }
            }
          }
        });
      }
    }
  }

  /** If the entity is marked for knockback update, adjust velocity */
  private static void livingTick(LivingTickEvent event) {
    LivingEntity living = event.getEntity();
    if (!living.level.isClientSide && !living.isSpectator()) {
      living.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
        BlastData blastData = data.get(BLAST_DATA);
        if (blastData != null && blastData.wasKnockback) {
          blastData.wasKnockback = false;
          float max = blastData.getMax();
          if (max > 0) {
            // due to MC-198809, vanilla does not actually reduce the knockback except on levels higher than obtainable in survival (blast prot VII)
            // thus, we only care about our own level for reducing
            double scale = 1 - (blastData.getMax() * 0.15f);
            if (scale <= 0) {
              living.setDeltaMovement(Vec3.ZERO);
            } else {
              living.setDeltaMovement(living.getDeltaMovement().multiply(scale, scale, scale));
            }
            living.hurtMarked = true;
          }
        }
      });
    }
  }

  /** Data object for the modifier */
  protected static class BlastData extends ModifierMaxLevel {
    /** If true, the entity was knocked back and needs their velocity adjusted */
    boolean wasKnockback = false;
  }
}
