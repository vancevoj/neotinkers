package slimeknights.tconstruct.library.json.predicate.material;

import net.minecraftforge.common.crafting.conditions.ICondition;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.MergingRegistryField;
import slimeknights.mantle.util.DataLoadedConditionContext;
import slimeknights.tconstruct.library.json.ConditionLoadable;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

/**
 * Material predicate that allows all materials if the condition is true. Intended to combine with and/or/not for desired logic.
 * @param condition  Condition that causes all materials to pass.
 */
public record ConditionMaterialPredicate(ICondition condition) implements MaterialPredicate {
  public static final RecordLoadable<ConditionMaterialPredicate> LOADER = RecordLoadable.create(new MergingRegistryField<>(ConditionLoadable.INSTANCE, "condition_type", ConditionMaterialPredicate::condition), ConditionMaterialPredicate::new);

  @Override
  public boolean matches(MaterialVariantId variant) {
    // all materials match if the condition matches
    return condition.test(DataLoadedConditionContext.INSTANCE);
  }

  @Override
  public RecordLoadable<? extends MaterialPredicate> getLoader() {
    return LOADER;
  }
}
