package slimeknights.tconstruct.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Transformable;
import slimeknights.mantle.block.StrippableLogBlock;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.block.PlatformBlock;
import slimeknights.tconstruct.shared.block.WaxedPlatformBlock;
import slimeknights.tconstruct.shared.block.WeatheringPlatformBlock;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the {@code neoforge:transformables} block data map.
 * Since NeoForge 26.3 vanilla axes no longer ask the block for its stripped, scraped, or unwaxed state, so every Tinkers block
 * that answered the old axe item abilities in {@code getToolModifiedState} needs an entry here to keep working with vanilla axes.
 * Tinkers tools still go through {@code getToolModifiedState} using the legacy abilities.
 */
public class TransformableDataMapProvider extends DataMapProvider {
  public TransformableDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
    super(packOutput, lookupProvider);
  }

  @SuppressWarnings("deprecation")  // easiest way to find all our blocks
  @Override
  protected void gather(HolderLookup.Provider provider) {
    Builder<Transformable,Block> transformables = builder(NeoForgeDataMaps.TRANSFORMABLES);
    for (Entry<ResourceKey<Block>,Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
      ResourceKey<Block> key = entry.getKey();
      if (!key.identifier().getNamespace().equals(TConstruct.MOD_ID)) {
        continue;
      }
      Block block = entry.getValue();
      // slimewood logs and wood, replaces the old AXE_STRIP ability on StrippableLogBlock
      if (block instanceof StrippableLogBlock log) {
        transformables.add(key, log.getTransformable(), false);
      }
      // copper platforms, replaces the old AXE_SCRAPE ability on WeatheringPlatformBlock
      else if (block instanceof WeatheringPlatformBlock weathering) {
        PlatformBlock scraped = weathering.getScraped();
        if (scraped != null) {
          transformables.add(key, Transformable.scraping(block, scraped), false);
        }
      }
      // waxed copper platforms, replaces the old AXE_WAX_OFF ability on WaxedPlatformBlock
      else if (block instanceof WaxedPlatformBlock waxed) {
        transformables.add(key, Transformable.waxOff(block, waxed.getUnwaxed()), false);
      }
    }
  }

  @Override
  public String getName() {
    return "Neo Tinkers Transformable Data Maps";
  }
}
