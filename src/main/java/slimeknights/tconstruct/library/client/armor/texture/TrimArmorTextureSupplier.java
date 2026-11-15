package slimeknights.tconstruct.library.client.armor.texture;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.palette.Palette;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modules.cosmetic.TrimModule;

import java.util.HashMap;
import java.util.Map;

/** Handles fetching textures for armor trims */
public record TrimArmorTextureSupplier(ModifierId modifier, Identifier patternKey, Identifier materialKey) implements ArmorTextureSupplier {
  /** Default instant using the tinkers modifier */
  public static TrimArmorTextureSupplier INSTANCE = new TrimArmorTextureSupplier(TinkerModifiers.trim.getId());
  public static final RecordLoadable<TrimArmorTextureSupplier> LOADER = RecordLoadable.create(ModifierId.PARSER.defaultField("modifier", TinkerModifiers.trim.getId(), TrimArmorTextureSupplier::modifier), TrimArmorTextureSupplier::new);

  /* Caches */
  private static final Map<String,ArmorTexture> ARMOR_CACHE = new HashMap<>();
  private static final Map<String,ArmorTexture> LEGGING_CACHE = new HashMap<>();
  /** Listener to clear caches associated with trim textures */
  public static final ResourceManagerReloadListener CACHE_INVALIDATOR = manager -> {
    ARMOR_CACHE.clear();
    LEGGING_CACHE.clear();
  };

  /** @apiNote use {@link #TrimArmorTextureSupplier(ModifierId)} */
  @Internal
  public TrimArmorTextureSupplier {}

  public TrimArmorTextureSupplier(ModifierId modifier) {
    this(modifier, TrimModule.patternKey(modifier), TrimModule.materialKey(modifier));
  }

  @Override
  public ArmorTexture getArmorTexture(ItemStack stack, TextureType textureType, RegistryAccess access) {
    if (textureType != TextureType.WINGS) {
      String patternId = ModifierUtil.getPersistentString(stack, patternKey);
      String materialId = ModifierUtil.getPersistentString(stack, materialKey);
      if (!patternId.isEmpty() && !materialId.isEmpty()) {
        String key = patternId + '#' + materialId;
        Map<String,ArmorTexture> cache = textureType == TextureType.LEGGINGS ? LEGGING_CACHE : ARMOR_CACHE;
        ArmorTexture texture = cache.get(key);
        if (texture != null) {
          return texture;
        }
        TrimPattern pattern = access.lookupOrThrow(Registries.TRIM_PATTERN).getValue(Identifier.tryParse(patternId));
        TrimMaterial material = access.lookupOrThrow(Registries.TRIM_MATERIAL).getValue(Identifier.tryParse(materialId));
        texture = ArmorTexture.EMPTY;
        if (pattern != null && material != null) {
          // 26.3 trim textures live under trims/entity/<layer type>/ and are palette swapped at runtime
          EquipmentClientInfo.LayerType layerType = textureType == TextureType.LEGGINGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
          texture = TrimArmorTexture.create(pattern.assetId().withPath(path -> layerType.trimAssetPrefix() + "/" + path), pattern.decal(), material);
        }
        cache.put(key, texture);
        return texture;
      }
    }
    return ArmorTexture.EMPTY;
  }

  @Override
  public RecordLoadable<TrimArmorTextureSupplier> getLoader() {
    return LOADER;
  }

  /**
   * Implementation of an armor texture for armor trims.
   * Since 26.3 there is no armor trims atlas, trim textures are palette swapped by the {@link PalettedTextureManager}.
   */
  @RequiredArgsConstructor
  public static class TrimArmorTexture implements ArmorTexture {
    /** Base trim texture relative to the textures folder. Needs a {@code .png.mcmeta} declaring its base palette, like the vanilla trims */
    private final Identifier baseTexture;
    /** Palette for the material, from {@link TrimMaterial#paletteId()} */
    private final Identifier paletteId;
    /** If true, the trim pattern is a decal */
    private final boolean decal;

    /** Creates the trim texture for the given root texture and material */
    private static ArmorTexture create(Identifier root, boolean decal, TrimMaterial material) {
      // start by trying and finding the material palette, replaces the material specific sprite in the trims atlas
      Identifier paletteId = material.paletteId();
      if (Minecraft.getInstance().getResourceManager().getResource(Palette.ID_CONVERTER.idToFile(paletteId)).isPresent()) {
        return new TrimArmorTexture(root, paletteId, decal);
      }
      // failed to find the palette, go for tinting the base
      int color = -1;
      TextColor textColor = material.description().getStyle().getColor();
      if (textColor != null) {
        color = textColor.getValue() | 0xFF000000;
      }
      TConstruct.LOG.error("Missing material palette {}, defaulting to tinting base texture #{}", paletteId, ColorLoadable.NO_ALPHA.getString(color));
      return new TintedArmorTexture(root.withPath("textures/" + root.getPath() + ".png"), color);
    }

    @Override
    public <S> void submit(Model<? super S> model, S state, PoseStack matrices, OrderedSubmitNodeCollector collector, int packedLight, int packedOverlay, int color, boolean hasGlint, int outlineColor) {
      // ignoring glint as odds are very low trim texture is the first one
      // the manager caches handles until the next resource reload, so looking it up on submit is cheap and never stale
      PalettedTextureManager.Handle handle = Minecraft.getInstance().getPalettedTextureManager().getOrPrepare(baseTexture, paletteId);
      collector.submitModel(model, state, matrices, RenderTypes.armorTrim(handle.textureLocation(), decal), packedLight, packedOverlay, color, handle, outlineColor);
    }
  }
}
