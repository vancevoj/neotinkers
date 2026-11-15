package slimeknights.tconstruct.smeltery;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tables.TinkerTables;

/**
 * Central mod-bus handler that registers the block-side {@code FluidHandler.BLOCK} and {@code ItemHandler.BLOCK}
 * capabilities for every block entity that exposed {@code ForgeCapabilities.FLUID_HANDLER} or
 * {@code ForgeCapabilities.ITEM_HANDLER} in the original 1.20.1 Forge build.
 * <p>
 * In NeoForge 1.21.1 a block capability is only visible to buckets, hoppers, and pipes if it is registered here via
 * {@link RegisterCapabilitiesEvent#registerBlockEntity}; the block entities only provide the getter methods.
 * <p>
 * Each provider lambda receives {@code (blockEntity, Direction side)}; {@code side} may be null. Getters that ignore the
 * side return the same handler for every face, matching the original {@code getCapability} behavior.
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = TConstruct.MOD_ID)
public final class SmelteryCapabilities {
  private SmelteryCapabilities() {}

  @SubscribeEvent
  static void registerCapabilities(RegisterCapabilitiesEvent event) {
    /* Fluid handlers (FluidHandler.BLOCK) */

    // basic tank component
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.tank.get(),
      (be, side) -> be.getFluidHandler(side));
    // fluid cannon extends the tank, inherits getFluidHandler
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.fluidCannon.get(),
      (be, side) -> be.getFluidHandler(side));
    // channel exposes a side-aware fluid handler
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.channel.get(),
      (be, side) -> be.getFluidHandler(side));
    // melter and alloyer expose their internal tank
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.melter.get(),
      (be, side) -> be.getTank());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.alloyer.get(),
      (be, side) -> be.getTank());
    // casting basin and table expose their casting fluid handler
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.basin.get(),
      (be, side) -> be.getTank());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.table.get(),
      (be, side) -> be.getTank());
    // casting tank exposes its internal tank
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.castingTank.get(),
      (be, side) -> be.getTank());
    // drains and ducts proxy the smeltery/foundry tank via the cached IO handler
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.drain.get(),
      (be, side) -> be.getHandler(side));
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.duct.get(),
      (be, side) -> be.getHandler(side));
    // proxy tank exposes the proxied item tank, which is also an IFluidHandler
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, TinkerSmeltery.proxyTank.get(),
      (be, side) -> be.getItemTank());


    /* Item handlers (ItemHandler.BLOCK) */

    // heater single-item inventory
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.heater.get(),
      (be, side) -> be.getItemHandler());
    // smeltery and foundry expose the melting inventory
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.smeltery.get(),
      (be, side) -> be.getMeltingInventory());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.foundry.get(),
      (be, side) -> be.getMeltingInventory());
    // melter exposes its melting inventory
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.melter.get(),
      (be, side) -> be.getItemHandler());
    // chute and duct expose their item handlers
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.chute.get(),
      (be, side) -> be.getHandler(side));
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.duct.get(),
      (be, side) -> be.getItemHandler());
    // fluid cannon item slot
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.fluidCannon.get(),
      (be, side) -> be.getItemHandler());
    // proxy tank exposes the proxied item tank, which is also an IItemHandler
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.proxyTank.get(),
      (be, side) -> be.getItemTank());
    // casting basin, table, and casting tank expose their (sided) inventory wrapper
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.basin.get(),
      (be, side) -> be.getItemHandler());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.table.get(),
      (be, side) -> be.getItemHandler());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerSmeltery.castingTank.get(),
      (be, side) -> be.getItemHandler());

    // crafting tables and worktables (all extend Mantle InventoryBlockEntity)
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerTables.craftingStationTile.get(),
      (be, side) -> be.getItemHandler());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerTables.tinkerStationTile.get(),
      (be, side) -> be.getItemHandler());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerTables.partBuilderTile.get(),
      (be, side) -> be.getItemHandler());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerTables.modifierWorktableTile.get(),
      (be, side) -> be.getItemHandler());
    // tinkers' chests
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerTables.tinkersChestTile.get(),
      (be, side) -> be.getItemHandler());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerTables.partChestTile.get(),
      (be, side) -> be.getItemHandler());
    event.registerBlockEntity(Capabilities.Item.BLOCK, TinkerTables.castChestTile.get(),
      (be, side) -> be.getItemHandler());
  }
}
