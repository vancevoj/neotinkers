package slimeknights.tconstruct.library.modifiers.fluid.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.NamedComponentRegistry;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Effect that damages an entity
 * @param modifiers  Additional properties to set on the damage source
 * @param damage     Amount of damage to apply
 */
public record DamageFluidEffect(List<Consumer<DamageSource>> modifiers, float damage) implements FluidEffect<FluidEffectContext.Entity> {
  /** Registry of various damage sources */
  public static final NamedComponentRegistry<Consumer<DamageSource>> SOURCE_MODIFIERS = new NamedComponentRegistry<>("Unregistered damage source modifier");
  /** Loader for this effect */
  public static final RecordLoadable<DamageFluidEffect> LOADER = RecordLoadable.create(
    SOURCE_MODIFIERS.list(0).defaultField("modifier", List.of(), e -> e.modifiers),
    FloatLoadable.FROM_ZERO.requiredField("damage", e -> e.damage),
    DamageFluidEffect::new);

  @SafeVarargs
  public DamageFluidEffect(float damage, Consumer<DamageSource> ... modifiers) {
    this(List.of(modifiers), damage);
  }

  @Override
  public RecordLoadable<DamageFluidEffect> getLoader() {
    return LOADER;
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, Entity context, FluidAction action) {
    float value = level.value();
    if (action.simulate()) {
      return value;
    }
    DamageSource source = context.createDamageSource();
    for (Consumer<DamageSource> modifier : modifiers) {
      modifier.accept(source);
    }
    return ToolAttackUtil.attackEntitySecondary(source, this.damage * value, context.getTarget(), context.getLivingTarget(), true) ? value : 0;
  }


  /** Makes the source fire damage */
  public static final Consumer<DamageSource> FIRE = modifier("fire", DamageSource::setIsFire);
  /** Makes the source explosion damage */
  public static final Consumer<DamageSource> EXPLOSION = modifier("explosion", DamageSource::setExplosion);
  /** Makes the source magic damage */
  public static final Consumer<DamageSource> MAGIC = modifier("magic", DamageSource::setMagic);
  /** Makes the source fall damage */
  public static final Consumer<DamageSource> FALL = modifier("fall", DamageSource::setIsFall);
  /** Makes the source not make the target hostile */
  public static final Consumer<DamageSource> NO_AGGRO = modifier("no_aggro", DamageSource::setNoAggro);
  /** Makes the damage bypass basic armor protection */
  public static final Consumer<DamageSource> BYPASS_ARMOR = modifier("bypass_armor", DamageSource::bypassArmor);
  /** Makes the damage bypass enchantments like protection */
  public static final Consumer<DamageSource> BYPASS_ENCHANTMENTS = modifier("bypass_enchantments", DamageSource::bypassEnchantments);
  /** Makes the damage bypass potion effects and enchantments */
  public static final Consumer<DamageSource> BYPASS_MAGIC = modifier("bypass_magic", DamageSource::bypassMagic);

  /** Registers a modifier locally */
  private static Consumer<DamageSource> modifier(String name, Consumer<DamageSource> consumer) {
    SOURCE_MODIFIERS.register(TConstruct.getResource(name), consumer);
    return consumer;
  }
}
