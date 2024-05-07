package slimeknights.tconstruct.library.json.field;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;

/** Wrapper around a loadable making it optionally load from an object key. If used with a record loadable, will condition on the object key being present */
public record OptionallyNestedLoadable<T>(Loadable<T> loadable, String objectKey) implements Loadable<T> {
  @Override
  public T convert(JsonElement element, String key) {
    if (element.isJsonObject()) {
      // only call the nested if the object key is defined, useful for optioanlly nested objects
      JsonObject json = element.getAsJsonObject();
      if (json.has(objectKey)) {
        return loadable.convert(json.get(objectKey), objectKey);
      }
    }
    return loadable.convert(element, objectKey);
  }

  @Override
  public JsonElement serialize(T object) {
    // don't bother using the key when serializing, someone else will if needed
    return loadable.serialize(object);
  }

  @Override
  public T decode(FriendlyByteBuf buffer) {
    return loadable.decode(buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T value) {
    loadable.encode(buffer, value);
  }
}
