package slimeknights.tconstruct.common.registration;

import net.minecraft.world.item.Item;
import slimeknights.mantle.registration.deferred.ItemDeferredRegister;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.PartCastItem;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Additional methods in the item deferred register.
 * <p>
 * Like the Mantle register, items need their ID in their properties before construction, so every method takes a constructor from properties
 * and an operator creating the properties. The operator is applied once per registered item, never share a properties instance.
 */
public class ItemDeferredRegisterExtension extends ItemDeferredRegister {
  public ItemDeferredRegisterExtension(String modID) {
    super(modID);
  }

  /**
   * Registers a set of three cast items at once
   * @param name         Base name of cast
   * @param constructor  Item constructor, receives the item properties with the ID set
   * @param props        Operator applied to new item properties, called once per cast
   * @return  Object containing casts
   */
  public CastItemObject registerCast(String name, Function<Item.Properties, ? extends Item> constructor, UnaryOperator<Item.Properties> props) {
    ItemObject<Item> cast = register(name + "_cast", constructor, props);
    ItemObject<Item> sandCast = register(name + "_sand_cast", constructor, props);
    ItemObject<Item> redSandCast = register(name + "_red_sand_cast", constructor, props);
    return new CastItemObject(resource(name), cast, sandCast, redSandCast);
  }

  /**
   * Registers a set of three cast items at once
   * @param name   Base name of cast
   * @param props  Operator applied to new item properties, called once per cast
   * @return  Object containing casts
   */
  public CastItemObject registerCast(String name, UnaryOperator<Item.Properties> props) {
    return registerCast(name, Item::new, props);
  }

  /**
   * Registers a set of three cast items at once using the part item cast
   * @param item   Part item base for the cast
   * @param props  Operator applied to new item properties, called once per cast
   * @return  Object containing casts
   */
  public CastItemObject registerCast(ItemObject<? extends IMaterialItem> item, UnaryOperator<Item.Properties> props) {
    return registerCast(item.getId().getPath(), p -> new PartCastItem(p, item), props);
  }
}
