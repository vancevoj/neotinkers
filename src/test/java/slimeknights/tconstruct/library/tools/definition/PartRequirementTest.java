package slimeknights.tconstruct.library.tools.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import static org.assertj.core.api.Assertions.assertThat;

class PartRequirementTest extends BaseMcTest {

  @BeforeAll
  static void registerParts() {
    MaterialItemFixture.init();
  }

  @Test
  void bufferReadWrite_part() {
    PartRequirement requirement = PartRequirement.ofPart(MaterialItemFixture.MATERIAL_ITEM, 5);
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    PartRequirement.LOADABLE.encode(buffer, requirement);

    PartRequirement decoded = PartRequirement.LOADABLE.decode(buffer);
    assertThat(decoded.getPart()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM);
    assertThat(decoded.getWeight()).isEqualTo(5);
  }

  @Test
  void serializeJson_part() {
    PartRequirement requirement = PartRequirement.ofPart(MaterialItemFixture.MATERIAL_ITEM_2, 5);
    JsonElement json = PartRequirement.LOADABLE.serialize(requirement);
    assertThat(json.isJsonObject()).isTrue();
    JsonObject object = json.getAsJsonObject();
    assertThat(GsonHelper.getAsString(object, "item")).isEqualTo(Registry.ITEM.getKey(MaterialItemFixture.MATERIAL_ITEM_2).toString());
    assertThat(object.has("stat")).isFalse();
    assertThat(GsonHelper.getAsInt(object, "weight")).isEqualTo(5);

    // weight is optional if 1
    requirement = PartRequirement.ofPart(MaterialItemFixture.MATERIAL_ITEM, 1);
    json = PartRequirement.LOADABLE.serialize(requirement);
    assertThat(json.isJsonObject()).isTrue();
    object = json.getAsJsonObject();
    assertThat(GsonHelper.getAsString(object, "item")).isEqualTo(Registry.ITEM.getKey(MaterialItemFixture.MATERIAL_ITEM).toString());
    assertThat(object.has("stat")).isFalse();
    assertThat(object.has("weight")).isFalse();
  }

  @Test
  void deserializeJson_part() {
    JsonObject json = new JsonObject();
    json.addProperty("item", Registry.ITEM.getKey(MaterialItemFixture.MATERIAL_ITEM_HEAD).toString());
    json.addProperty("weight", 4);
    PartRequirement requirement = PartRequirement.LOADABLE.deserialize(json);
    assertThat(requirement.getPart()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_HEAD);
    assertThat(requirement.getWeight()).isEqualTo(4);

    // no weight defaults to 1
    json = new JsonObject();
    json.addProperty("item", Registry.ITEM.getKey(MaterialItemFixture.MATERIAL_ITEM_HANDLE).toString());
    requirement = PartRequirement.LOADABLE.deserialize(json);
    assertThat(requirement.getPart()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_HANDLE);
    assertThat(requirement.getWeight()).isEqualTo(1);
  }

  @Test
  void bufferReadWrite_stat() {
    PartRequirement requirement = PartRequirement.ofStat(HeadMaterialStats.ID, 5);
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    PartRequirement.LOADABLE.encode(buffer, requirement);

    PartRequirement decoded = PartRequirement.LOADABLE.decode(buffer);
    assertThat(decoded.getPart()).isNull();
    assertThat(decoded.getStatType()).isEqualTo(HeadMaterialStats.ID);
    assertThat(decoded.getWeight()).isEqualTo(5);
  }

  @Test
  void serializeJson_stat() {
    PartRequirement requirement = PartRequirement.ofStat(HandleMaterialStats.ID, 5);
    JsonElement json = PartRequirement.LOADABLE.serialize(requirement);
    assertThat(json.isJsonObject()).isTrue();
    JsonObject object = json.getAsJsonObject();
    assertThat(GsonHelper.getAsString(object, "stat")).isEqualTo(HandleMaterialStats.ID.toString());
    assertThat(object.has("item")).isFalse();
    assertThat(GsonHelper.getAsInt(object, "weight")).isEqualTo(5);

    // weight is optional if 1
    requirement = PartRequirement.ofStat(HeadMaterialStats.ID, 1);
    json = PartRequirement.LOADABLE.serialize(requirement);
    assertThat(json.isJsonObject()).isTrue();
    object = json.getAsJsonObject();
    assertThat(GsonHelper.getAsString(object, "stat")).isEqualTo(HeadMaterialStats.ID.toString());
    assertThat(object.has("item")).isFalse();
    assertThat(object.has("weight")).isFalse();
  }

  @Test
  void deserializeJson_stat() {
    JsonObject json = new JsonObject();
    json.addProperty("stat", HeadMaterialStats.ID.toString());
    json.addProperty("weight", 4);
    PartRequirement requirement = PartRequirement.LOADABLE.deserialize(json);
    assertThat(requirement.getPart()).isNull();
    assertThat(requirement.getStatType()).isEqualTo(HeadMaterialStats.ID);
    assertThat(requirement.getWeight()).isEqualTo(4);

    // no weight defaults to 1
    json = new JsonObject();
    json.addProperty("stat", HandleMaterialStats.ID.toString());
    requirement = PartRequirement.LOADABLE.deserialize(json);
    assertThat(requirement.getPart()).isNull();
    assertThat(requirement.getStatType()).isEqualTo(HandleMaterialStats.ID);
    assertThat(requirement.getWeight()).isEqualTo(1);
  }
}
