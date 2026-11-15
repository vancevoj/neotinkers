package slimeknights.tconstruct.common.data.loot;

import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.LootContext.EntityTarget;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import slimeknights.mantle.loot.AddEntryLootModifier;
import slimeknights.mantle.loot.ReplaceItemLootModifier;
import slimeknights.mantle.loot.condition.BlockTagLootCondition;
import slimeknights.mantle.loot.condition.ContainsItemModifierLootCondition;
import slimeknights.mantle.loot.entry.TagPreferenceLootEntry;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.json.BlockOrEntityCondition;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.library.json.loot.HasLootContextSetCondition;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.smeltery.data.SmelteryCompat;
import slimeknights.tconstruct.smeltery.data.SmelteryCompat.CompatType;
import slimeknights.tconstruct.tools.data.ModifierIds;
import slimeknights.tconstruct.tools.modifiers.ModifierLootModifier;
import slimeknights.tconstruct.tools.modifiers.loot.ChrysophiliteBonusFunction;
import slimeknights.tconstruct.tools.modifiers.loot.ChrysophiliteLootCondition;
import slimeknights.tconstruct.tools.modifiers.loot.HasModifierLootCondition;
import slimeknights.tconstruct.tools.modifiers.loot.ModifierBonusLootFunction;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static slimeknights.mantle.Mantle.commonResource;

public class GlobalLootModifiersProvider extends GlobalLootModifierProvider {
  public GlobalLootModifiersProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(output, registries, TConstruct.MOD_ID);
  }

  /** Gets a holder set for the given tag, falling back to an unbound set if the datagen lookup has no tags */
  private <T> HolderSet<T> tag(ResourceKey<? extends Registry<? extends T>> registry, TagKey<T> tag) {
    HolderLookup.RegistryLookup<T> lookup = this.registries.lookupOrThrow(registry);
    try {
      Optional<HolderSet.Named<T>> named = lookup.get(tag);
      if (named.isPresent()) {
        return named.get();
      }
    } catch (IllegalStateException e) {
      // tags are not bound during datagen, fall through to the unbound set
    }
    return HolderSet.emptyNamed(lookup, tag);
  }

  @SuppressWarnings("removal")
  @Override
  protected void start() {
    add("wither_bone", ReplaceItemLootModifier.builder(Ingredient.of(Items.BONE), ItemOutput.fromItem(TinkerMaterials.necroticBone))
      .addCondition(LootTableIdCondition.builder(Identifier.parse("entities/wither_skeleton")).build())
      .addCondition(ConfigEnabledCondition.WITHER_BONE_DROP)
      .build());

    // generic modifier hook
    // TODO: look into migrating this fully to loot tables
    add("modifier_hook", ModifierLootModifier.builder().addCondition(BlockOrEntityCondition.INSTANCE).build());

    // tasty drops more bacon
    add("tasty_bacon", AddEntryLootModifier.builder(LootItem.lootTableItem(TinkerCommons.bacon))
      // this target must be a bacon producer
      .addCondition(new HasLootContextSetCondition(LootContextParamSets.ENTITY))
      .addCondition(LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.Builder.entity().entityType(new EntityTypePredicate(tag(Registries.ENTITY_TYPE, TinkerTags.EntityTypes.BACON_PRODUCER)))).build())
      .addCondition(new HasModifierLootCondition(ModifierIds.tasty))
      // 25% chance to drop
      .addFunction(SetItemCountFunction.setCount(ContextIntProviders.between(-2, 1)).build())
      // each looting adds a chance of +1
      .addFunction(EnchantedCountIncreaseFunction.lootingMultiplier(this.registries.lookupOrThrow(Registries.ENCHANTMENT), ContextFloatProviders.between(0, 1)).build())
      .build());

    // chrysophilite modifier hook
    add("chrysophilite_modifier", AddEntryLootModifier.builder(LootItem.lootTableItem(Items.GOLD_NUGGET))
      .addCondition(new BlockTagLootCondition(TinkerTags.Blocks.CHRYSOPHILITE_ORES))
      .addCondition(new ContainsItemModifierLootCondition(IngredientHelper.tag(TinkerTags.Items.CHRYSOPHILITE_ORES)).inverted())
      .addCondition(ChrysophiliteLootCondition.INSTANCE)
      .addFunction(SetItemCountFunction.setCount(ContextIntProviders.between(2, 6)).build())
      .addFunction(ChrysophiliteBonusFunction.oreDrops(false).build())
      .addFunction(ApplyExplosionDecay.explosionDecay().build())
      .build());

    // lustrous implementation
    addLustrous("iron", false);
    addLustrous("gold", false);
    addLustrous("copper", false);
    addLustrous("cobalt", false);
    addLustrous("netherite_scrap", false);
    for (SmelteryCompat compat : SmelteryCompat.values()) {
      if (compat.getType() == CompatType.ORE) {
        addLustrous(compat.getName(), true);
      }
    }
  }

  /** Adds lustrous for an ore */
  private void addLustrous(String name, boolean optional) {
    TagKey<Item> nuggets = TagKey.create(Registries.ITEM, commonResource("nuggets/" + name));
    Identifier ores = commonResource("ores/" + name);
    AddEntryLootModifier.Builder builder = AddEntryLootModifier.builder(TagPreferenceLootEntry.tagPreference(nuggets));
    builder.addCondition(new BlockTagLootCondition(TagKey.create(Registries.BLOCK, ores)))
           .addCondition(new ContainsItemModifierLootCondition(IngredientHelper.tag(TagKey.create(Registries.ITEM, ores))).inverted());
    if (optional) {
      builder.addCondition(new TagFilledCondition<>(nuggets));
    }
    AddEntryLootModifier modifier = builder.addCondition(new HasModifierLootCondition(ModifierIds.lustrous))
      .addFunction(SetItemCountFunction.setCount(ContextIntProviders.between(2, 4)).build())
      .addFunction(ModifierBonusLootFunction.oreDrops(ModifierIds.lustrous, false).build())
      .addFunction(ApplyExplosionDecay.explosionDecay().build())
      .build();
    if (optional) {
      // 26.3 ingredients refuse tags that do not exist, so skip the whole modifier when no mod adds the ore
      add("lustrous/" + name, modifier, new TagFilledCondition<>(TagKey.create(Registries.ITEM, ores)));
    } else {
      add("lustrous/" + name, modifier);
    }
  }
}
