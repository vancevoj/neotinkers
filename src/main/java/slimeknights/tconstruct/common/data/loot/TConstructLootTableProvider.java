package slimeknights.tconstruct.common.data.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.DatapackRegistryGatherer;

import java.util.List;
import java.util.Set;

/**
 * Loot table bootstrap for all Tinkers loot tables. Since 26.3 loot tables are a reloadable registry built from bootstraps,
 * so this is registered with {@link net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent} instead of being a data provider.
 * The sub providers receive the real registry lookup, so holders such as our enchantments resolve.
 */
public class TConstructLootTableProvider extends LootTableProvider {
  private static final Set<ResourceKey<LootTable>> REQUIRED_TABLES = Set.of();

  public TConstructLootTableProvider() {
    super(REQUIRED_TABLES, List.of(
      new LootTableProvider.SubProviderEntry(BlockLootTableProvider::new, LootContextParamSets.BLOCK),
      new LootTableProvider.SubProviderEntry(AdvancementLootTableProvider::new, LootContextParamSets.ADVANCEMENT_REWARD),
      new LootTableProvider.SubProviderEntry(EntityLootTableProvider::new, LootContextParamSets.ENTITY)));
  }

  /** Registers all loot tables with the datagen registry gatherer */
  public static void register(DatapackRegistryGatherer gatherer) {
    gatherer.add(Registries.LOOT_TABLE, new TConstructLootTableProvider());
  }
}
