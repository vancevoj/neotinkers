package slimeknights.tconstruct.common.data.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

public class AdvancementLootTableProvider implements LootTableSubProvider {
  private final LootTableSubProvider.Context context;
  public AdvancementLootTableProvider(LootTableSubProvider.Context context) {
    this.context = context;
  }

  @Override
  public void run() {
    context.accept(ResourceKey.create(Registries.LOOT_TABLE, TConstruct.getResource("gameplay/starting_book")), LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(TinkerCommons.materialsAndYou))));
  }
}
