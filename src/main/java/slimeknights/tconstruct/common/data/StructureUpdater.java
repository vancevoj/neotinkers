package slimeknights.tconstruct.common.data;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerUpper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.data.GenericNBTProvider;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Data provider to update structures to a newer data fixer upper version
 * Based on https://github.com/BluSunrize/ImmersiveEngineering/blob/1.19.2/src/datagen/java/blusunrize/immersiveengineering/data/StructureUpdater.java
 */
@SuppressWarnings("unused")  // we use it once each update then disable as its task need not be repeated
public class StructureUpdater extends GenericNBTProvider {
  private final String basePath;
  private final String modId;
  private final ResourceManager resources;

  /**
   * Creates a new structure updater
   * @param output     Pack output
   * @param resources  Resource manager matching the pack type, from {@link net.neoforged.neoforge.data.event.GatherDataEvent#getResourceManager(net.minecraft.server.packs.PackType)}
   *                   ({@code SERVER_DATA} for {@link Target#DATA_PACK}, {@code CLIENT_RESOURCES} for {@link Target#RESOURCE_PACK})
   * @param modId      Mod ID to update
   * @param packType   Output pack type
   * @param basePath   Folder containing the structures
   */
  public StructureUpdater(PackOutput output, ResourceManager resources, String modId, Target packType, String basePath) {
    super(output, packType, basePath);
    this.modId = modId;
    this.basePath = basePath;
    this.resources = resources;
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    return GenericDataProvider.allOf(
      resources.listResources(basePath, file -> file.getNamespace().equals(modId) && file.getPath().endsWith(".nbt"))
               .entrySet().stream()
               .<CompletableFuture<?>>map(entry -> process(localize(entry.getKey()), entry.getValue(), cache))
               .filter(Objects::nonNull));
  }

  /** Updates the given structure */
  @Nullable
  private CompletableFuture<?> process(Identifier location, Resource resource, CachedOutput cache) {
    try {
      CompoundTag inputNBT = NbtIo.readCompressed(resource.open(), NbtAccounter.unlimitedHeap());
      CompoundTag converted = updateNBT(inputNBT);
      if (!converted.equals(inputNBT)) {
        Class<? extends DataFixer> fixerClass = DataFixers.getDataFixer().getClass();
        if (!fixerClass.equals(DataFixerUpper.class)) {
          throw new RuntimeException("Structures are not up to date, but unknown data fixer is in use: " + fixerClass.getName());
        }
        return saveNBT(cache, location, converted);
      }
      return null;
    } catch (IOException e) {
      TConstruct.LOG.error("Couldn't read NBT for {}", location, e);
      return CompletableFuture.failedFuture(e);
    }
  }

  @SuppressWarnings("deprecation")  // I'd like to see Forge try this
  private static CompoundTag updateNBT(CompoundTag nbt) {
    final CompoundTag updatedNBT = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), nbt, nbt.getIntOr("DataVersion", 0));
    StructureTemplate template = new StructureTemplate();
    template.load(BuiltInRegistries.BLOCK, updatedNBT);
    return template.save(new CompoundTag());
  }

  @Override
  public String getName() {
    return "Update structure files in " + basePath;
  }
}
