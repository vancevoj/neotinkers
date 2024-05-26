package slimeknights.tconstruct.common.registration;

import net.minecraft.world.item.Item;
import slimeknights.mantle.registration.deferred.ItemDeferredRegister;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.PartCastItem;

import java.util.function.Supplier;

public class ItemDeferredRegisterExtension extends ItemDeferredRegister {
  public ItemDeferredRegisterExtension(String modID) {
    super(modID);
  }

  /**
   * Registers a set of three cast items at once
   * @param name         Base name of cast
   * @param constructor  Item constructor
   * @return  Object containing casts
   */
  public CastItemObject registerCast(String name, Supplier<? extends Item> constructor) {
    ItemObject<Item> cast = register(name + "_cast", constructor);
    ItemObject<Item> sandCast = register(name + "_sand_cast", constructor);
    ItemObject<Item> redSandCast = register(name + "_red_sand_cast", constructor);
    return new CastItemObject(resource(name), cast, sandCast, redSandCast);
  }

  /**
   * Registers a set of three cast items at once
   * @param name   Base name of cast
   * @param props  Item properties
   * @return  Object containing casts
   */
  public CastItemObject registerCast(String name, Item.Properties props) {
    return registerCast(name, () -> new Item(props));
  }

  /**
   * Registers a set of three cast items at once using the part item cast
   * @param item   Part item base for the cast
   * @param props  Item properties
   * @return  Object containing casts
   */
  public CastItemObject registerCast(ItemObject<? extends IMaterialItem> item, Item.Properties props) {
    return registerCast(item.getId().getPath(), () -> new PartCastItem(props, item));
  }
}
