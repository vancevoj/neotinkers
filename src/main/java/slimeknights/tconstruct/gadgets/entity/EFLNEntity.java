package slimeknights.tconstruct.gadgets.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import slimeknights.tconstruct.gadgets.TinkerGadgets;

/** @deprecated use {@link slimeknights.tconstruct.tools.entity.ThrownShuriken} */
@Deprecated
public class EFLNEntity extends ThrowableItemProjectile implements IEntityWithComplexSpawn {
  public EFLNEntity(EntityType<? extends EFLNEntity> type, Level level) {
    super(type, level);
  }

  public EFLNEntity(Level level, LivingEntity thrower, ItemStack stack) {
    super(TinkerGadgets.eflnEntity.get(), thrower, level, stack);
  }

  public EFLNEntity(Level level, LivingEntity thrower) {
    this(level, thrower, new ItemStack(TinkerGadgets.efln.get()));
  }

  public EFLNEntity(Level worldIn, double x, double y, double z) {
    super(TinkerGadgets.eflnEntity.get(), x, y, z, worldIn, new ItemStack(TinkerGadgets.efln.get()));
  }

  @Override
  protected Item getDefaultItem() {
    return TinkerGadgets.efln.get();
  }

  @Override
  protected void onHit(HitResult result) {
    if (level() instanceof ServerLevel level) {
      new EFLNExplosion(level, position(), 4f, this, 8f, null, 1, false, BlockInteraction.DESTROY).handleServer();
      this.discard();
    }
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
