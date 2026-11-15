package slimeknights.tconstruct.library.tools;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerTools;

import javax.annotation.Nullable;

/** Item entity that will never die */
public class IndestructibleItemEntity extends ItemEntity {
  /** Modifier key to make a tool spawn an indestructable entity */
  public static final Identifier INDESTRUCTIBLE_ENTITY = TConstruct.getResource("indestructible");

  public IndestructibleItemEntity(EntityType<? extends IndestructibleItemEntity> entityType, Level world) {
    super(entityType, world);
    // using setUnlimitedLifetime() makes the item no longer spin, dumb design
    // since age is a short, this value should never be reachable so the item will never despawn
    this.lifespan = Integer.MAX_VALUE;
  }

  public IndestructibleItemEntity(Level worldIn, double x, double y, double z, ItemStack stack) {
    this(TinkerTools.indestructibleItem.get(), worldIn);
    this.setPos(x, y, z);
    this.setYRot(this.random.nextFloat() * 360.0F);
    this.setDeltaMovement(this.random.nextDouble() * 0.2D - 0.1D, 0.2D, this.random.nextDouble() * 0.2D - 0.1D);
    this.setItem(stack);
  }

  // 1.21: getAddEntityPacket now takes a ServerEntity and item entities sync their stack via tracked data, so the
  // vanilla ItemEntity implementation is sufficient; the old NetworkHooks.getEntitySpawningPacket helper is gone.

  /** Copies the pickup delay from another entity */
  public void setPickupDelayFrom(Entity reference) {
    if (reference instanceof ItemEntity itemEntity) {
      this.setPickUpDelay(itemEntity.pickupDelay);
    }
    setDeltaMovement(reference.getDeltaMovement());
  }

  @Override
  public boolean fireImmune() {
    return true;
  }

  /**
   * Since 1.21.2 the damage entry point {@link ItemEntity#hurtServer} is final and the overridable {@code isInvulnerableTo} is gone.
   * Being permanently invulnerable blocks all damage besides out of world in {@link #isInvulnerableToBase(DamageSource)},
   * except damage from creative players, which {@link #canBeHurtBy(ItemStack, DamageSource)} blocks through the item.
   */
  @Override
  public boolean isPermanentlyInvulnerable() {
    return true;
  }

  /** Checks if the given stack has a custom entity */
  public static boolean hasCustomEntity(ItemStack stack) {
    return ModifierUtil.checkVolatileFlag(stack, INDESTRUCTIBLE_ENTITY);
  }

  /**
   * Implementation of {@link net.neoforged.neoforge.common.extensions.IItemExtension#canBeHurtBy(ItemStack, DamageSource)} for modifiable items.
   * Prevents any damage besides out of world for stacks that spawn an indestructible entity, matching the old {@code isInvulnerableTo} on this entity.
   * @param stack   Stack in the item entity
   * @param source  Damage source
   * @return  True if the item entity can be damaged
   */
  public static boolean canBeHurtBy(ItemStack stack, DamageSource source) {
    return !hasCustomEntity(stack) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
  }

  /**
   * Creates an indestructible item entity from the given item stack (if needed). Intended to be called in {@link net.neoforged.neoforge.common.extensions.IItemExtension#createEntity(Level, Entity, ItemStack)}
   * @param world     World instance
   * @param original  Original entity
   * @param stack     Stack to drop
   * @return  indestructible entity, or null if the stack is not marked indestructible
   */
  @Nullable
  public static Entity createFrom(Level world, Entity original, ItemStack stack) {
    if (ModifierUtil.checkVolatileFlag(stack, INDESTRUCTIBLE_ENTITY)) {
      IndestructibleItemEntity entity = new IndestructibleItemEntity(world, original.getX(), original.getY(), original.getZ(), stack);
      entity.setPickupDelayFrom(original);
      return entity;
    }
    return null;
  }
}
