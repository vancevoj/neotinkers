package slimeknights.tconstruct.library.modifiers.modules.capacity;

import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/** Helper for modules that provide capacity to be able to specify a target */
public interface CapacitySourceModule {
  /** Owner field instance */
  LoadableField<ModifierId,CapacitySourceModule> OWNER_FIELD = ModifierId.PARSER.nullableField("owner", CapacitySourceModule::owner);

  /** Bar owner. If null, uses the modifier itself */
  @Nullable
  ModifierId owner();

  /** Gets the bar for this module */
  default ModifierEntry barModifier(IToolStackView tool, ModifierEntry entry) {
    ModifierId owner = owner();
    if (owner != null) {
      return tool.getModifier(owner);
    }
    return entry;
  }
}
