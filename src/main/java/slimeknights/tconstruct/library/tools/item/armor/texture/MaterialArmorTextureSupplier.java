package slimeknights.tconstruct.library.tools.item.armor.texture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.SimpleCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Logic to create material texture variants for armor */
@RequiredArgsConstructor
public abstract class MaterialArmorTextureSupplier implements ArmorTextureSupplier {
  /** List of material caches for clearing when resource packs reload */
  private static final List<SimpleCache<String,ArmorTexture>> CACHES = new ArrayList<>();
  /** Field for parsing the variant from JSON */
  private static final LoadableField<ResourceLocation,MaterialArmorTextureSupplier> NAME_FIELD = Loadables.RESOURCE_LOCATION.requiredField("name", m -> m.name);

  /** Listener to clear render type cache */
  @Internal
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> {
    for (SimpleCache<String,ArmorTexture> cache : CACHES) {
      cache.clear();
    }
  };

  /** Makes a material getter for the given base and type */
  public static Function<String,ArmorTexture> materialGetter(ResourceLocation name) {
    SimpleCache<String,ArmorTexture> cache = new SimpleCache<>(material -> {
      if (!material.isEmpty() && FMLEnvironment.dist == Dist.CLIENT) {
        return ClientOnly.getTexture(name, material);
      }
      return ArmorTexture.EMPTY;
    });
    CACHES.add(cache);
    return cache;
  }

  private final ResourceLocation name;
  private final Function<String, ArmorTexture>[] textures;
  @SuppressWarnings("unchecked")
  public MaterialArmorTextureSupplier(ResourceLocation name) {
    this.name = name;
    this.textures = new Function[] {
      materialGetter(LocationExtender.INSTANCE.suffix(name, "/armor")),
      materialGetter(LocationExtender.INSTANCE.suffix(name, "/leggings")),
      materialGetter(LocationExtender.INSTANCE.suffix(name, "/wings"))
    };
  }

  /** Gets the material from a given stack */
  protected abstract String getMaterial(ItemStack stack);

  @Override
  public ArmorTexture getArmorTexture(ItemStack stack, TextureType textureType) {
    String material = getMaterial(stack);
    if (!material.isEmpty()) {
      return textures[textureType.ordinal()].apply(material);
    }
    return ArmorTexture.EMPTY;
  }

  /** Material supplier using persistent data */
  public static class PersistentData extends MaterialArmorTextureSupplier {
    public static final RecordLoadable<PersistentData> LOADER = RecordLoadable.create(
      NAME_FIELD,
      Loadables.RESOURCE_LOCATION.requiredField("material_key", d -> d.key),
      PersistentData::new);

    private final ResourceLocation key;

    public PersistentData(ResourceLocation name, ResourceLocation key) {
      super(name);
      this.key = key;
    }

    @Override
    protected String getMaterial(ItemStack stack) {
      return ModifierUtil.getPersistentString(stack, key);
    }

    @Override
    public RecordLoadable<PersistentData> getLoader() {
      return LOADER;
    }
  }

  /** Material supplier using material data */
  public static class Material extends MaterialArmorTextureSupplier {
    public static final RecordLoadable<Material> LOADER = RecordLoadable.create(
      NAME_FIELD,
      IntLoadable.FROM_ZERO.requiredField("index", m -> m.index),
      Material::new);

    private final int index;
    public Material(ResourceLocation name, int index) {
      super(name);
      this.index = index;
    }

    public Material(ResourceLocation base, String variant, int index) {
      this(LocationExtender.INSTANCE.suffix(base, variant), index);
    }

    @Override
    protected String getMaterial(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains(ToolStack.TAG_MATERIALS, Tag.TAG_LIST)) {
        return tag.getList(ToolStack.TAG_MATERIALS, Tag.TAG_STRING).getString(index);
      }
      return "";
    }

    @Override
    public RecordLoadable<Material> getLoader() {
      return LOADER;
    }
  }

  /** Cache for validating and fetching material textures */
  @SuppressWarnings("unchecked")
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class MaterialSetCache {
    public static final Function<ResourceLocation,MaterialSetCache> FACTORY = Util.memoize(MaterialSetCache::new);

    private final ResourceLocation name;
    private Function<String,ArmorTexture>[] armor = new Function[0];
    private Function<String,ArmorTexture>[] leggings = new Function[0];
    private Function<String,ArmorTexture>[] wings = new Function[0];

    /** Ensures the given size is supported */
    public void checkSize(int size) {
      int oldSize = armor.length;
      if (armor.length < size) {
        armor = Arrays.copyOf(armor, size);
        leggings = Arrays.copyOf(leggings, size);
        wings = Arrays.copyOf(wings, size);
        for (int i = oldSize; i < size; i++) {
          armor[i] = materialGetter(LocationExtender.INSTANCE.suffix(name, "/armor_" + i));
          leggings[i] = materialGetter(LocationExtender.INSTANCE.suffix(name, "/leggings_" + i));
          wings[i] = materialGetter(LocationExtender.INSTANCE.suffix(name, "/wings_" + i));
        }
      }
    }

    /** Gets the texture with the given material */
    public ArmorTexture getTexture(String material, int index, TextureType type) {
      return (switch (type) {
        case ARMOR -> armor;
        case LEGGINGS -> leggings;
        case WINGS -> wings;
      })[index].apply(material);
    }
  }

  private static class ClientOnly {
    /** Maps the given name to a texture */
    private static String makeTexture(ResourceLocation name) {
      return name.getNamespace() + ':' + FOLDER + '/' + name.getPath() + ".png";
    }

    /** Makes a texture for the given variant and material, returns null if its missing */
    private static ArmorTexture tryTexture(ResourceLocation name, int color, String material) {
      ResourceLocation texture = LocationExtender.INSTANCE.suffix(name, material);
      if (ARMOR_VALIDATOR.test(texture)) {
        return new ArmorTexture(makeTexture(texture), color);
      }
      return ArmorTexture.EMPTY;
    }

    /** Gets the armor texture using the material render info */
    public static ArmorTexture getTexture(ResourceLocation name, String materialStr) {
      MaterialVariantId material = MaterialVariantId.tryParse(materialStr);
      int color = -1;
      if (material != null) {
        Optional<MaterialRenderInfo> infoOptional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
        if (infoOptional.isPresent()) {
          MaterialRenderInfo info = infoOptional.get();
          ResourceLocation untinted = info.getTexture();
          if (untinted != null) {
            ArmorTexture texture = tryTexture(name, -1, '_' + untinted.getNamespace() + '_' + untinted.getPath());
            if (texture != ArmorTexture.EMPTY) {
              return texture;
            }
          }
          color = info.getVertexColor();
          for (String fallback : info.getFallbacks()) {
            ArmorTexture texture = tryTexture(name, color, '_' + fallback);
            if (texture != ArmorTexture.EMPTY) {
              return texture;
            }
          }
        }
      }
      return tryTexture(name, color, "");
    }
  }
}
