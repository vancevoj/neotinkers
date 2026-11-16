package slimeknights.tconstruct.tables;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import slimeknights.mantle.client.render.InventoryBlockEntityRenderer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.CraftingStationScreen;
import slimeknights.tconstruct.tables.client.inventory.ModifierWorktableScreen;
import slimeknights.tconstruct.tables.client.inventory.PartBuilderScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerChestScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

import java.util.List;

@SuppressWarnings("unused")
@EventBusSubscriber(modid=TConstruct.MOD_ID, value=Dist.CLIENT)
public class TableClientEvents extends ClientEventBase {
  /** Tints the tinkers chest with the color stored in its block entity */
  private static final BlockTintSource CHEST_TINT = new BlockTintSource() {
    @Override
    public int color(BlockState state) {
      // no position, so no block entity to read the color from
      return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
      if (world.getBlockEntity(pos) instanceof TinkersChestBlockEntity te) {
        // 26.3 multiplies the vertex color by the full ARGB tint, while the chest color is RGB
        return ARGB.opaque(te.getColor());
      }
      return -1;
    }
  };

  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    BlockEntityRendererProvider<TableBlockEntity, InventoryBlockEntityRenderer.State> tableRenderer = InventoryBlockEntityRenderer::new;
    event.registerBlockEntityRenderer(TinkerTables.craftingStationTile.get(), tableRenderer);
    event.registerBlockEntityRenderer(TinkerTables.tinkerStationTile.get(), tableRenderer);
    event.registerBlockEntityRenderer(TinkerTables.modifierWorktableTile.get(), tableRenderer);
    event.registerBlockEntityRenderer(TinkerTables.partBuilderTile.get(), tableRenderer);
  }

  @SubscribeEvent
  static void registerScreens(final RegisterMenuScreensEvent event) {
    event.register(TinkerTables.craftingStationContainer.get(), CraftingStationScreen::new);
    event.register(TinkerTables.tinkerStationContainer.get(), TinkerStationScreen::new);
    event.register(TinkerTables.partBuilderContainer.get(), PartBuilderScreen::new);
    event.register(TinkerTables.modifierWorktableContainer.get(), ModifierWorktableScreen::new);
    event.register(TinkerTables.tinkerChestContainer.get(), TinkerChestScreen::new);
  }

  @SubscribeEvent
  static void registerBlockColors(final RegisterColorHandlersEvent.BlockTintSources event) {
    event.register(List.of(CHEST_TINT), TinkerTables.tinkersChest.get());
  }

  // 26.3: item colors are data driven. The tinkers chest item uses the vanilla minecraft:dye tint source with the default chest color
  // in its client item JSON (datagen), which reads the same dyed color component as TinkersChestBlockItem#getColor.
}
