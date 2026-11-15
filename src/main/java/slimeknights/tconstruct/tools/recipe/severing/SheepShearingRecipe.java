package slimeknights.tconstruct.tools.recipe.severing;

import net.minecraft.world.entity.EntityTypes;
import com.google.common.collect.Maps;
import net.minecraft.util.Util;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.Map;

public class SheepShearingRecipe extends SeveringRecipe {
  public static final Map<DyeColor,ItemLike> WOOL_BY_COLOR = Util.make(Maps.newEnumMap(DyeColor.class), map -> {
    map.put(DyeColor.WHITE, Blocks.WOOL.white());
    map.put(DyeColor.ORANGE, Blocks.WOOL.orange());
    map.put(DyeColor.MAGENTA, Blocks.WOOL.magenta());
    map.put(DyeColor.LIGHT_BLUE, Blocks.WOOL.lightBlue());
    map.put(DyeColor.YELLOW, Blocks.WOOL.yellow());
    map.put(DyeColor.LIME, Blocks.WOOL.lime());
    map.put(DyeColor.PINK, Blocks.WOOL.pink());
    map.put(DyeColor.GRAY, Blocks.WOOL.gray());
    map.put(DyeColor.LIGHT_GRAY, Blocks.WOOL.lightGray());
    map.put(DyeColor.CYAN, Blocks.WOOL.cyan());
    map.put(DyeColor.PURPLE, Blocks.WOOL.purple());
    map.put(DyeColor.BLUE, Blocks.WOOL.blue());
    map.put(DyeColor.BROWN, Blocks.WOOL.brown());
    map.put(DyeColor.GREEN, Blocks.WOOL.green());
    map.put(DyeColor.RED, Blocks.WOOL.red());
    map.put(DyeColor.BLACK, Blocks.WOOL.black());
  });
  public static final RecordLoadable<SheepShearingRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), BASE_CHANCE_FIELD, LOOTING_BONUS_FIELD, SheepShearingRecipe::new);

  public SheepShearingRecipe(Identifier id, float baseChance, float lootingBonus) {
    super(id, EntityIngredient.of(EntityTypes.SHEEP), ItemOutput.fromItem(Blocks.WOOL.white(), 2), baseChance, lootingBonus);
  }

  @Override
  public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
    return TinkerModifiers.sheepShearing.get();
  }

  @Override
  public ItemStack getOutput(Entity entity) {
    if (entity instanceof Sheep sheep) {
      if (!sheep.isSheared() && !sheep.isBaby()) {
        return new ItemStack(WOOL_BY_COLOR.get(sheep.getColor()), 2);
      }
    }
    return ItemStack.EMPTY;
  }
}
