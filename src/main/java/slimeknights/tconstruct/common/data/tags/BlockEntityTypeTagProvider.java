package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.data.IntrinsicTagsProvider;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.concurrent.CompletableFuture;

public class BlockEntityTypeTagProvider extends IntrinsicTagsProvider<BlockEntityType<?>> {
  public BlockEntityTypeTagProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider) {
    super(packOutput, BuiltInRegistries.BLOCK_ENTITY_TYPE, lookupProvider, TConstruct.MOD_ID);
  }

  /** Creates a RL for iron chests */
  private static void ironchest(IntrinsicTagAppender<BlockEntityType<?>> appender, String name) {
    Identifier chest = Identifier.fromNamespaceAndPath("ironchest", name + "_chest");
    appender.addOptional(chest).addOptional(chest.withPrefix("trapped_"));
    if (!"dirt".equals(name)) {
      appender.addOptional(Identifier.fromNamespaceAndPath("ironshulkerbox", name + "_shulker_box"));
    }
  }

  @Override
  protected void addTags(Provider provider) {
    IntrinsicTagAppender<BlockEntityType<?>> sideInventories = tag(TinkerTags.TileEntityTypes.SIDE_INVENTORIES);
    sideInventories.add(
      BlockEntityTypes.CHEST, BlockEntityTypes.TRAPPED_CHEST, BlockEntityTypes.BARREL, BlockEntityTypes.SHULKER_BOX,
      BlockEntityTypes.DISPENSER, BlockEntityTypes.DROPPER, BlockEntityTypes.HOPPER);
    // TODO 1.21: verify if BlockEntityTypes.CHISELED_BOOKSHELF has fixed the bug where setItem(ItemStack.EMPTY) doesn't work so it can be whitelisted.
    sideInventories.addOptional(Identifier.fromNamespaceAndPath("immersiveengineering", "woodencrate"));
    ironchest(sideInventories, "iron");
    ironchest(sideInventories, "gold");
    ironchest(sideInventories, "diamond");
    ironchest(sideInventories, "copper");
    ironchest(sideInventories, "crystal");
    ironchest(sideInventories, "obsidian");
    ironchest(sideInventories, "dirt");

    // these block entities don't fully sync the fluid to client, so show simplified information
    tag(MantleTags.BlockEntities.HIDES_GAUGE_AMOUNT).add(TinkerSmeltery.faucet.get(), TinkerSmeltery.channel.get());
  }

  @Override
  public String getName() {
    return "Neo Tinkers Block Entity Type Tags";
  }
}
