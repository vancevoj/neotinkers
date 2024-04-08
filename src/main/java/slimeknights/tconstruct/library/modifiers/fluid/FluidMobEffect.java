package slimeknights.tconstruct.library.modifiers.fluid;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.block.MobEffectCloudFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.MobEffectFluidEffect;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common logic for effects between {@link slimeknights.tconstruct.library.modifiers.fluid.entity.MobEffectFluidEffect} and {@link slimeknights.tconstruct.library.modifiers.fluid.block.MobEffectCloudFluidEffect}
 * @param effect  Effect to apply
 * @param level   Potion level starting at 1. Fixed with respect to fluid amount.
 * @param time    Potion time in ticks, scales with fluid amount.
 * @param curativeItems  Items allowed to cure the effect
 */
public record FluidMobEffect(MobEffect effect, int time, int level, @Nullable List<Item> curativeItems) {
  public static final RecordLoadable<FluidMobEffect> LOADABLE = RecordLoadable.create(
    Loadables.MOB_EFFECT.requiredField("effect", e -> e.effect),
    IntLoadable.FROM_ONE.requiredField("time", e -> e.time),
    IntLoadable.FROM_ONE.defaultField("level", 1, true, e -> e.level),
    Loadables.ITEM.list(0).nullableField("curative_items", e -> e.curativeItems),
    FluidMobEffect::new);

  /** Gets the amplifier for a mob effect */
  public int amplifier() {
    return level - 1;
  }

  /** Creates the final effect */
  public MobEffectInstance effectWithTime(int time) {
    MobEffectInstance instance = new MobEffectInstance(effect, time, this.level - 1);
    if (curativeItems != null) {
      instance.setCurativeItems(curativeItems.stream().map(ItemStack::new).collect(Collectors.toList()));
    }
    return instance;
  }

  /** Creates the final effect */
  public MobEffectInstance makeEffect(float scale) {
    return effectWithTime((int)(this.time * scale));
  }

  /** Creates a new builder instance */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final ImmutableList.Builder<FluidMobEffect> effects = ImmutableList.builder();

    private Builder() {}

    /** Adds an effect to the builder with the passed cures. If none are passed, effect will have no cure*/
    public Builder effectCure(MobEffect effect, int time, int level, Item... curativeItems) {
      effects.add(new FluidMobEffect(effect, time, level, List.of(curativeItems)));
      return this;
    }

    /** Adds an effect to the builder with default cures */
    public Builder effect(MobEffect effect, int time, int level) {
      effects.add(new FluidMobEffect(effect, time, level, null));
      return this;
    }

    /** Adds an effect to the builder */
    public Builder effect(MobEffect effect, int time) {
      return effect(effect, time, 1);
    }

    private List<FluidMobEffect> getEffects() {
      List<FluidMobEffect> effects = this.effects.build();
      if (effects.isEmpty()) {
        throw new IllegalStateException("Must have at least one effect");
      }
      return effects;
    }

    /** Builds the cloud effect for blocks */
    public MobEffectCloudFluidEffect buildCloud() {
      return new MobEffectCloudFluidEffect(getEffects());
    }

    public List<MobEffectFluidEffect> buildEntity(TimeAction action) {
      return getEffects().stream().map(effect -> new MobEffectFluidEffect(effect, action)).toList();
    }
  }
}
