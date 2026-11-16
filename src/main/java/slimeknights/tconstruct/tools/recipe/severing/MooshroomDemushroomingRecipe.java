package slimeknights.tconstruct.tools.recipe.severing;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;

/**
 * Recipe to deshroom a mooshroom, taking brown into account
 */
public class MooshroomDemushroomingRecipe extends SeveringRecipe {
  public static final RecordLoadable<MooshroomDemushroomingRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), BASE_CHANCE_FIELD, LOOTING_BONUS_FIELD, MooshroomDemushroomingRecipe::new);

  public MooshroomDemushroomingRecipe(Identifier id, float baseChance, float lootingBonus) {
    super(id, EntityIngredient.of(EntityTypes.MOOSHROOM), ItemOutput.fromItem(Items.RED_MUSHROOM, 5), baseChance, lootingBonus);
  }

  @Override
  public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
    return TinkerModifiers.mooshroomDemushroomingSerializer.get();
  }

  @Override
  public ItemStack getOutput(Entity entity) {
    if (entity instanceof MushroomCow mooshroom) {
      if (!mooshroom.isBaby()) {
        return new ItemStack(mooshroom.getVariant() == MushroomCow.Variant.BROWN ? Items.BROWN_MUSHROOM : Items.RED_MUSHROOM, 5);
      }
    }
    return ItemStack.EMPTY;
  }
}
