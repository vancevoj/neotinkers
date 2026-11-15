package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/** Ingredient that contains another ingredient nested inside */
public abstract class NestedIngredient implements ICustomIngredient {
  protected final Ingredient nested;

  protected NestedIngredient(Ingredient nested) {
    this.nested = nested;
  }


  /* Defer to nested */

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return nested.test(stack);
  }

  @SuppressWarnings("deprecation")
  @Override
  public Stream<Holder<Item>> items() {
    return nested.items();
  }

  /** Uses the nested display, so components on the nested display stacks are kept */
  @Override
  public SlotDisplay display() {
    return nested.display();
  }

  @Override
  public boolean isSimple() {
    return nested.isSimple();
  }
}
