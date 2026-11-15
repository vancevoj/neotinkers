package slimeknights.tconstruct.world.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import slimeknights.mantle.recipe.data.ICommonRecipeHelper;
import slimeknights.tconstruct.common.data.BaseRecipeProvider;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.TinkerWorld;

public class WorldRecipeProvider extends BaseRecipeProvider implements ICommonRecipeHelper {
  public WorldRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
    super(recipes, advancements);
  }

  @Override
  public String getName() {
    return "Neo Tinkers World Recipes";
  }

  @Override
  protected void buildRecipes(RecipeOutput consumer) {
    // Add recipe for all slimeball <-> congealed and slimeblock <-> slimeball
    // only earth slime recipe we need here slime
    shaped(RecipeCategory.BUILDING_BLOCKS, TinkerWorld.congealedSlime.get(SlimeType.EARTH))
                       .define('#', SlimeType.EARTH.getSlimeballTag())
                       .pattern("##")
                       .pattern("##")
                       .unlockedBy("has_item", has(SlimeType.EARTH.getSlimeballTag()))
                       .group("tconstruct:congealed_slime")
                       .save(consumer, recipeKey(location("common/slime/earth/congealed")));

    // does not need green as its the fallback
    for (SlimeType slimeType : SlimeType.TINKER) {
      Identifier name = location("common/slime/" + slimeType.getSerializedName() + "/congealed");
      shaped(RecipeCategory.BUILDING_BLOCKS, TinkerWorld.congealedSlime.get(slimeType))
                         .define('#', slimeType.getSlimeballTag())
                         .pattern("##")
                         .pattern("##")
                         .unlockedBy("has_item", has(slimeType.getSlimeballTag()))
                         .group("tconstruct:congealed_slime")
                         .save(consumer, recipeKey(name));
      Identifier blockName = location("common/slime/" + slimeType.getSerializedName() + "/slimeblock");
      shaped(RecipeCategory.REDSTONE, TinkerWorld.slime.get(slimeType))
                         .define('#', slimeType.getSlimeballTag())
                         .pattern("###")
                         .pattern("###")
                         .pattern("###")
                         .unlockedBy("has_item", has(slimeType.getSlimeballTag()))
                         .group("slime_blocks")
                         .save(consumer, recipeKey(blockName));
      // green already can craft into slime balls
      shapeless(RecipeCategory.MISC, TinkerCommons.slimeball.get(slimeType), 9)
                            .requires(TinkerWorld.slime.get(slimeType))
                            .unlockedBy("has_item", has(TinkerWorld.slime.get(slimeType)))
                            .group("tconstruct:slime_balls")
                            .save(consumer, "tconstruct:common/slime/" + slimeType.getSerializedName() + "/slimeball_from_block");
    }
    // all types of congealed need a recipe to a block
    for (SlimeType slimeType : SlimeType.values()) {
      shapeless(RecipeCategory.MISC, TinkerCommons.slimeball.get(slimeType), 4)
                            .requires(TinkerWorld.congealedSlime.get(slimeType))
                            .unlockedBy("has_item", has(TinkerWorld.congealedSlime.get(slimeType)))
                            .group("tconstruct:slime_balls")
                            .save(consumer, "tconstruct:common/slime/" + slimeType.getSerializedName() + "/slimeball_from_congealed");
    }

    // craft other slime based items, forge does not automatically add recipes using the tag anymore
    RecipeOutput slimeConsumer = withCondition(consumer, ConfigEnabledCondition.SLIME_RECIPE_FIX);
    shaped(RecipeCategory.REDSTONE, Blocks.STICKY_PISTON)
                       .pattern("#")
                       .pattern("P")
                       .define('#', Tags.Items.SLIME_BALLS)
                       .define('P', Blocks.PISTON)
                       .unlockedBy("has_slime_ball", has(Tags.Items.SLIME_BALLS))
                       .save(slimeConsumer, recipeKey(location("common/slime/sticky_piston")));
    shaped(RecipeCategory.TOOLS, Items.LEAD, 2)
                       .define('~', Items.STRING)
                       .define('O', Tags.Items.SLIME_BALLS)
                       .pattern("~~ ")
                       .pattern("~O ")
                       .pattern("  ~")
                       .unlockedBy("has_slime_ball", has(Tags.Items.SLIME_BALLS))
                       .save(slimeConsumer, recipeKey(location("common/slime/lead")));

    // wood
    String woodFolder = "world/wood/";
    woodCrafting(consumer, TinkerWorld.greenheart, woodFolder + "greenheart/");
    woodCrafting(consumer, TinkerWorld.skyroot, woodFolder + "skyroot/");
    woodCrafting(consumer, TinkerWorld.bloodshroom, woodFolder + "bloodshroom/");
    woodCrafting(consumer, TinkerWorld.enderbark, woodFolder + "enderbark/");

    // geodes
    geodeRecipes(consumer, TinkerWorld.earthGeode, SlimeType.EARTH, "common/slime/earth/");
    geodeRecipes(consumer, TinkerWorld.skyGeode,   SlimeType.SKY,   "common/slime/sky/");
    geodeRecipes(consumer, TinkerWorld.ichorGeode, SlimeType.ICHOR, "common/slime/ichor/");
    geodeRecipes(consumer, TinkerWorld.enderGeode, SlimeType.ENDER, "common/slime/ender/");
  }

  private void geodeRecipes(RecipeOutput consumer, GeodeItemObject geode, SlimeType slime, String folder) {
    shaped(RecipeCategory.BUILDING_BLOCKS, geode.getBlock())
                       .define('#', geode.asItem())
                       .pattern("##")
                       .pattern("##")
                       .unlockedBy("has_item", has(geode.asItem()))
                       .group("tconstruct:slime_crystal_block")
                       .save(consumer, recipeKey(location(folder + "crystal_block")));
    SimpleCookingRecipeBuilder.blasting(Ingredient.of(geode), RecipeCategory.MISC, CookingBookCategory.MISC, TinkerCommons.slimeball.get(slime), 0.2f, 200)
                              .unlockedBy("has_crystal", has(geode))
                              .group("tconstruct:slime_crystal")
                              .save(consumer, recipeKey(location(folder + "crystal_smelting")));
    ItemLike dirt = TinkerWorld.slimeDirt.get(slime.asDirt());
    SimpleCookingRecipeBuilder.blasting(Ingredient.of(dirt), RecipeCategory.MISC, CookingBookCategory.MISC, geode, 0.2f, 400)
                              .unlockedBy("has_dirt", has(dirt))
                              .group("tconstruct:slime_dirt")
                              .save(consumer, recipeKey(location(folder + "crystal_growing")));
  }
}
