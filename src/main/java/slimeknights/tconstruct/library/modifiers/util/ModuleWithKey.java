package slimeknights.tconstruct.library.modifiers.util;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import javax.annotation.Nullable;

/**
 * Shared boilerplate for a module with a nullable key that can alternatively be the modifier ID
 */
public interface ModuleWithKey {
  /** Field for building loadables */ // TODO 1.21: rename to key field
  LoadableField<Identifier,ModuleWithKey> FIELD = Loadables.RESOURCE_LOCATION.nullableField("key", ModuleWithKey::key);

  /** Gets the key for the module */
  default Identifier getKey(Modifier modifier) {
    Identifier key = key();
    if (key != null) {
      return key;
    }
    return modifier.getId();
  }

  /** Gets the key for the module */
  default Identifier getKey(ModifierEntry modifier) {
    Identifier key = key();
    if (key != null) {
      return key;
    }
    return modifier.getId();
  }

  /** Gets the key field from the record */
  @Nullable
  Identifier key();

  /**
   * Parses the key from JSON
   * @param json  Json object
   * @return  Key, or null if not present
   */
  @Nullable
  static Identifier parseKey(JsonObject json) {
    if (json.has("key")) {
      return JsonHelper.getResourceLocation(json, "key");
    }
    return null;
  }

  /** Reads the key from the network */
  @Nullable
  static Identifier fromNetwork(FriendlyByteBuf buffer) {
    if (buffer.readBoolean()) {
      return buffer.readIdentifier();
    }
    return null;
  }

  /** Writes the key to the network */
  static void toNetwork(@Nullable Identifier key, FriendlyByteBuf buffer) {
    if (key != null) {
      buffer.writeBoolean(true);
      buffer.writeIdentifier(key);
    } else {
      buffer.writeBoolean(false);
    }
  }
}
