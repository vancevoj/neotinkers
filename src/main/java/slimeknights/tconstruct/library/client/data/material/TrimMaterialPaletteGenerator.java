package slimeknights.tconstruct.library.client.data.material;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import slimeknights.tconstruct.library.client.data.GenericTextureGenerator;
import slimeknights.tconstruct.library.client.data.spritetransformer.ISpriteTransformer;
import slimeknights.tconstruct.library.client.data.util.DataGenSpriteReader;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Generator transforming the trim palette using our material generators.
 * Since 26.3 trim palettes live in {@code textures/palettes}: the base palette is {@code minecraft:trim_base} and material
 * palettes are referenced by {@link net.minecraft.world.item.equipment.trim.TrimMaterial#paletteId()}, so a material
 * {@code ns:path} generates the palette {@code ns:trim/path} at {@code assets/ns/textures/palettes/trim/path.png}.
 */
public class TrimMaterialPaletteGenerator extends GenericTextureGenerator {
  /** Folder containing all palettes, relative to textures */
  private static final String PALETTES = "palettes";
  /** Folder containing trim material palettes, relative to {@link #PALETTES} */
  public static final String PALETTE_FOLDER = "trim";
  private static final String PALETTE_TEXTURES = "textures/" + PALETTES;
  /** Vanilla base palette */
  private static final Identifier BASE_PALETTE = Identifier.withDefaultNamespace("trim_base");

  private final String name;
  private final MaterialId[] materials;
  private final AbstractMaterialSpriteProvider materialProvider;
  /** Path provider for the trimmed compat JSON, as 26.3 rejects the empty folder the palettes used to share with it */
  private final PackOutput.PathProvider trimmedPathProvider;
  public TrimMaterialPaletteGenerator(PackOutput packOutput, String name, ResourceManager resourceManager, AbstractMaterialSpriteProvider materialProvider, MaterialId... materials) {
    super(packOutput, resourceManager, PALETTE_TEXTURES + '/' + PALETTE_FOLDER);
    this.trimmedPathProvider = packOutput.createPathProvider(Target.RESOURCE_PACK, "maps");
    this.name = name;
    this.materialProvider = materialProvider;
    this.materials = materials;
  }

  /** Gets the sprite transformer for the given material */
  protected ISpriteTransformer getTransformer(MaterialId material) {
    return Objects.requireNonNull(materialProvider.getMaterialInfo(material), "Missing material provider " + material).getTransformer();
  }

  @SuppressWarnings("removal")
  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    assert resourceManager != null;
    DataGenSpriteReader spriteReader = new DataGenSpriteReader(resourceManager, PALETTE_TEXTURES);
    try {
      // create JSON of all materials for compat with trimmed
      JsonObject trimmedJson = new JsonObject();
      JsonObject values = new JsonObject();
      for (MaterialId material : materials) {
        values.addProperty(material.withPrefix(PALETTES + '/' + PALETTE_FOLDER + '/').toString(), material.getSuffix());
      }
      trimmedJson.add("pairs", values);

      NativeImage original = spriteReader.read(BASE_PALETTE);
      return allOf(Stream.concat(
        Stream.of(DataProvider.saveStable(cache, trimmedJson, trimmedPathProvider.json(Identifier.fromNamespaceAndPath("trimmed", "unchecked/custom_trim_material_permutations")))),
        Arrays.stream(materials).map(
        material -> saveImage(cache, material, getTransformer(material).transformCopy(original, false)))))
        .thenRunAsync(spriteReader::closeAll);
    } catch (IOException ex) {
      return CompletableFuture.failedFuture(ex);
    }
  }

  @Override
  public String getName() {
    return name + " Material Trim Palette Generator";
  }
}
