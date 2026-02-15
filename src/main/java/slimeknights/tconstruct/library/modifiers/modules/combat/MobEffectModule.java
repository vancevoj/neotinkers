package slimeknights.tconstruct.library.modifiers.modules.combat;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.LegacyLoadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.json.RandomLevelingValue;
import slimeknights.tconstruct.library.json.predicate.TinkerPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MonsterMeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.tools.modules.armor.CounterModule;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNullElse;
import static slimeknights.tconstruct.TConstruct.RANDOM;

/** Module that applies a mob effect on various causes. */
public interface MobEffectModule extends ModifierModule, ConditionalModule<IToolStackView> {
  /** Shared effect field instance */
  RecordField<ModifierMobEffect, MobEffectModule> EFFECT_FIELD = ModifierMobEffect.LOADER.directField(MobEffectModule::effect);
  /** Shared chance field */
  RecordField<LevelingValue, MobEffectModule> CHANCE_FIELD = LevelingValue.LOADABLE.defaultField("chance", LevelingValue.ONE, MobEffectModule::chance);

  /** @deprecated use {@link Weapon#LOADER} or {@link ArmorCounter#LOADER}. */
  @Deprecated
  RecordLoadable<Legacy> LOADER = LegacyLoadable.message(RecordLoadable.create(
    EFFECT_FIELD, WeaponCommon.BEFORE_MELEE_FIELD,
    LevelingValue.LOADABLE.defaultField("chance", LevelingValue.eachLevel(0.25f), MobEffectModule::chance),
    IntLoadable.FROM_ZERO.defaultField("counter_durability_usage", 1, CounterCommon::durabilityUsage),
    ModifierCondition.TOOL_FIELD,
    Legacy::new), "Using deprecated modifier module 'tconstruct:mob_effect'. Use one or both of 'tconstruct:weapon_mob_effect' or 'tconstruct:counter_mob_effect'");

  /** Gets the effect for this module */
  ModifierMobEffect effect();

  /** Chance of the effect applying. */
  default LevelingValue chance() {
    return LevelingValue.ONE;
  }

  /** Checks if the modifier passes the chance to apply. */
  default boolean checkChance(float level) {
    float chance = this.chance().compute(level);
    return chance > 0 && (chance >= 1 || RANDOM.nextFloat() < chance);
  }

  /** Checks if the modifier passes the chance to apply. */
  default boolean checkChance(ModifierEntry entry) {
    return checkChance(entry.getEffectiveLevel());
  }

  @Override
  RecordLoadable<? extends MobEffectModule> getLoader();


  /** Creates a builder instance */
  static MobEffectModule.Builder builder(MobEffect effect) {
    return new Builder(effect);
  }

  /** Creates a builder instance */
  static MobEffectModule.Builder builder(Supplier<? extends MobEffect> effect) {
    return new Builder(effect.get());
  }

  /** Builder for this modifier in datagen */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  @Setter
  class Builder extends ModuleBuilder.Stack<Builder> {
    // general fields
    /** Effect to apply. */
    private final MobEffect effect;
    /** Entity getting the effect. */
    private IJsonPredicate<LivingEntity> target = LivingEntityPredicate.ANY;
    /** Entity using the weapon. Unused for non-combat effects. */
    private IJsonPredicate<LivingEntity> holder = LivingEntityPredicate.ANY;
    /** Effect time in ticks. */
    private RandomLevelingValue level = RandomLevelingValue.flat(1);
    /** Effect level, starting from 1. */
    private RandomLevelingValue time = RandomLevelingValue.flat(0);
    /** Chance of applying the effect. */
    private LevelingValue chance = null;

    // weapon
    /** Applies the effect before melee hit instead of after. */
    private boolean applyBeforeMelee = false;

    // counter
    /** Amount of durability spent applying this modifier to counter-attacks. TODO 1.21: rename to {@code durabilityUsage} */
    private int counterDurabilityUsage = 1;


    /** Builds the effect */
    private ModifierMobEffect buildEffect() {
      return new ModifierMobEffect(effect, level, time, target);
    }

    /** Builds the finished modifier */
    public Weapon buildWeapon() {
      return new Weapon(buildEffect(), requireNonNullElse(chance, LevelingValue.ONE), holder, applyBeforeMelee, condition);
    }

    /** Builds the finished modifier */
    public ArmorCounter buildCounter() {
      return new ArmorCounter(buildEffect(), requireNonNullElse(chance, LevelingValue.eachLevel(0.15f)), holder, counterDurabilityUsage, condition);
    }

    /** Builds the finished modifier */
    @Deprecated(forRemoval = true)
    public Legacy build() {
      return new Legacy(buildEffect(), applyBeforeMelee, requireNonNullElse(chance, LevelingValue.eachLevel(0.25f)), counterDurabilityUsage, condition);
    }
  }

  /** Represents a mob effect applied via a modifier. Meant to be nested inside a modifier module. */
  record ModifierMobEffect(MobEffect effect, RandomLevelingValue level, RandomLevelingValue time, IJsonPredicate<LivingEntity> target) {
    public static final RecordLoadable<ModifierMobEffect> LOADER = RecordLoadable.create(
      Loadables.MOB_EFFECT.requiredField("effect", ModifierMobEffect::effect),
      RandomLevelingValue.LOADABLE.requiredField("level", ModifierMobEffect::level),
      RandomLevelingValue.LOADABLE.requiredField("time", ModifierMobEffect::time),
      LivingEntityPredicate.LOADER.defaultField("target", ModifierMobEffect::target),
      ModifierMobEffect::new);

    /** Applies the effect for the given level */
    public void applyEffect(@Nullable LivingEntity target, float scaledLevel, @Nullable Entity cause) {
      if (target == null || !this.target.matches(target)) {
        return;
      }
      int level = Math.round(this.level.computeValue(scaledLevel)) - 1;
      if (level < 0) {
        return;
      }
      float duration = this.time.computeValue(scaledLevel);
      if (duration > 0) {
        target.addEffect(new MobEffectInstance(effect, (int)duration, level), cause);
      }
    }

    /** Applies the effect for the given modifier entry */
    public void applyEffect(@Nullable LivingEntity target, ModifierEntry entry, @Nullable Entity cause) {
      applyEffect(target, entry.getEffectiveLevel(), cause);
    }
  }


  /* Implementations */

  /** Common field between modules which distinguish holder from target */
  interface Combat extends MobEffectModule {
    RecordField<IJsonPredicate<LivingEntity>,Combat> HOLDER_FIELD = LivingEntityPredicate.LOADER.defaultField("holder", Combat::holder);

    /** Predicate on the entity holding this weapon or wearing this armor. */
    default IJsonPredicate<LivingEntity> holder() {
      return LivingEntityPredicate.ANY;
    }
  }

  /** Common logic between {@link Weapon} and {@link Legacy}. TODO 1.21: merge into {@link Weapon} */
  @Internal
  interface WeaponCommon extends Combat, MeleeHitModifierHook, MonsterMeleeHitModifierHook, ProjectileHitModifierHook {
    RecordField<Boolean,WeaponCommon> BEFORE_MELEE_FIELD = BooleanLoadable.INSTANCE.defaultField("apply_before_melee", false, false, WeaponCommon::applyBeforeMelee);

    /** If true, this applies before melee hits instead of after. */
    boolean applyBeforeMelee();

    @Override
    default void onMonsterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
      if (condition().matches(tool, modifier) && checkChance(modifier)) {
        LivingEntity attacker = context.getAttacker();
        if (holder().matches(attacker)) {
          effect().applyEffect(context.getLivingTarget(), modifier, attacker);
        }
      }
    }

    @Override
    default float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
      if (applyBeforeMelee()) {
        onMonsterMeleeHit(tool, modifier, context, damage);
      }
      return knockback;
    }

    @Override
    default void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
      if (!applyBeforeMelee()) {
        onMonsterMeleeHit(tool, modifier, context, damageDealt);
      }
    }

    @Override
    default boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
      if (condition().modifierLevel().test(modifier.getLevel()) && TinkerPredicate.matches(holder(), attacker) && checkChance(modifier)) {
        effect().applyEffect(target, modifier, projectile.getEffectSource());
      }
      return false;
    }
  }

  /** Implementation for melee and ranged weapons, applying the effect to the target. */
  @Internal
  record Weapon(ModifierMobEffect effect, LevelingValue chance, IJsonPredicate<LivingEntity> holder, boolean applyBeforeMelee, ModifierCondition<IToolStackView> condition) implements WeaponCommon {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<Weapon>defaultHooks(ModifierHooks.MELEE_HIT, ModifierHooks.MONSTER_MELEE_HIT, ModifierHooks.PROJECTILE_HIT);
    /** Loader for a weapon effect. */
    public static final RecordLoadable<Weapon> LOADER = RecordLoadable.create(EFFECT_FIELD, CHANCE_FIELD, HOLDER_FIELD, BEFORE_MELEE_FIELD, ModifierCondition.TOOL_FIELD, Weapon::new);

    /** @apiNote use {@link Builder#buildWeapon()} ()} */
    @Internal
    public Weapon {}

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
      return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<? extends Weapon> getLoader() {
      return LOADER;
    }
  }

  /** Common logic between {@link ArmorCounter} and {@link Legacy}. TODO 1.21: merge into {@link ArmorCounter}. */
  @Internal
  interface CounterCommon extends Combat, OnAttackedModifierHook {
    /** Reimplementation of chance field to change the default */
    RecordField<LevelingValue, CounterCommon> CHANCE_FIELD = LevelingValue.LOADABLE.defaultField("chance", LevelingValue.eachLevel(0.15f), false, CounterCommon::chance);

    /** Gets the amount of durability consumed by counter-attacks. */
    int durabilityUsage();

    @Override
    default void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
      Entity attacker = source.getEntity();
      LivingEntity defender = context.getEntity();
      if (isDirectDamage && condition().matches(tool, modifier) && holder().matches(defender) && attacker instanceof LivingEntity living) {
        float scaledLevel = CounterModule.getLevel(tool, modifier, slotType, defender);
        if (checkChance(scaledLevel)) {
          effect().applyEffect(living, scaledLevel, defender);
          int durabilityUsage = this.durabilityUsage();
          if (durabilityUsage > 0) {
            ToolDamageUtil.damageAnimated(tool, durabilityUsage, defender, slotType, modifier.getId());
          }
        }
      }
    }
  }

  /** Implementation for counter-attacks from armor */
  record ArmorCounter(ModifierMobEffect effect, LevelingValue chance, IJsonPredicate<LivingEntity> holder, int durabilityUsage, ModifierCondition<IToolStackView> condition) implements CounterCommon {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ArmorCounter>defaultHooks(ModifierHooks.ON_ATTACKED);
    public static final RecordLoadable<ArmorCounter> LOADER = RecordLoadable.create(
      EFFECT_FIELD, CHANCE_FIELD, HOLDER_FIELD,
      IntLoadable.FROM_ZERO.defaultField("durability_usage", 1, ArmorCounter::durabilityUsage),
      ModifierCondition.TOOL_FIELD, ArmorCounter::new);

    /** @apiNote use {@link Builder#buildCounter()} */
    @Internal
    public ArmorCounter {}

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
      return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<ArmorCounter> getLoader() {
      return LOADER;
    }
  }

  /** Legacy implementation that does weapon, armor, and alike. Too many things happening. */
  @Deprecated
  record Legacy(ModifierMobEffect effect, boolean applyBeforeMelee, LevelingValue chance, int durabilityUsage, ModifierCondition<IToolStackView> condition) implements WeaponCommon, CounterCommon {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<Legacy>defaultHooks(ModifierHooks.ON_ATTACKED, ModifierHooks.MELEE_HIT, ModifierHooks.MONSTER_MELEE_HIT, ModifierHooks.PROJECTILE_HIT);

    /** @apiNote use {@link Builder#build()} */
    @Internal
    public Legacy {}

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
      return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<Legacy> getLoader() {
      return MobEffectModule.LOADER;
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
      // legacy implementation conditioned on only being armor. For the new module we let you set the condition for that.
      if (tool.hasTag(TinkerTags.Items.ARMOR)) {
        CounterCommon.super.onAttacked(tool, modifier, context, slotType, source, amount, isDirectDamage);
      }
    }
  }
}
