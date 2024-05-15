package slimeknights.tconstruct.library.tools.item.armor.texture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

/**
 * Armor texture supplier that supplies a fixed texture that is colored using the given persistent data key
 */
public class DyedArmorTextureSupplier implements ArmorTextureSupplier {
  public static final RecordLoadable<DyedArmorTextureSupplier> LOADER = RecordLoadable.create(
    Loadables.RESOURCE_LOCATION.requiredField("name", s -> s.name),
    ModifierId.PARSER.requiredField("modifier", s -> s.modifier),
    BooleanLoadable.INSTANCE.defaultField("always_render", false, s -> s.alwaysRender),
    DyedArmorTextureSupplier::new);

  private final ResourceLocation name;
  private final ModifierId modifier;
  private final boolean alwaysRender;
  private final String[] textures;

  public DyedArmorTextureSupplier(ResourceLocation name, ModifierId modifier, boolean alwaysRender) {
    this.name = name;
    this.modifier = modifier;
    this.alwaysRender = alwaysRender;
    this.textures = new String[] {
      FixedArmorTextureSupplier.getTexture(name, "armor"),
      FixedArmorTextureSupplier.getTexture(name, "leggings"),
      FixedArmorTextureSupplier.getTexture(name, "wings"),
    };
  }

  public DyedArmorTextureSupplier(ResourceLocation base, String variant, ModifierId modifier, boolean alwaysRender) {
    this(LocationExtender.INSTANCE.suffix(base, variant), modifier, alwaysRender);
  }

  @Override
  public ArmorTexture getArmorTexture(ItemStack stack, TextureType textureType) {
    if (ModifierUtil.getModifierLevel(stack, modifier) > 0) {
      int color = ModifierUtil.getPersistentInt(stack, modifier, -1);
      if (alwaysRender || color != -1) {
        return new ArmorTexture(textures[textureType.ordinal()], 0xFF000000 | color);
      }
    }
    return ArmorTexture.EMPTY;
  }

  @Override
  public RecordLoadable<DyedArmorTextureSupplier> getLoader() {
    return LOADER;
  }
}
