package slimeknights.tconstruct.library.json;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Singleton loader for record loadables, will likely be moved to Mantle once {@link slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader} is removed.
 */
public record SingletonRecordLoadable<T>(T instance) implements RecordLoadable<T> {
  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    return instance;
  }

  @Override
  public void serialize(T object, JsonObject json) {}

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    return instance;
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T value) {}
}
