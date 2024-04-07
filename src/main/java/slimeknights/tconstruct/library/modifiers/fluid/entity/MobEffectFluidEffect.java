package slimeknights.tconstruct.library.modifiers.fluid.entity;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;
import slimeknights.tconstruct.library.modifiers.fluid.TimeAction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spilling effect to apply a potion effect
 * @param effect  Effect to apply
 * @param level   Potion level starting at 1. Fixed with respect to fluid amount.
 * @param time    Potion time in ticks, scales with fluid amount.
 */
public record MobEffectFluidEffect(MobEffect effect, int level, int time, TimeAction action, @Nullable List<Item> curativeItems) implements FluidEffect<FluidEffectContext.Entity> {
  public static final RecordLoadable<MobEffectFluidEffect> LOADER = RecordLoadable.create(
    Loadables.MOB_EFFECT.requiredField("effect", e -> e.effect),
    IntLoadable.FROM_ONE.defaultField("level", 1, true, e -> e.level),
    IntLoadable.FROM_ONE.requiredField("time", e -> e.time),
    TimeAction.LOADABLE.requiredField("action", e -> e.action),
    Loadables.ITEM.list(0).nullableField("curative_items", e -> e.curativeItems),
    MobEffectFluidEffect::new);

  /** Creates a new builder instance */
  public static Builder set(MobEffect effect) {
    return new Builder(effect, TimeAction.SET);
  }

  /** Creates a new builder instance */
  public static Builder add(MobEffect effect) {
    return new Builder(effect, TimeAction.ADD);
  }

  @Override
  public RecordLoadable<MobEffectFluidEffect> getLoader() {
    return LOADER;
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel scale, Entity context, FluidAction action) {
    // first, need a target
    LivingEntity target = context.getLivingTarget();
    if (target != null) {
      float used;
      int time;
      // add and set both have distinct behavior under an existing effect, same otherwise
      MobEffectInstance existingInstance = target.getEffect(effect);
      if (existingInstance != null && existingInstance.getAmplifier() >= this.level) {
        if (this.action == TimeAction.ADD) {
          int extraTime = (int)(this.time * scale.value());
          if (extraTime <= 0) {
            return 0;
          }
          // add simply sums existing time with the desired time. If the level is higher than ours, it produces a hidden effect
          time = existingInstance.getDuration() + extraTime;
          used = scale.value();
        } else {
          // set can produce time up to the maximum allowed by scale,
          float existing = existingInstance.getDuration() / (float)this.time;
          float effective = scale.effective(existing);
          // no change? means we skip applying entirely
          if (effective < existing) {
            return 0;
          }
          used = effective - existing;
          time = (int)(this.time * effective);
        }
      } else {
        // if no existing, time and set behave the same way, use maximum amount to compute time
        time = (int)(this.time * scale.value());
        used = scale.value();
      }
      // if we got time, add the effect
      if (time > 0) {
        MobEffectInstance newInstance = new MobEffectInstance(effect, time, this.level);
        if (curativeItems != null) {
          newInstance.setCurativeItems(curativeItems.stream().map(ItemStack::new).collect(Collectors.toList()));
        }
        // if simulating, just ask if it's applicable. Won't handle event canceling, but thats acceptable.
        if (action.simulate()) {
          return target.canBeAffected(newInstance) ? used : 0;
        }
        return target.addEffect(newInstance) ? used : 0;
      }
    }
    return 0;
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  public static class Builder {
    private final MobEffect effect;
    @Setter
    private int level = 1;
    @Setter
    private int time;
    private final TimeAction action;
    private List<Item> curativeItems;

    /** Makes the added effect incurable */
    public Builder noCure() {
      curativeItems = List.of();
      return this;
    }

    /** Adds an item to the cures, overrides default cures */
    public Builder curativeItem(Item item) {
      if (curativeItems == null) {
        curativeItems = new ArrayList<>();
      }
      curativeItems.add(item);
      return this;
    }

    /** Sets the time in seconds to the given flat value */
    public Builder timeSeconds(int time) {
      return time(time * 20);
    }

    /** Builds the final effect */
    public MobEffectFluidEffect build() {
      if (level <= 0) throw new IllegalStateException("Level must be positive");
      if (time <= 0) throw new IllegalStateException("Time must be positive");
      return new MobEffectFluidEffect(effect, level, time, action, curativeItems);
    }
  }
}
