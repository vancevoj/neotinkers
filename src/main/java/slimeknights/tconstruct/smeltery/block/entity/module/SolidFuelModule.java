package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;

import javax.annotation.Nullable;

/** Fuel module variant that supports both item and fluid fuels. Only supports a single fluid position which should not change. */
public class SolidFuelModule extends FuelModule {
  /** Location of the fuel tank */
  private final BlockPos fuelPos;
  /**
   * Server-side cache of the item handler at the fuel position. {@link BlockCapabilityCache} auto-tracks invalidation
   * and refetches, so it self-heals across neighbor changes and world reloads. Null until first resolved; only valid
   * on a {@link ServerLevel}.
   */
  @Nullable
  private BlockCapabilityCache<ResourceHandler<ItemResource>,?> itemHandlerCache;

  public SolidFuelModule(MantleBlockEntity parent, BlockPos fuelPos) {
    super(parent);
    this.fuelPos = fuelPos;
  }


  /* Capability resolution */

  /** Resolves the fluid handler at the fuel position, self-healing on the server and re-querying on the client */
  @Nullable
  private ResourceHandler<FluidResource> getFluidHandler() {
    fluidHandler = getHandlerAt(fuelPos);
    return fluidHandler;
  }

  /** Resolves the item handler at the fuel position, self-healing on the server and re-querying on the client */
  @Nullable
  private ResourceHandler<ItemResource> getItemHandler() {
    Level level = getLevel();
    if (level instanceof ServerLevel serverLevel) {
      if (itemHandlerCache == null) {
        itemHandlerCache = BlockCapabilityCache.create(Capabilities.Item.BLOCK, serverLevel, fuelPos, null);
      }
      return itemHandlerCache.getCapability();
    }
    // client: always re-query so a transiently-missing capability recovers after reload
    return level.getCapability(Capabilities.Item.BLOCK, fuelPos, null);
  }


  /* Fuel updating */

  /**
   * Gets the burn time of the given solid fuel. The burn time component can only be resolved on the server,
   * so the client only knows if the item is a fuel (fuel is only consumed on the server).
   * @param stack  Fuel stack
   * @return  Burn time, or a positive placeholder client side if the stack is a fuel
   */
  private int getSolidBurnTime(ItemStack stack) {
    Level level = getLevel();
    if (level instanceof ServerLevel serverLevel) {
      // prefer the block entity holding the fuel for the loot context
      BlockEntity holder = level.getBlockEntity(fuelPos);
      return getBurnTime(stack, serverLevel, holder != null ? holder : parent);
    }
    return isSolidFuel(stack) ? 4 : 0;
  }

  /**
   * Tries to consume fuel from the given fluid handler
   * @param handler  Handler to consume fuel from
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  private int trySolidFuel(ResourceHandler<ItemResource> handler, boolean consume) {
    for (int i = 0; i < handler.size(); i++) {
      ItemResource resource = handler.getResource(i);
      if (resource.isEmpty()) {
        continue;
      }
      ItemStack stack = resource.toStack(handler.getAmountAsInt(i));
      int time = getSolidBurnTime(stack) / 4;
      if (time > 0) {
        MeltingFuel solid = MeltingFuelLookup.getSolid();
        if (consume) {
          int extractedCount;
          try (Transaction transaction = Transaction.openRoot()) {
            extractedCount = handler.extract(i, resource, 1, transaction);
            transaction.commit();
          }
          ItemStack extracted = extractedCount > 0 ? resource.toStack(extractedCount) : ItemStack.EMPTY;
          if (ItemStack.isSameItem(extracted, stack)) {
            fuel += time;
            fuelQuality = time;
            temperature = solid.getTemperature();
            rate = solid.getRate();
            parent.setChangedFast();
            // return the container
            ItemStackTemplate remainder = extracted.getCraftingRemainder();
            ItemStack container = remainder != null ? remainder.create() : ItemStack.EMPTY;
            if (!container.isEmpty()) {
              // if we cannot insert the container back, spit it on the ground
              ItemStack notInserted = ItemUtil.insertItemReturnRemaining(handler, container, false, null);
              if (!notInserted.isEmpty()) {
                Level world = getLevel();
                double x = (world.getRandom().nextFloat() * 0.5F) + 0.25D;
                double y = (world.getRandom().nextFloat() * 0.5F) + 0.25D;
                double z = (world.getRandom().nextFloat() * 0.5F) + 0.25D;
                ItemEntity itementity = new ItemEntity(world, fuelPos.getX() + x, fuelPos.getY() + y, fuelPos.getZ() + z, notInserted);
                itementity.setDefaultPickUpDelay();
                world.addFreshEntity(itementity);
              }
            }
          } else {
            TConstruct.LOG.error("Invalid item removed from solid fuel handler");
          }
        }
        return solid.getTemperature();
      }
    }
    return 0;
  }

  @Override
  public int findFuel(boolean consume) {
    // prioritize liquid fuel - it usually goes hotter
    int temperature = 0;
    ResourceHandler<FluidResource> fluidHandler = getFluidHandler();
    if (fluidHandler != null) {
      temperature = tryLiquidFuel(fluidHandler, consume);
    }
    // next, try solid fuel
    if (temperature == 0) {
      ResourceHandler<ItemResource> itemHandler = getItemHandler();
      if (itemHandler != null) {
        temperature = trySolidFuel(itemHandler, consume);
      }
    }
    // no handler found, tell client of the lack of fuel
    if (temperature == 0 && consume) {
      this.temperature = 0;
      this.rate = 0;
    }
    return temperature;
  }


  /* UI Syncing */

  @Override
  public FuelInfo getFuelInfo() {
    // resolve the fluid handler so the base implementation has it available (re-queried each call so it survives reload)
    getFluidHandler();

    FuelInfo info = super.getFuelInfo();
    if (info.isEmpty() && getItemHandler() != null) {
      return FuelInfo.ITEM;
    }
    return info;
  }


  /* Fluid handler */

  /** Gets the fluid handler for proxy */
  public ResourceHandler<FluidResource> getTank() {
    ResourceHandler<FluidResource> fluidHandler = getFluidHandler();
    if (fluidHandler != null) {
      return fluidHandler;
    }
    return EmptyResourceHandler.instance();
  }
}
