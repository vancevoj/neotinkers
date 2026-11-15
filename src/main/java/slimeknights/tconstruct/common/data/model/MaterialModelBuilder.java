package slimeknights.tconstruct.common.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import slimeknights.tconstruct.TConstruct;

/**
 * Builder for the {@code tconstruct:material} model loader.
 * Use with {@link net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder#customLoader(java.util.function.Supplier, java.util.function.Consumer)}.
 */
public class MaterialModelBuilder extends CustomLoaderBuilder {
  private JsonArray offset = null;
  public MaterialModelBuilder() {
    super(TConstruct.getResource("material"), false);
  }

  /** Sets the offset */
  public MaterialModelBuilder offset(int x, int y) {
    this.offset = new JsonArray(2);
    this.offset.add(x);
    this.offset.add(y);
    return this;
  }

  @Override
  protected CustomLoaderBuilder copyInternal() {
    MaterialModelBuilder builder = new MaterialModelBuilder();
    builder.offset = this.offset == null ? null : this.offset.deepCopy();
    return builder;
  }

  @Override
  public JsonObject toJson(JsonObject json) {
    super.toJson(json);
    if (offset != null) {
      json.add("offset", offset.deepCopy());
    }
    return json;
  }
}
