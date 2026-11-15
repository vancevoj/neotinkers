package slimeknights.tconstruct.library.tools.stat;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.utils.HarvestTiers;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.Objects;

/** Tool stat for comparing tool tiers */
@SuppressWarnings("ClassCanBeRecord")
@Getter @RequiredArgsConstructor
public class ToolTierStat implements IToolStat<ToolMaterial> {
  /** Name of this tool stat */
  private final ToolStatId name;

  @Override
  public boolean supports(Item item) {
    return RegistryHelper.contains(TinkerTags.Items.HARVEST, item);
  }

  @Override
  public ToolMaterial getDefaultValue() {
    return HarvestTiers.minTier();
  }

  @Override
  public Object makeBuilder() {
    return new TierBuilder(getDefaultValue());
  }

  @Override
  public ToolMaterial build(ModifierStatsBuilder parent, Object builder) {
    return ((TierBuilder) builder).value;
  }

  /**
   * Sets the tier to the new tier, keeping the largest
   * @param builder  Builder instance
   * @param value    Amount to add
   */
  @Override
  public void update(ModifierStatsBuilder builder, ToolMaterial value) {
    builder.<TierBuilder>updateStat(this, b -> b.value = HarvestTiers.max(b.value, value));
  }

  @Nullable
  @Override
  public ToolMaterial read(Tag tag) {
    if (tag instanceof StringTag(String value)) {
      Identifier tierId = Identifier.tryParse(value);
      if (tierId != null) {
        return HarvestTiers.byName(tierId);
      }
    }
    return null;
  }

  @Override
  public Tag write(ToolMaterial value) {
    Identifier id = HarvestTiers.getId(value);
    if (id != null) {
      return StringTag.valueOf(id.toString());
    }
    return null;
  }

  @Override
  public ToolMaterial deserialize(JsonElement json) {
    Identifier id = JsonHelper.convertToResourceLocation(json, getName().toString());
    ToolMaterial tier = HarvestTiers.byName(id);
    if (tier != null) {
      return tier;
    }
    throw new JsonSyntaxException("Unknown tool tier " + id);
  }

  @Override
  public JsonElement serialize(ToolMaterial value) {
    return new JsonPrimitive(Objects.requireNonNull(HarvestTiers.getId(value)).toString());
  }

  @Override
  public ToolMaterial fromNetwork(FriendlyByteBuf buffer) {
    Identifier id = buffer.readIdentifier();
    ToolMaterial tier = HarvestTiers.byName(id);
    if (tier != null) {
      return tier;
    }
    throw new DecoderException("Unknown tool tier " + id);
  }

  @Override
  public void toNetwork(FriendlyByteBuf buffer, ToolMaterial value) {
    buffer.writeIdentifier(Objects.requireNonNull(HarvestTiers.getId(value)));
  }

  @Override
  public Component formatValue(ToolMaterial value) {
    return Component.translatable(Util.makeTranslationKey("tool_stat", getName())).append(HarvestTiers.getName(value));
  }

  @Override
  public String toString() {
    return "ToolTierStat{" + name + '}';
  }

  /** Builder for a tier object */
  @AllArgsConstructor
  private static class TierBuilder {
    private ToolMaterial value;
  }
}
