package slimeknights.tconstruct.fluids;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterCauldronInteractionEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.item.ConstantFluidContainerWrapper;
import slimeknights.mantle.item.ContainerFoodItem.FluidContainerFoodItem;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.item.MagmaBottleItem;
import slimeknights.tconstruct.fluids.item.PotionBucketItem;
import slimeknights.tconstruct.fluids.util.EmptyBottleIntoEmpty;
import slimeknights.tconstruct.fluids.util.EmptyBottleIntoWater;
import slimeknights.tconstruct.fluids.util.FillBottle;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.item.CopperCanFluidHandler;

/**
 * Event subscriber for fluid related events.
 * <p>
 * The blazing blood bucket furnace fuel moved to the {@code cooking_fuel} component on the bucket (see {@link TinkerFluids#blazingBlood}),
 * as NeoForge 26.3 removed the furnace fuel event.
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = TConstruct.MOD_ID)
public class FluidEvents {
  /**
   * Registers the cauldron interactions for splash and lingering bottles. Fired on the mod bus after the items are registered.
   * <p>
   * The vanilla dispatchers no longer expose the interaction registered for an item (only a lookup by stack, and stacks cannot be created yet),
   * so the potions fall back to the default interaction, which is what vanilla registers for splash and lingering potions.
   */
  @SubscribeEvent
  static void registerCauldronInteractions(RegisterCauldronInteractionEvent.Interaction event) {
    CauldronInteractions.WATER.put(TinkerFluids.splashBottle.get(), new FillBottle(Items.SPLASH_POTION));
    CauldronInteractions.WATER.put(TinkerFluids.lingeringBottle.get(), new FillBottle(Items.LINGERING_POTION));
    CauldronInteractions.WATER.put(Items.SPLASH_POTION,    new EmptyBottleIntoWater(TinkerFluids.splashBottle,    CauldronInteraction.DEFAULT));
    CauldronInteractions.WATER.put(Items.LINGERING_POTION, new EmptyBottleIntoWater(TinkerFluids.lingeringBottle, CauldronInteraction.DEFAULT));
    CauldronInteractions.EMPTY.put(Items.SPLASH_POTION,    new EmptyBottleIntoEmpty(TinkerFluids.splashBottle,    CauldronInteraction.DEFAULT));
    CauldronInteractions.EMPTY.put(Items.LINGERING_POTION, new EmptyBottleIntoEmpty(TinkerFluids.lingeringBottle, CauldronInteraction.DEFAULT));
  }

  /**
   * Mod-bus subscriber registering the item fluid handler capabilities.
   * <p>
   * Item capabilities are registered once per item in {@link RegisterCapabilitiesEvent} using {@code Capabilities.Fluid.ITEM}, whose context is the {@link net.neoforged.neoforge.transfer.access.ItemAccess}.
   */
  @EventBusSubscriber(modid = TConstruct.MOD_ID)
  public static class ModBusEvents {
    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
      // fluid food containers (bottles of slime/venom)
      event.registerItem(Capabilities.Fluid.ITEM,
        (stack, access) -> ((FluidContainerFoodItem)stack.getItem()).getFluidHandler(access), TinkerFluids.venomBottle.get());
      for (Item item : TinkerFluids.slimeBottle.values()) {
        event.registerItem(Capabilities.Fluid.ITEM,
          (stack, access) -> ((FluidContainerFoodItem)stack.getItem()).getFluidHandler(access), item);
      }
      // magma bottle
      event.registerItem(Capabilities.Fluid.ITEM,
        (stack, access) -> ((MagmaBottleItem)stack.getItem()).getFluidHandler(access), TinkerFluids.magmaBottle.get());
      // potion bucket
      event.registerItem(Capabilities.Fluid.ITEM,
        (stack, access) -> ((PotionBucketItem)stack.getItem()).getFluidHandler(access), TinkerFluids.potion.asItem());
      // vanilla powder snow bucket gets a constant powdered snow fluid handler
      event.registerItem(Capabilities.Fluid.ITEM,
        (stack, access) -> new ConstantFluidContainerWrapper(new FluidStack(TinkerFluids.powderedSnow.get(), FluidType.BUCKET_VOLUME), access, Items.BUCKET.getDefaultInstance()),
        Items.POWDER_SNOW_BUCKET);
      // copper can, holds a single ingot of fluid
      event.registerItem(Capabilities.Fluid.ITEM,
        (stack, access) -> new CopperCanFluidHandler(access), TinkerSmeltery.copperCan.get());
    }
  }
}
