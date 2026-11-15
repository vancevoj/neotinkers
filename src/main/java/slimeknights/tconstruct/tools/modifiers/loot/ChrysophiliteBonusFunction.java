package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.BinomialWithBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.Formula;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.OreDrops;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.UniformBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.shared.TinkerAttributes;

import java.util.Optional;
import java.util.Set;

/** Loot modifier to boost drops based on teh chrysophilite amount */
public class ChrysophiliteBonusFunction extends LootItemConditionalFunction {
  /** Codec for the {@link Formula} dispatch, reuses the package-private codec in {@link ApplyBonusCount} (widened via access transformer) */
  static final MapCodec<Formula> FORMULA_CODEC = ApplyBonusCount.FORMULA_CODEC;

  /** Codec for this function */
  public static final MapCodec<ChrysophiliteBonusFunction> CODEC = RecordCodecBuilder.mapCodec(inst -> commonFields(inst).and(inst.group(
    FORMULA_CODEC.forGetter(f -> f.formula),
    Codec.BOOL.optionalFieldOf("include_base", true).forGetter(f -> f.includeBase)
  )).apply(inst, ChrysophiliteBonusFunction::new));

  /** Formula to apply */
  private final Formula formula;
  /** If true, the includes the helmet in the level, if false level is just gold pieces */
  private final boolean includeBase;
  protected ChrysophiliteBonusFunction(Optional<Holder<LootItemCondition>> condition, Formula formula, boolean includeBase) {
    super(condition);
    this.formula = formula;
    this.includeBase = includeBase;
  }

  /** Creates a generic builder */
  public static Builder<?> builder(Formula formula, boolean includeBase) {
    return simpleBuilder(condition -> new ChrysophiliteBonusFunction(condition, formula, includeBase));
  }

  /** Creates a builder for the binomial with bonus formula */
  public static Builder<?> binomialWithBonusCount(float probability, int extra, boolean includeBase) {
    return builder(new BinomialWithBonusCount(extra, probability), includeBase);
  }

  /** Creates a builder for the ore drops formula */
  public static Builder<?> oreDrops(boolean includeBase) {
    return builder(OreDrops.INSTANCE, includeBase);
  }

  /** Creates a builder for the uniform bonus count */
  public static Builder<?> uniformBonusCount(int bonusMultiplier, boolean includeBase) {
    return builder(new UniformBonusCount(bonusMultiplier), includeBase);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    if (context.getOptional(LootContextParams.THIS_ENTITY) instanceof LivingEntity entity) {
      int level = (int) entity.getAttributeValue(TinkerAttributes.CHRYSOPHILITE);
      if (!includeBase) {
        level--;
      }
      if (level > 0) {
        stack.setCount(formula.calculateNewCount(context.getRandom(), stack.getCount(), level));
      }
    }
    return stack;
  }

  @Override
  public Set<ContextKey<?>> getReferencedContextParams() {
    return ImmutableSet.of(LootContextParams.THIS_ENTITY);
  }

  @Override
  public MapCodec<ChrysophiliteBonusFunction> codec() {
    return CODEC;
  }
}
