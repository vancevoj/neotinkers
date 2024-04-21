package slimeknights.tconstruct.library.modifiers.modules.behavior;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.AttributeModuleBuilder;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Module to add an attribute to a tool
 */
public record AttributeModule(String unique, Attribute attribute, Operation operation, LevelingValue amount, UUID[] slotUUIDs, ModifierCondition<IToolStackView> condition) implements AttributesModifierHook, ModifierModule, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<AttributeModule>defaultHooks(ModifierHooks.ATTRIBUTES);
  public static final RecordLoadable<AttributeModule> LOADER = RecordLoadable.create(
    StringLoadable.DEFAULT.requiredField("unique", AttributeModule::unique),
    Loadables.ATTRIBUTE.requiredField("attribute", AttributeModule::attribute),
    TinkerLoadables.OPERATION.requiredField("operation", AttributeModule::operation),
    LevelingValue.LOADABLE.directField(AttributeModule::amount),
    TinkerLoadables.EQUIPMENT_SLOT_SET.requiredField("slots", m -> uuidsToSlots(m.slotUUIDs)),
    ModifierCondition.TOOL_FIELD,
    (unique, attribute, operation, amount, slots, condition) -> new AttributeModule(unique, attribute, operation, amount, slotsToUUIDs(unique, slots), condition));

  /** Gets the UUID from a name */
  public static UUID getUUID(String name, EquipmentSlot slot) {
    return UUID.nameUUIDFromBytes((name + "." + slot.getName()).getBytes());
  }

  /** Converts a list of slots to an array of UUIDs at each index */
  public static UUID[] slotsToUUIDs(String name, Collection<EquipmentSlot> slots) {
    UUID[] slotUUIDs = new UUID[6];
    for (EquipmentSlot slot : slots) {
      slotUUIDs[slot.getFilterFlag()] = getUUID(name, slot);
    }
    return slotUUIDs;
  }

  /** Maps the UUID array to a set for serializing */
  public static Set<EquipmentSlot> uuidsToSlots(UUID[] uuids) {
    Set<EquipmentSlot> set = EnumSet.noneOf(EquipmentSlot.class);
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      if (uuids[slot.getFilterFlag()] != null) {
        set.add(slot);
      }
    }
    return set;
  }

  @Override
  public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute,AttributeModifier> consumer) {
    if (condition.matches(tool, modifier)) {
      UUID uuid = slotUUIDs[slot.getFilterFlag()];
      if (uuid != null) {
        consumer.accept(attribute, new AttributeModifier(uuid, unique + "." + slot.getName(), amount.compute(modifier.getEffectiveLevel()), operation));
      }
    }
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public RecordLoadable<AttributeModule> getLoader() {
    return LOADER;
  }


  /** Creates a new builder instance */
  public static Builder builder(Attribute attribute, Operation operation) {
    return new Builder(attribute, operation);
  }

  public static class Builder extends AttributeModuleBuilder<Builder,AttributeModule> {
    private EquipmentSlot[] slots = EquipmentSlot.values();

    protected Builder(Attribute attribute, Operation operation) {
      super(attribute, operation);
    }

    /** Adds the given slots to this builder */
    public Builder slots(EquipmentSlot... slots) {
      this.slots = slots;
      return this;
    }

    @Override
    public AttributeModule amount(float flat, float eachLevel) {
      if (unique == null) {
        throw new IllegalStateException("Must set unique for attributes");
      }
      return new AttributeModule(unique, attribute, operation, new LevelingValue(flat, eachLevel), slotsToUUIDs(unique, List.of(slots)), condition);
    }
  }
}
