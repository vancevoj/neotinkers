package slimeknights.tconstruct.library.tools.capability;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Capability to allow an entity to store modifiers, used on projectiles fired from modifiable items.
 * <p>
 * In NeoForge 1.21 this is backed by a serialized {@link AttachmentType data attachment} replacing the old Forge capability.
 */
public class EntityModifierCapability {
  /** Default instance to use with orElse */
  public static final EntityModifiers EMPTY = new EntityModifiers() {
    @Override
    public ModifierNBT getModifiers() {
      return ModifierNBT.EMPTY;
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {}

    @Override
    public void addModifiers(ModifierNBT nbt) {}
  };

  private EntityModifierCapability() {}

  /* Static helpers */

  /** List of predicates to check if the entity supports this capability */
  private static final List<Predicate<Entity>> ENTITY_PREDICATES = new ArrayList<>();

  /** Deferred register for the attachment type. Must be registered on the mod bus during construction via {@link #register(IEventBus)}. */
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  /** Attachment holding the entity modifiers. Serialized so projectiles keep their modifiers across save/load. */
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<Storage>> ATTACHMENT =
    ATTACHMENTS.register("modifiers", () -> AttachmentType.serializable(Storage::new).build());

  /** Gets the capability for the entity or an empty instance if missing */
  public static EntityModifiers getCapability(Entity entity) {
    // only create the attachment for supported entities, returning the read-only empty instance otherwise to preserve old behavior
    if (entity.hasData(ATTACHMENT) || supportCapability(entity)) {
      return entity.getData(ATTACHMENT);
    }
    return EMPTY;
  }

  /** Gets the data or an empty instance if missing */
  public static ModifierNBT getOrEmpty(Entity entity) {
    Storage storage = entity.getExistingDataOrNull(ATTACHMENT);
    return storage != null ? storage.getModifiers() : ModifierNBT.EMPTY;
  }

  /** Checks if the given entity supports this capability */
  public static boolean supportCapability(Entity entity) {
    for (Predicate<Entity> entityPredicate : ENTITY_PREDICATES) {
      if (entityPredicate.test(entity)) {
        return true;
      }
    }
    return false;
  }

  /** Registers a predicate of entites that need this capability */
  public static void registerEntityPredicate(Predicate<Entity> predicate) {
    ENTITY_PREDICATES.add(predicate);
  }

  /** Registers the attachment type with the mod event bus. Call during mod construction. */
  public static void register(IEventBus bus) {
    ATTACHMENTS.register(bus);
  }

  /**
   * Restores modifiers saved by Neo Tinkers for 1.21.1, which stored this attachment as a bare modifier list. 26.3 attachments read through a
   * {@link ValueInput} child, which must be a compound, so such an attachment loads empty (see {@link Storage}). This reads the old list from the
   * entity data instead. Called after an entity finished loading, see {@link slimeknights.tconstruct.mixin.EntityMixin}.
   * @param entity  Entity that finished loading
   * @param input   Entity save data, the root holding the attachments
   */
  @Internal
  public static void readLegacyModifiers(Entity entity, ValueInput input) {
    // the attachment is only present if the entity saved one, so nearly every entity stops here
    Storage storage = entity.getExistingDataOrNull(ATTACHMENT);
    if (storage != null && storage.getModifiers().isEmpty()) {
      input.child(AttachmentHolder.ATTACHMENTS_NBT_KEY)
        .flatMap(attachments -> attachments.read(ATTACHMENT.getId().toString(), ExtraCodecs.NBT))
        .filter(tag -> tag instanceof ListTag)
        .ifPresent(tag -> storage.setModifiers(ModifierNBT.readFromNBT(tag)));
    }
  }

  /**
   * Mutable storage backing the attachment, serialized via the modifier NBT list.
   * Attachments now serialize through a {@link ValueOutput}, which cannot have a list as its root, so the list is stored under {@link #TAG_MODIFIERS}.
   * The 1.21.1 layout (the bare list) is read by {@link #readLegacyModifiers(Entity, ValueInput)}.
   */
  public static class Storage implements ValueIOSerializable, EntityModifiers {
    /** Key holding the modifier list in the attachment data */
    private static final String TAG_MODIFIERS = "modifiers";

    @Getter @Setter
    private ModifierNBT modifiers = ModifierNBT.EMPTY;

    @Override
    public void serialize(ValueOutput output) {
      output.store(TAG_MODIFIERS, ExtraCodecs.NBT, modifiers.serializeToNBT());
    }

    @Override
    public void deserialize(ValueInput input) {
      modifiers = input.read(TAG_MODIFIERS, ExtraCodecs.NBT).map(ModifierNBT::readFromNBT).orElse(ModifierNBT.EMPTY);
    }
  }

  /** Interface for callers to use */
  public interface EntityModifiers {
    /** Gets the stored modifiers */
    ModifierNBT getModifiers();

    /** Sets the stored modifiers */
    void setModifiers(ModifierNBT nbt);

    /** Adds additional modifiers to the stored modifiers */
    default void addModifiers(ModifierNBT nbt) {
      ModifierNBT existing = getModifiers();
      if (existing.isEmpty()) {
        setModifiers(nbt);
      } else {
        setModifiers(ModifierNBT.builder().add(existing).add(nbt).build());
      }
    }
  }
}
