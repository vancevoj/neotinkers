package slimeknights.tconstruct.fluids.item;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.tconstruct.fluids.fluids.PotionFluidType;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

/** Implements filling a bucket with an NBT fluid */
public class PotionBucketItem extends PotionItem {
  /** Use duration in ticks, 3x the duration of potion bottles */
  private static final int USE_DURATION = 96;

  private final Supplier<? extends Fluid> supplier;
  public PotionBucketItem(Supplier<? extends Fluid> supplier, Properties builder) {
    // drinking (use, animation, sounds) comes from the consumable component since 1.21.2, the effects are applied in finishUsingItem
    super(builder.component(DataComponents.CONSUMABLE, Consumables.defaultDrink().consumeSeconds(USE_DURATION / 20f).build()));
    this.supplier = supplier;
  }

  public Fluid getFluid() {
    return supplier.get();
  }

  /** Gets the potion contents stored on the stack */
  private static PotionContents getContents(ItemStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null ? contents : PotionContents.EMPTY;
  }

  @Override
  public Component getName(ItemStack stack) {
    PotionContents contents = getContents(stack);
    String bucketKey = PotionFluidType.getPotionKey(contents, getDescriptionId() + ".effect.");
    if (Util.canTranslate(bucketKey)) {
      return Component.translatable(bucketKey);
    }
    // default to filling with the contents
    return Component.translatable(getDescriptionId() + ".contents", Component.translatable(PotionFluidType.getPotionKey(contents, "item.minecraft.potion.effect.")));
  }

  @Override
  public ItemStack getDefaultInstance() {
    return PotionContents.createItemStack(this, Potions.AWKWARD);
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    // not calling super, as the consumable would apply the potion effects at normal strength; the final drink sound normally comes from the consumable
    Consumable consumable = stack.get(DataComponents.CONSUMABLE);
    if (consumable != null) {
      consumable.emitParticlesAndSounds(living.getRandom(), living, stack, 16);
    }
    Player player = living instanceof Player p ? p : null;
    if (player instanceof ServerPlayer serverPlayer) {
      CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
    }

    // effects are 2.5x duration
    if (level instanceof ServerLevel serverLevel) {
      for (MobEffectInstance effect : getContents(stack).getAllEffects()) {
        Holder<MobEffect> mobEffect = effect.getEffect();
        if (mobEffect.value().isInstantaneous()) {
          mobEffect.value().applyInstantaneousEffect(serverLevel, player, player, living, effect.getAmplifier(), 2.5D);
        } else {
          living.addEffect(effect.withScaledDuration(2.5f));
        }
      }
    }

    if (player != null) {
      player.awardStat(Stats.ITEM_USED.get(this));
      if (!player.getAbilities().instabuild) {
        stack.shrink(1);
      }
    }

    if (player == null || !player.getAbilities().instabuild) {
      if (stack.isEmpty()) {
        return new ItemStack(Items.BUCKET);
      }
      if (player != null) {
        player.getInventory().add(new ItemStack(Items.BUCKET));
      }
    }
    living.gameEvent(GameEvent.DRINK);
    return stack;
  }

  @Override
  public int getUseDuration(ItemStack pStack, LivingEntity living) {
    return USE_DURATION;
  }

  /** Builds the fluid handler for the given item access, for registration in {@code RegisterCapabilitiesEvent}. */
  public ResourceHandler<FluidResource> getFluidHandler(ItemAccess access) {
    return new PotionBucketWrapper(access);
  }

  /**
   * Fluid handler for a potion bucket, holding a full bucket of the potion fluid with the potion contents of the stack.
   * Draining the bucket turns it into an empty bucket; filling an empty bucket with the potion fluid is handled by the empty bucket through {@link PotionFluidType#getBucket(FluidStack)}.
   */
  public static class PotionBucketWrapper extends ItemAccessResourceHandler<FluidResource> {
    public PotionBucketWrapper(ItemAccess access) {
      super(access, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
      if (accessResource.getItem() instanceof PotionBucketItem bucket) {
        PotionContents contents = accessResource.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
          return FluidResource.of(bucket.getFluid(), DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, contents).build());
        }
        return FluidResource.of(bucket.getFluid());
      }
      return FluidResource.EMPTY;
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
      return getResourceFrom(accessResource, index).isEmpty() ? 0 : FluidType.BUCKET_VOLUME;
    }

    @Nullable
    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
      if (newAmount == 0) {
        return ItemResource.of(Items.BUCKET);
      }
      // only full buckets can exist
      if (newAmount != FluidType.BUCKET_VOLUME) {
        return ItemResource.EMPTY;
      }
      FluidStack stack = newResource.toStack(newAmount);
      return ItemResource.of(stack.getFluidType().getBucket(stack));
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
      Objects.checkIndex(index, size());
      return FluidType.BUCKET_VOLUME;
    }
  }
}
