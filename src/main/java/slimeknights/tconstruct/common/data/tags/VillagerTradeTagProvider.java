package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.VillagerTradesTagsProvider;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.item.trading.VillagerTrade;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.logic.AncientToolItemListing;

import java.util.concurrent.CompletableFuture;

/**
 * Tags for villager trades. Since 26.3 wandering trader trades are a datapack registry selected through trade tags,
 * so the ancient tool trades are added to the uncommon trade tag here instead of through the removed wandering trader event.
 */
public class VillagerTradeTagProvider extends VillagerTradesTagsProvider {
  public VillagerTradeTagProvider(PackOutput output, CompletableFuture<Provider> lookupProvider) {
    super(output, lookupProvider, TConstruct.MOD_ID);
  }

  @Override
  protected void addTags(Provider provider) {
    // the config weight enables the first N copies of the trade, so all copies go in the tag
    TagAppender<VillagerTrade> uncommon = tag(VillagerTradeTags.WANDERING_TRADER_UNCOMMON);
    for (int i = 1; i <= AncientToolItemListing.TRADE_COPIES; i++) {
      uncommon.add(AncientToolItemListing.tradeKey(i));
    }
  }

  @Override
  public String getName() {
    return "Neo Tinkers Villager Trade Tags";
  }
}
