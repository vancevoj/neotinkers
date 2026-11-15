package slimeknights.tconstruct.world.logic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.Optional;

/**
 * Trade for wandering traders to randomly sell an ancient tool.
 * <p>
 * Villager trades are a datapack registry since 26.1, so this is now the item modifier of the {@code tconstruct:wandering_trader/ancient_tool_N} trades.
 * The trade itself gives a placeholder and wants zero emeralds; this function replaces the placeholder with a random ancient tool and adds the
 * tier based emerald cost through {@link DataComponents#ADDITIONAL_TRADE_COST}.
 * <p>
 * The trades are added to the wandering trader uncommon trade tag {@link #TRADE_COPIES} times, the config weight enables the first "weight" copies.
 * Disabled copies (and copies offered when the trader already sells an ancient tool) return an empty stack, so the trader picks another trade.
 * @param index  Index of this trade copy, starting at 1
 */
public record AncientToolItemListing(int index) implements LootItemFunction {
  public static final MapCodec<AncientToolItemListing> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    ExtraCodecs.POSITIVE_INT.optionalFieldOf("index", 1).forGetter(AncientToolItemListing::index)
  ).apply(inst, AncientToolItemListing::new));
  /**
   * Number of copies of the trade in the wandering trader tag. The trader picks 2 of the 16 vanilla uncommon trades, so 16 copies give
   * nearly the same chance per config weight as the old "1 of 6 rare trades plus weight copies" logic. Weights above this act as this value.
   */
  public static final int TRADE_COPIES = 16;

  @Override
  public MapCodec<AncientToolItemListing> codec() {
    return CODEC;
  }

  @Override
  public ItemStack apply(ItemStack stack, LootContext context) {
    // config weight controls how many of the trade copies are enabled
    if (index > Config.COMMON.wandererAncientToolWeight.get()) {
      return ItemStack.EMPTY;
    }
    // only sell one ancient tool per trader, the old rare trade slot only ever had one
    Entity trader = context.getOptional(LootContextParams.THIS_ENTITY);
    if (trader instanceof AbstractVillager villager) {
      for (MerchantOffer offer : villager.getOffers()) {
        if (offer.getResult().is(TinkerTags.Items.TRADER_TOOLS)) {
          return ItemStack.EMPTY;
        }
      }
    }

    // step 1: select ancient tool
    RandomSource random = context.getRandom();
    Optional<Holder<Item>> selected = BuiltInRegistries.ITEM.get(TinkerTags.Items.TRADER_TOOLS).flatMap(t -> t.getRandomElement(random));
    if (selected.isPresent() && selected.get().value() instanceof IModifiable toolItem) {
      // step 2: select materials
      ToolStack tool = ToolBuildHandler.buildToolRandomMaterials(toolItem, RandomMaterial.ancient(), random);
      // step 3: calculate cost based on tier
      float tier = 0;
      MaterialNBT materials = tool.getMaterials();
      if (materials.isEmpty()) {
        // if no materials, just choose a baseline tier of 2
        tier = 2;
      } else {
        for (MaterialVariant material : materials) {
          tier += material.get().getTier();
        }
        tier /= materials.size();
      }
      // formula is a cost of 6-8 emeralds per tier, meaning cost ranges from 6 (min tier 1) to 32 (max tier 4)
      int cost = Math.round(tier * 6) + random.nextInt(Math.round(2 * tier) + 1);
      ItemStack result = tool.createStack();
      // the trade wants 0 emeralds, the cost is added on top through the component (removed again by the trade)
      result.set(DataComponents.ADDITIONAL_TRADE_COST, cost);
      return result;
    }
    return ItemStack.EMPTY;
  }


  /* Data */

  /** Gets the key for the trade copy with the given index */
  public static ResourceKey<VillagerTrade> tradeKey(int index) {
    return ResourceKey.create(Registries.VILLAGER_TRADE, TConstruct.getResource("wandering_trader/ancient_tool_" + index));
  }

  /**
   * Creates the trade copy with the given index.
   * Matches the old merchant offer: 1 use, 15 experience and a price multiplier of 1.
   */
  public static VillagerTrade createTrade(int index) {
    return VillagerTrade.builder(new TradeCost(Items.EMERALD, 0), new ItemStackTemplate(TinkerTools.pickaxe.get()), 1, 15, 1.0f)
      .addModifier(() -> new AncientToolItemListing(index))
      .build();
  }

  /** Registers all trade copies, used by datagen */
  public static void bootstrap(BootstrapContext<VillagerTrade> context) {
    for (int i = 1; i <= TRADE_COPIES; i++) {
      context.register(tradeKey(i), createTrade(i));
    }
  }
}
