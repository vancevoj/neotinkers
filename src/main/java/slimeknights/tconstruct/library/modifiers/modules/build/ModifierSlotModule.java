package slimeknights.tconstruct.library.modifiers.modules.build;

import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.context.ToolRebuildContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.List;

/**
 * Module that adds extra modifier slots to a tool.
 */
public record ModifierSlotModule(SlotType type, int count, ModifierCondition<IToolContext> condition) implements VolatileDataModifierHook, ModifierModule, ConditionalModule<IToolContext> {
  private static final List<ModifierHook<?>> DEFAULT_HOOKS = List.of(TinkerHooks.VOLATILE_DATA);
  public static final RecordLoadable<ModifierSlotModule> LOADER = RecordLoadable.create(
    SlotType.LOADABLE.requiredField("name", ModifierSlotModule::type),
    IntLoadable.ANY_SHORT.defaultField("count", 1, true, ModifierSlotModule::count),
    ModifierCondition.CONTEXT_FIELD,
    ModifierSlotModule::new);

  public ModifierSlotModule(SlotType type, int count) {
    this(type, count, ModifierCondition.ANY_CONTEXT);
  }

  public ModifierSlotModule(SlotType type) {
    this(type, 1);
  }

  @Override
  public Integer getPriority() {
    // show lower priority so they group together
    return 50;
  }

  @Override
  public void addVolatileData(ToolRebuildContext context, ModifierEntry modifier, ModDataNBT volatileData) {
    if (condition.matches(context, modifier)) {
      volatileData.addSlots(type, count * modifier.getLevel());
    }
  }

  @Override
  public RecordLoadable<ModifierSlotModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModifierHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }
}
