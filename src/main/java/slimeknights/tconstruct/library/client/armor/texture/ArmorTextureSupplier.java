package slimeknights.tconstruct.library.client.armor.texture;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.listener.ResourceValidator;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;

/** Logic to get an armor texture given a stack */
public interface ArmorTextureSupplier extends IHaveLoader {
  /** Folder for tinkers armor, intentionally not the normal armor folder to make the texture scan low risk */
  String FOLDER = "textures/tinker_armor";
  /** Validator checking if armor textures exist */
  ResourceValidator TEXTURE_VALIDATOR = new ResourceValidator(FOLDER, FOLDER, ".png");

  /** Empty module instance. Used as fallback for {@link ConditionalArmorTextureSupplier} modules. Not registered. */
  ArmorTextureSupplier EMPTY = new ArmorTextureSupplier() {
    @Override
    public ArmorTexture getArmorTexture(ItemStack stack, TextureType leggings, RegistryAccess access) {
      return ArmorTexture.EMPTY;
    }

    @Override
    public RecordLoadable<? extends ArmorTextureSupplier> getLoader() {
      throw new UnsupportedOperationException("Cannot datagen an empty texture supplier.");
    }
  };
  /** Registry for resource packs */
  GenericLoaderRegistry<ArmorTextureSupplier> LOADER = new GenericLoaderRegistry<>("Armor texture type", EMPTY, true);

  /** Gets the texture and color to display for the given stack. Use {@link ArmorTexture#EMPTY} to indicates this texture will not render */
  ArmorTexture getArmorTexture(ItemStack stack, TextureType leggings, RegistryAccess access);

  /** Pair of texture and color */
  interface ArmorTexture {
    /** Empty instance since caches don't support caching null. */
    ArmorTexture EMPTY = new ArmorTexture() {
      @Override
      public <S> void submit(Model<? super S> model, S state, PoseStack matrices, OrderedSubmitNodeCollector collector, int packedLight, int packedOverlay, int color, boolean hasGlint, int outlineColor) {}
    };

    /**
     * Submits this texture on the given model. Since 26.3 nothing renders immediately, the model is drawn later using the passed state.
     * @param model          Model to render, posed by the submit node collector using {@code state}
     * @param state          Render state for the model
     * @param matrices       Pose stack, captured at submit time
     * @param collector      Collector to submit to, already ordered by the caller so stacked layers draw in order
     * @param packedLight    Light coordinates
     * @param packedOverlay  Overlay coordinates
     * @param color          ARGB color multiplier for the whole model, -1 for no tint (formerly the red, green, blue and alpha floats)
     * @param hasGlint       If true, this layer should draw the enchantment glint
     * @param outlineColor   Entity outline color, 0 for none
     */
    <S> void submit(Model<? super S> model, S state, PoseStack matrices, OrderedSubmitNodeCollector collector, int packedLight, int packedOverlay, int color, boolean hasGlint, int outlineColor);
  }

  /** Texture variants, armor is used for helmet, chestplate, and boots, while leggings is leggings and wings is on chest for elytra */
  enum TextureType {
    ARMOR, LEGGINGS, WINGS;

    /** Gets the type for the given slot */
    public static TextureType fromSlot(EquipmentSlot slot) {
      return slot == EquipmentSlot.LEGS ? LEGGINGS : ARMOR;
    }
  }

  /**
   * Gets a texture using the named format
   */
  static Identifier getTexturePath(Identifier name) {
    return name.withPath(FOLDER + '/' + name.getPath() + ".png");
  }
}
