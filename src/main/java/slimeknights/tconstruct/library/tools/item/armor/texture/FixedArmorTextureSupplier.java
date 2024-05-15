package slimeknights.tconstruct.library.tools.item.armor.texture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

import javax.annotation.Nullable;

/**
 * Armor texture supplier that supplies a fixed texture, optionally filtered on a modifier
 */
public class FixedArmorTextureSupplier implements ArmorTextureSupplier {
  public static final RecordLoadable<FixedArmorTextureSupplier> LOADER = RecordLoadable.create(
    Loadables.RESOURCE_LOCATION.requiredField("name", s -> s.name),
    StringLoadable.DEFAULT.defaultField("suffix", "", s -> s.suffix),
    ColorLoadable.ALPHA.defaultField("color", -1, s -> s.textures[0].color()),
    ModifierId.PARSER.nullableField("modifier", s -> s.modifier),
    FixedArmorTextureSupplier::new);
  public static final String VARIANT = "{variant}";

  private final ResourceLocation name;
  private final String suffix;
  @Nullable
  private final ModifierId modifier;
  private final ArmorTexture[] textures;
  public FixedArmorTextureSupplier(ResourceLocation name, String suffix, int color, @Nullable ModifierId modifier) {
    this.name = name;
    this.suffix = suffix;
    this.modifier = modifier;
    this.textures = new ArmorTexture[] {
      new ArmorTexture(getTexture(name, "armor" + suffix), color),
      new ArmorTexture(getTexture(name, "leggings" + suffix), color),
      new ArmorTexture(getTexture(name, "wings" + suffix), color),
    };
  }

  /**
   * Gets a texture using the named format
   */
  public static String getTexture(ResourceLocation base, String variant) {
    return base.getNamespace() + ':' + FOLDER + '/' + base.getPath() + variant + ".png";
  }

  @Override
  public ArmorTexture getArmorTexture(ItemStack stack, TextureType textureType) {
    if (modifier == null || ModifierUtil.getModifierLevel(stack, modifier) > 0) {
      return textures[textureType.ordinal()];
    }
    return ArmorTexture.EMPTY;
  }

  @Override
  public RecordLoadable<FixedArmorTextureSupplier> getLoader() {
    return LOADER;
  }


  /* Builder */

  /** Creates a new builder instance */
  public static Builder builder(ResourceLocation base, String variant) {
    return new Builder(LocationExtender.INSTANCE.suffix(base, variant));
  }

  @Accessors(fluent = true)
  @Setter
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final ResourceLocation name;
    @Nullable
    private ModifierId modifier;
    private int color = -1;
    private String suffix = "";

    /** Sets the suffix to a material variant */
    public Builder materialSuffix(MaterialVariantId id) {
      this.suffix = '_' + id.getSuffix();
      return this;
    }

    public FixedArmorTextureSupplier build() {
      return new FixedArmorTextureSupplier(name, suffix, color, modifier);
    }
  }
}
