package slimeknights.tconstruct.library.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map.Entry;

/** Loadable for a {@link ICondition}. Syncs using {@link ExtraCodecs#JSON} and {@link FriendlyByteBuf#writeWithCodec(DynamicOps, Codec, Object)} */
public enum ConditionLoadable implements RecordLoadable<ICondition> {
  INSTANCE;

  @Override
  public ICondition deserialize(JsonObject json, TypedMap context) {
    return CraftingHelper.getCondition(json);
  }

  @Override
  public JsonObject serialize(ICondition condition) {
    return CraftingHelper.serialize(condition);
  }

  @Override
  public void serialize(ICondition object, JsonObject json) {
    JsonObject serialized = CraftingHelper.serialize(object);
    for (Entry<String, JsonElement> entry : serialized.entrySet()) {
      json.add(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public ICondition decode(FriendlyByteBuf buffer, TypedMap context) {
    try {
      return CraftingHelper.getCondition(buffer.readWithCodec(NbtOps.INSTANCE, ExtraCodecs.JSON).getAsJsonObject());
    } catch (JsonParseException|IllegalStateException e) {
      throw new DecoderException(e);
    }
  }

  @Override
  public void encode(FriendlyByteBuf buffer, ICondition condition) {
    buffer.writeWithCodec(NbtOps.INSTANCE, ExtraCodecs.JSON, CraftingHelper.serialize(condition));
  }
}
