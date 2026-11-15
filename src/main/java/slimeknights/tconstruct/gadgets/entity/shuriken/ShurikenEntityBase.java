package slimeknights.tconstruct.gadgets.entity.shuriken;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

/** @deprecated use {@link ThrownShuriken} */
@Deprecated
public abstract class ShurikenEntityBase extends ThrowableItemProjectile implements IEntityWithComplexSpawn, ToolProjectile {

  public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, Level worldIn) {
    super(type, worldIn);
  }

  public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, double x, double y, double z, Level worldIn, ItemStack stack) {
    super(type, x, y, z, worldIn, stack);
  }

  public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, LivingEntity livingEntityIn, Level worldIn, ItemStack stack) {
    super(type, livingEntityIn, worldIn, stack);
  }

    /**
   * Get damage dealt by Shuriken
   * Should be <= 20.0F
   * @return float damage
   */
  public abstract float getDamage();

  /**
   * Get knockback dealt by Shuriken
   * Should be <= 1.0F, Minecraft
   * typically uses values from 0.2F-0.6F
   * @return float knockback
   */
  public abstract float getKnockback();

  @Override
  protected void onHit(HitResult result) {
    super.onHit(result);

    Level level = level();
    if (!level.isClientSide()) {
      level.broadcastEntityEvent(this, (byte) 3); // TODO: find the proper constant for this event ID
      this.discard();
    }
  }

  @Override
  protected void onHitBlock(BlockHitResult result) {
    super.onHitBlock(result);

    if (level() instanceof ServerLevel server) {
      this.spawnAtLocation(server, getDefaultItem());
    }
  }

  @Override
  protected void onHitEntity(EntityHitResult result) {
    Entity entity = result.getEntity();
    if (level() instanceof ServerLevel server) {
      DamageSource source = damageSources().thrown(this, this.getOwner());
      entity.hurtServer(server, source, this.getDamage());

      if (entity instanceof LivingEntity living) {
        Vec3 motion = this.getDeltaMovement().normalize();
        living.knockback(this.getKnockback(), -motion.x, -motion.z, source, this.getDamage());
      }
    }
  }

  @Override
  public ItemStack getDisplayTool() {
    return getItem();
  }

  @Override
  public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.getItem());
  }

  @Override
  public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
    this.setItem(ItemStack.OPTIONAL_STREAM_CODEC.decode(additionalData));
  }
}
