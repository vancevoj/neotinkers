package slimeknights.tconstruct.common.data.loot;

import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.advancements.predicates.entity.CubeMobPredicate;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContext.EntityTarget;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.entity.ArmoredSlimeEntity;

import javax.annotation.Nullable;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class EntityLootTableProvider extends EntityLootSubProvider {
  /** Condition matching an entity that is currently on fire, used to smelt drops */
  private static final LootItemCondition.Builder ENTITY_ON_FIRE = LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true)));

  protected EntityLootTableProvider(LootTableSubProvider.Context context) {
    super(FeatureFlags.REGISTRY.allFlags(), context);
  }

  @Override
  protected Stream<EntityType<?>> getKnownEntityTypes() {
    return BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                                   // remove earth slime entity, we redirect to the vanilla loot table
                                   .filter(entry -> TConstruct.MOD_ID.equals(entry.getKey().identifier().getNamespace()))
                                   .map(Entry::getValue)
                                   // 26.3 removed canHaveLootTable, vanilla now requires a table for any type with a default loot table.
                                   // keep the old rule of skipping misc entities (projectiles and such) apart from the armor stand
                                   .filter(this::canHaveLootTable);
  }

  /** Replacement for the removed vanilla method, matching the 1.21.1 logic */
  protected boolean canHaveLootTable(EntityType<?> type) {
    return type.getCategory() != MobCategory.MISC || type == TinkerGadgets.armorStandEntity.get();
  }

  @Override
  public void generate() {
    this.add(TinkerWorld.skySlimeEntity.get(), dropSlimeballs(SlimeType.SKY, TinkerWorld.steelShard.get()));
    this.add(TinkerWorld.enderSlimeEntity.get(), dropSlimeballs(SlimeType.ENDER, TinkerWorld.knightmetalShard.get()));
    this.add(TinkerWorld.terracubeEntity.get(),
                           LootTable.lootTable().withPool(LootPool.lootPool()
                                                                   .setRolls(ContextIntProviders.exactly(1))
                                                                   .add(LootItem.lootTableItem(Items.CLAY_BALL)
                                                                                          .apply(SetItemCountFunction.setCount(ContextIntProviders.between(-2, 1)))
                                                                                          .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F)))
                                                                                          .apply(SmeltItemFunction.smelted().when(ENTITY_ON_FIRE)))));

    LootItemCondition.Builder killedByFrog = killedByFrog();
    this.add(TinkerWorld.terracubeEntity.get(),
             LootTable.lootTable()
                      .withPool(LootPool.lootPool()
                                        .setRolls(ContextIntProviders.exactly(1))
                                        .add(LootItem.lootTableItem(Items.CLAY_BALL)
                                                     .apply(SetItemCountFunction.setCount(ContextIntProviders.between(-2, 1)))
                                                     .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F)))
                                                     .when(killedByFrog.invert())
                                                     .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().cubeMob(CubeMobPredicate.sized(MinMaxBounds.Ints.atLeast(2))))))
                                        .add(LootItem.lootTableItem(TinkerSmeltery.searedLamp)
                                                     .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(1)))
                                                     .when(killedByFrog))
                                        .apply(SmeltItemFunction.smelted().when(ENTITY_ON_FIRE))));
    // not really sure why armor stands have a loot table, but vanilla does it
    this.add(TinkerGadgets.armorStandEntity.get(), LootTable.lootTable());
  }

  /** Drops an item using the same chances as slimeballs */
  private LootPoolEntryContainer.Builder<?> slimeball(Item item) {
    return LootItem.lootTableItem(item)
      .apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
      .apply(EnchantedCountIncreaseFunction.lootingMultiplier(this.enchantments, ContextFloatProviders.between(0.0F, 1.0F)));
  }

  /** Drops a frog slimeball */
  private static LootPoolEntryContainer.Builder<?> frogball(Item item) {
    return LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(1)));
  }

  /** Drops slimeballs, and optionally nuggets for metal slimes. */
  private LootTable.Builder dropSlimeballs(SlimeType type, @Nullable Item nugget) {
    LootItemCondition.Builder small = LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.Builder.entity().cubeMob(CubeMobPredicate.sized(MinMaxBounds.Ints.exactly(1))));
    LootItemCondition.Builder killedByFrog = killedByFrog();
    LootPool.Builder noFrog = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).when(killedByFrog.invert()).when(small);
    LootPool.Builder frog = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).when(killedByFrog).when(small);
    Item slimeball = TinkerCommons.slimeball.get(type);
    // if given a nugget, add that drop when metal
    if (nugget != null) {
      CompoundTag isMetal = new CompoundTag();
      isMetal.putBoolean(ArmoredSlimeEntity.TAG_METAL, true);
      LootItemCondition.Builder predicate = LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.Builder.entity().nbt(new NbtPredicate(isMetal)));
      noFrog.add(slimeball(slimeball).when(predicate.invert()));
      noFrog.add(slimeball(nugget).when(predicate));
      frog.add(frogball(slimeball).when(predicate.invert()));
      frog.add(frogball(nugget).when(predicate));
    } else {
      noFrog.add(slimeball(slimeball));
      frog.add(frogball(slimeball));
    }
    return LootTable.lootTable().withPool(noFrog).withPool(frog);
  }
}
