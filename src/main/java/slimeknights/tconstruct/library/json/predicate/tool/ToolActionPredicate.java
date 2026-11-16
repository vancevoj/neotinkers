package slimeknights.tconstruct.library.json.predicate.tool;

import net.neoforged.neoforge.common.ItemAbility;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** Predicate matching tools exposing a given tool action */
public record ToolActionPredicate(ItemAbility action) implements ToolStackPredicate {
  public static final RecordLoadable<ToolActionPredicate> LOADER = RecordLoadable.create(Loadables.ITEM_ABILITY.requiredField("action", ToolActionPredicate::action), ToolActionPredicate::new);

  @Override
  public boolean matches(IToolStackView tool) {
    return ModifierUtil.canPerformAction(tool, action);
  }

  @Override
  public RecordLoadable<ToolActionPredicate> getLoader() {
    return LOADER;
  }
}
