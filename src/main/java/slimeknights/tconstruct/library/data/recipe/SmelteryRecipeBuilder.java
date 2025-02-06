package slimeknights.tconstruct.library.data.recipe;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.DifferenceIngredient;
import net.minecraftforge.common.crafting.IntersectionIngredient;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import net.minecraftforge.common.crafting.conditions.TrueCondition;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.recipe.condition.TagCombinationCondition;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.mantle.recipe.data.ConsumerWrapperBuilder;
import slimeknights.mantle.recipe.data.ItemNameIngredient;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipeBuilder;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeBuilder;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static slimeknights.mantle.Mantle.commonResource;
import static slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe.getTemperature;

/** Helper for building melting and casting recipes for a fluid */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(fluent = true)
@CanIgnoreReturnValue
public class SmelteryRecipeBuilder {
  /** Consumer for recipe results */
  private final Consumer<FinishedRecipe> consumer;
  /** Resource name, domain is location for results and name is tag root */
  private final ResourceLocation name;
  /** Fluid object to generate results and ingredients, takes top priority */
  @Nullable
  private final FluidObject<?> fluidObject;
  /** Fluid to generate results and ingredients. If set along tag, tag is priorized for ingredients. */
  @Nullable
  private final Fluid fluid;
  /** Fluid tag to generate results and ingredients. If set along fluid, fluid is prioritized for results */
  @Nullable
  private final TagKey<Fluid> fluidTag;
  /** Temperature for recipes */
  @Setter
  private int temperature;
  /** If true, all recipes are optional. If false only unsupported types are optional */
  @Setter
  private boolean optional = false;
  /** If true, ore recipes should be added */
  private boolean hasOre = false;
  /** List of byproducts for these recipes */
  private IByproduct[] byproducts = new IByproduct[0];
  /** Folder to save melting recipes */
  private String meltingFolder = "melting/";
  /** Folder to save casting recipes */
  private String castingFolder = "casting/";
  /** Keeps track of whether this builder is used for gems or metals */
  private OreRateType oreRate = null;
  /** Base unit value for builder */
  private int baseUnit = 0;
  /** Base unit value for builder */
  private int damageUnit = 0;

  // mod compat
  private boolean mekanismTools = false;
  private boolean toolsComplement = false;

  /* Constructors */

  /** Creates a builder for the given fluid object */
  @CheckReturnValue
  public static SmelteryRecipeBuilder fluid(Consumer<FinishedRecipe> consumer, ResourceLocation name, FluidObject<?> fluid) {
    return new SmelteryRecipeBuilder(consumer, name, fluid, null, null).temperature(getTemperature(fluid));
  }

  /** Creates a builder for the given fluid and tags. Tag will be used for inputs and fluid for outputs */
  @CheckReturnValue
  public static SmelteryRecipeBuilder fluid(Consumer<FinishedRecipe> consumer, ResourceLocation name, @Nullable Fluid fluid, @Nullable TagKey<Fluid> fluidTag) {
    assert fluid != null || fluidTag != null;
    SmelteryRecipeBuilder builder = new SmelteryRecipeBuilder(consumer, name, null, fluid, fluidTag);
    if (fluid != null) {
      builder.temperature(getTemperature(fluid));
    }
    return builder;
  }

  /** Creates a builder for the given fluid, used as input and output */
  @CheckReturnValue
  public static SmelteryRecipeBuilder fluid(Consumer<FinishedRecipe> consumer, ResourceLocation name, Fluid fluid) {
    return fluid(consumer, name, fluid, null);
  }

  /** Creates a builder for the given fluid tags, used as input and output */
  @CheckReturnValue
  public static SmelteryRecipeBuilder fluid(Consumer<FinishedRecipe> consumer, ResourceLocation name, TagKey<Fluid> fluidTag) {
    return fluid(consumer, name, null, fluidTag);
  }


  /* Setters */

  /** Sets all recipes to optional, used for compat */
  public SmelteryRecipeBuilder optional() {
    return optional(true);
  }

  /** Sets the byproducts for following recipes */
  public SmelteryRecipeBuilder ore(IByproduct... byproducts) {
    this.byproducts = byproducts;
    this.hasOre = true;
    return this;
  }

  /** Sets the output prefix for melting recipes */
  public SmelteryRecipeBuilder meltingFolder(String meltingFolder) {
    this.meltingFolder = meltingFolder + '/';
    return this;
  }

  /** Sets the output prefix for casting recipes */
  public SmelteryRecipeBuilder castingFolder(String castingFolder) {
    this.castingFolder = castingFolder + '/';
    return this;
  }


  /* Basic helpers */

  /** Creates a recipe result for melting */
  @CheckReturnValue
  private FluidOutput result(int amount) {
    if (fluidObject != null) {
      return fluidObject.result(amount);
    }
    if (fluid != null) {
      return FluidOutput.fromFluid(fluid, amount);
    }
    assert fluidTag != null;
    return FluidOutput.fromTag(fluidTag, amount);
  }

  /** Creates an ingredient input for casting */
  @CheckReturnValue
  private FluidIngredient ingredient(int amount) {
    if (fluidObject != null) {
      return fluidObject.ingredient(amount);
    }
    if (fluidTag != null) {
      return FluidIngredient.of(fluidTag, amount);
    }
    assert fluid != null;
    return FluidIngredient.of(fluid, amount);
  }

  /** Adds the given conditions to the given builder */
  @CheckReturnValue
  public Consumer<FinishedRecipe> withCondition(ICondition... conditions) {
    ConsumerWrapperBuilder builder = ConsumerWrapperBuilder.wrap();
    for (ICondition condition : conditions) {
      builder.addCondition(condition);
    }
    return builder.build(consumer);
  }

  /** Creates a condition for a tag being empty */
  @CheckReturnValue
  public static ICondition tagCondition(ResourceLocation tag) {
    return new TagFilledCondition<>(ItemTags.create(tag));
  }

  /** Creates a condition for a tag being empty */
  @CheckReturnValue
  public static ICondition tagCondition(String name) {
    return tagCondition(commonResource(name));
  }

  /** Creates a tag key for an item */
  @CheckReturnValue
  public static TagKey<Item> itemTag(String name) {
    return ItemTags.create(commonResource(name));
  }

  /** Creates a location under the given domain with the passed prefix  */
  @CheckReturnValue
  private ResourceLocation location(String folder, String variant) {
    return name.withPath(folder + name.getPath() + '/' + variant);
  }

  /** Fetchs an item from the registry */
  @SuppressWarnings("deprecation")
  private static Item item(String name) {
    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(name));
    if (item == Items.AIR) {
      throw new IllegalArgumentException("Unknown item name minecraft:" + name);
    }
    return item;
  }


  /* Melting helpers */

  /** Adds a recipe for melting a list of items. Never optional */
  private void itemMelting(int amount, String output, float factor, int damageUnit, Item... items) {
    MeltingRecipeBuilder.melting(Ingredient.of(items), result(amount), temperature, factor)
                        .setDamagable(damageUnit)
                        .save(consumer, location(meltingFolder, output));
  }

  /** Adds a recipe for melting a list of items. Never optional */
  private void itemMelting(int amount, String output, float factor, int damageUnit, String name) {
    itemMelting(amount, output, factor, damageUnit, item(name));
  }

  /** Adds a recipe for melting an item by ID. Automatically optional */
  private void itemMelting(int amount, String output, float factor, ResourceLocation itemName, int damageUnit) {
    MeltingRecipeBuilder.melting(ItemNameIngredient.from(itemName), result(amount), temperature, factor)
                        .setDamagable(damageUnit)
                        .save(withCondition(new ItemExistsCondition(itemName)), location(meltingFolder, output));
  }

  /** Adds a recipe for melting an item from a tag */
  private void tagMelting(int amount, String output, float factor, String tagName, boolean forceOptional) {
    tagMelting(amount, output, factor, commonResource(tagName), 0, forceOptional);
  }

  /** Adds a recipe for melting an item from a tag */
  private void tagMelting(int amount, String output, float factor, ResourceLocation tagName, int damageUnit, boolean forceOptional) {
    Consumer<FinishedRecipe> wrapped = optional || forceOptional ? withCondition(tagCondition(tagName)) : consumer;
    MeltingRecipeBuilder.melting(Ingredient.of(ItemTags.create(tagName)), result(amount), temperature, factor)
                        .setDamagable(damageUnit)
                        .save(wrapped, location(meltingFolder, output));
  }

  /** Adds recipes to melt an ore item with byproducts */
  private void oreMelting(float scale, String tagName, @Nullable TagKey<Item> size, float factor, String output, boolean forceOptional) {
    assert oreRate != null;
    assert baseUnit != 0;
    Consumer<FinishedRecipe> wrapped;
    Ingredient baseIngredient = Ingredient.of(itemTag(tagName));
    Ingredient ingredient;
    // not everyone sets size, so treat singular as the fallback, means we want anything in the tag that is not sparse or dense
    if (size == Tags.Items.ORE_RATES_SINGULAR) {
      ingredient = DifferenceIngredient.of(baseIngredient, Ingredient.of(TinkerTags.Items.NON_SINGULAR_ORE_RATES));
      wrapped = withCondition(TagCombinationCondition.difference(itemTag(tagName), TinkerTags.Items.NON_SINGULAR_ORE_RATES));
      // size tag means we want an intersection between the tag and that size
    } else if (size != null) {
      ingredient = IntersectionIngredient.of(baseIngredient, Ingredient.of(size));
      wrapped = withCondition(TagCombinationCondition.intersection(itemTag(tagName), size));
      // default only need it to be in the tag
    } else {
      ingredient = baseIngredient;
      wrapped = optional || forceOptional ? withCondition(tagCondition(tagName)) : consumer;
    }
    Supplier<MeltingRecipeBuilder> supplier = () -> MeltingRecipeBuilder.melting(ingredient, result((int)(baseUnit * scale)), temperature, factor).setOre(oreRate);
    ResourceLocation location = location(meltingFolder, output);

    // if no byproducts, just build directly
    if (byproducts.length == 0) {
      supplier.get().save(wrapped, location);
      // if first option is always present, only need that one
    } else if (byproducts[0].isAlwaysPresent()) {
      supplier.get()
              .addByproduct(byproducts[0].getFluid(scale))
              .setOre(oreRate, byproducts[0].getOreRate())
              .save(wrapped, location);
    } else {
      // multiple options, will need a conditonal recipe
      ConditionalRecipe.Builder builder = ConditionalRecipe.builder();
      boolean alwaysPresent = false;
      for (IByproduct byproduct : byproducts) {
        // found an always present byproduct? no need to tag and we are done
        alwaysPresent = byproduct.isAlwaysPresent();
        if (alwaysPresent) {
          builder.addCondition(TrueCondition.INSTANCE);
        } else {
          builder.addCondition(tagCondition("ingots/" + byproduct.getName()));
        }
        builder.addRecipe(supplier.get().addByproduct(byproduct.getFluid(scale)).setOre(oreRate, byproduct.getOreRate())::save);

        if (alwaysPresent) {
          break;
        }
      }
      // not always present? add a recipe with no byproducts as a final fallback
      if (!alwaysPresent) {
        builder.addCondition(TrueCondition.INSTANCE);
        builder.addRecipe(supplier.get()::save);
      }
      builder.build(wrapped, location);
    }
  }


  /* Casting helpers */

  /** Recipe to cast using a cast */
  private void tagCasting(int amount, String outputPrefix, CastItemObject cast, String tagName, boolean forceOptional) {
    Consumer<FinishedRecipe> wrapped = optional || forceOptional ? withCondition(tagCondition(tagName)) : consumer;
    ItemOutput output = ItemOutput.fromTag(itemTag(tagName));
    FluidIngredient fluid = ingredient(amount);
    ItemCastingRecipeBuilder.tableRecipe(output)
                            .setFluid(fluid)
                            .setCoolingTime(temperature, amount)
                            .setCast(cast.getMultiUseTag(), false)
                            .save(wrapped, location(castingFolder, outputPrefix + "_gold_cast"));
    ItemCastingRecipeBuilder.tableRecipe(output)
                            .setFluid(fluid)
                            .setCoolingTime(temperature, amount)
                            .setCast(cast.getSingleUseTag(), true)
                            .save(wrapped, location(castingFolder, outputPrefix + "_sand_cast"));
  }

  /** Recipe to cast using a basin */
  private void basinCasting(int amount, String output, String tagName, boolean forceOptional) {
    Consumer<FinishedRecipe> wrapped = optional || forceOptional ? withCondition(tagCondition(tagName)) : consumer;
    ItemCastingRecipeBuilder.basinRecipe(ItemOutput.fromTag(itemTag(tagName)))
                            .setFluid(ingredient(amount))
                            .setCoolingTime(temperature, amount)
                            .save(wrapped, location(castingFolder, output));
  }


  /* Joint helpers */

  /** Adds melting and casting recipes for the given object */
  public SmelteryRecipeBuilder meltingCasting(float scale, String tagPrefix, CastItemObject cast, float factor, boolean forceOptional) {
    assert baseUnit != 0;
    int amount = (int)(baseUnit * scale);
    String tagName = tagPrefix + "s/" + name.getPath();
    tagMelting(amount, tagPrefix, factor, tagName, forceOptional);
    tagCasting(amount, tagPrefix, cast,   tagName, forceOptional);
    return this;
  }

  /** Adds melting and casting recipes for the given object */
  public SmelteryRecipeBuilder meltingCasting(float scale, CastItemObject cast, float factor, boolean forceOptional) {
    return meltingCasting(scale, cast.getName().getPath(), cast, factor, forceOptional);
  }

  /** Adds a recipe melting a tag item */
  public SmelteryRecipeBuilder melting(float scale, String output, ResourceLocation tagName, float factor, int damageUnit, boolean forceOptional) {
    assert baseUnit != 0;
    tagMelting((int)(baseUnit * scale), output, factor, tagName, damageUnit, forceOptional);
    return this;
  }

  /** Adds a recipe melting a tag item */
  public SmelteryRecipeBuilder melting(float scale, String output, ResourceLocation tagName, int damageUnit, boolean forceOptional) {
    return melting(scale, output, tagName, (float)Math.sqrt(scale), damageUnit, forceOptional);
  }

  /** Adds a recipe melting a tag item */
  public SmelteryRecipeBuilder melting(float scale, String output, String tagPrefix, float factor, int damageUnit, boolean forceOptional) {
    return melting(scale, output, commonResource(tagPrefix + '/' + name.getPath()), factor, damageUnit, forceOptional);
  }

  /** Adds a recipe melting a tag item */
  public SmelteryRecipeBuilder melting(float scale, String output, String tagPrefix, int damageUnit, boolean forceOptional) {
    return melting(scale, output, tagPrefix, (float)Math.sqrt(scale), damageUnit, forceOptional);
  }

  /** Adds a recipe melting a tag item */
  public SmelteryRecipeBuilder melting(float scale, String tagPrefix, float factor, boolean forceOptional) {
    return melting(scale, tagPrefix, tagPrefix + "s", factor, 0, forceOptional);
  }


  /* Main recipes */

  /**
   * Adds the raw ore and raw ore block metal melting recipes.
   * This is automatically called by {@link #metal()} if {@link #hasOre}, which is set automatically by {@link #ore(IByproduct...)}.
   * Provided for non-standard ores (like gold).
   */
  public SmelteryRecipeBuilder rawOre() {
    assert oreRate != null;
    assert baseUnit != 0;
    String name = this.name.getPath();
    oreMelting(1, "raw_materials/" + name,      null, 1.5f, "raw",       false);
    oreMelting(9, "storage_blocks/raw_" + name, null, 6.0f, "raw_block", false);
    return this;
  }

  /** Adds the sparse ore recipe at the given scale. Automatcally called by {@link #metal()} and {@link #gem(int)}, so only needed if doing unusual things. */
  public SmelteryRecipeBuilder sparseOre(float scale) {
    oreMelting(scale, "ores/" + name.getPath(), Tags.Items.ORE_RATES_SPARSE, 1.5f, "ore_sparse", false);
    return this;
  }

  /** Adds the sparse ore recipe at the given scale. Automatcally called by {@link #metal()} and {@link #gem(int)}, so only needed if doing unusual things. */
  public SmelteryRecipeBuilder singularOre(float scale) {
    oreMelting(scale, "ores/" + name.getPath(), Tags.Items.ORE_RATES_SINGULAR, 2.5f, "ore_singular", false);
    return this;
  }

  /** Adds the sparse ore recipe at the given scale. Automatcally called by {@link #metal()} and {@link #gem(int)}, so only needed if doing unusual things. */
  public SmelteryRecipeBuilder denseOre(float scale) {
    oreMelting(scale, "ores/" + name.getPath(), Tags.Items.ORE_RATES_DENSE, 4.5f, "ore_dense", false);
    return this;
  }

  /** Adds standard metal recipes for melting ingots, nuggets, and blocks */
  public SmelteryRecipeBuilder metal() {
    oreRate = OreRateType.METAL;
    baseUnit = FluidValues.INGOT;
    damageUnit = FluidValues.NUGGET;
    String name = this.name.getPath();
    tagMelting(FluidValues.METAL_BLOCK, "block", 3.0f, "storage_blocks/" + name, false);
    basinCasting(FluidValues.METAL_BLOCK, "block", "storage_blocks/" + name, false);
    meltingCasting(1,      TinkerSmeltery.ingotCast,  1.0f, false);
    meltingCasting(1 / 9f, TinkerSmeltery.nuggetCast, 1 / 3f, false);
    // if we set byproducts, we are an ore
    if (hasOre) {
      rawOre();
      sparseOre(1);
      singularOre(2);
      denseOre(6);
    }
    return this;
  }

  /** Adds basic recipes for gems */
  public SmelteryRecipeBuilder gem(int storageSize) {
    oreRate = OreRateType.GEM;
    baseUnit = FluidValues.GEM;
    damageUnit = FluidValues.GEM_SHARD;
    String name = this.name.getPath();
    tagMelting(FluidValues.GEM * storageSize, "block", (float)Math.sqrt(storageSize), "storage_blocks/" + name, false);
    basinCasting(FluidValues.GEM * storageSize, "block", "storage_blocks/" + name, false);
    meltingCasting(1, TinkerSmeltery.gemCast, 1.0f, false);
    // if we set byproducts, we are an ore
    if (hasOre) {
      sparseOre(0.5f);
      singularOre(1);
      denseOre(3);
    }
    return this;
  }

  /** Adds basic recipes for a amethyst/quartz style gem */
  public SmelteryRecipeBuilder smallGem() {
    return gem(4);
  }

  /** Adds basic recipes for a diamond/emerald style gem */
  public SmelteryRecipeBuilder largeGem() {
    return gem(9);
  }

  /** Adds geore recipes. Will add either metal or gem based on the ore rate set. */
  public SmelteryRecipeBuilder geore() {
    assert oreRate != null;
    assert baseUnit != 0;
    String name = this.name.getPath();
    // base - no byproducts
    tagMelting(baseUnit, "geore/shard", 1.0f, "geore_shards/" + name, true);
    tagMelting(baseUnit * 4, "geore/block", 2.0f, "geore_blocks/" + name, true);
    // clusters - ores with byproducts
    oreMelting(4, "geore_clusters/" + name,    null, 2.5f, "geore/cluster",    true);
    oreMelting(1, "geore_small_buds/" + name,  null, 1.0f, "geore/bud_small",  true);
    oreMelting(2, "geore_medium_buds/" + name, null, 1.5f, "geore/bud_medium", true);
    oreMelting(3, "geore_large_buds/" + name,  null, 2.0f, "geore/bud_large",  true);
    return this;
  }

  /** Adds recipes to melt oreberries */
  public SmelteryRecipeBuilder oreberry() {
    assert baseUnit == FluidValues.INGOT;
    itemMelting(FluidValues.NUGGET, "oreberry", 1 / 3f, new ResourceLocation("oreberriesreplanted", name.getPath() + "_oreberry"), 0);
    return this;
  }

  /** Adds a recipe for melting dust */
  public SmelteryRecipeBuilder dust() {
    return melting(1, "dust", 0.75f, true);
  }

  /** Adds a recipe for melting plates */
  public SmelteryRecipeBuilder plate() {
    return meltingCasting(1, TinkerSmeltery.plateCast, 1, true);
  }

  /** Adds a recipe for melting gears */
  public SmelteryRecipeBuilder gear() {
    return meltingCasting(4, TinkerSmeltery.gearCast, 2, true);
  }

  /** Adds a recipe for melting rods from IE */
  public SmelteryRecipeBuilder rod() {
    return meltingCasting(0.5f, TinkerSmeltery.rodCast, 1 / 5f, true);
  }

  /** Adds a recipe for melting sheetmetal from IE */
  public SmelteryRecipeBuilder sheetmetal() {
    return melting(1, "sheetmetal", 1, true);
  }

  /** Adds a recipe for melting coins from thermal */
  public SmelteryRecipeBuilder coin() {
    return meltingCasting(1 / 3f, TinkerSmeltery.coinCast, 2/3f, true);
  }

  /** Adds a recipe for melting wires from IE */
  public SmelteryRecipeBuilder wire() {
    return meltingCasting(0.5f, TinkerSmeltery.wireCast, 1 / 5f, true);
  }

  /** Common logic between vanilla and mods */
  private void commonTools(boolean forceOptional) {
    // if tools complement is present, shovel recipe also handles knife
    if (toolsComplement) {
      melting(1, "shovel", name.withPath("melting/" + name.getPath() + "/tools_costing_1"), damageUnit, forceOptional);
    } else {
      melting(1, "shovel", "tools/shovels", damageUnit, forceOptional);
    }
    // sword recipe also handles hoe
    melting(2, "sword", name.withPath("melting/" + name.getPath() + "/tools_costing_2"), damageUnit, true);
    // axe and pickaxe together
    melting(3, "axes",  name.withPath("melting/" + name.getPath() + "/tools_costing_3"), damageUnit, forceOptional);
  }

  /** Adds vanilla tools with the given prefix for item IDs */
  @Internal
  public SmelteryRecipeBuilder minecraftTools(String prefix) {
    // always have tools complement for vanilla
    toolsComplement();
    commonTools(false);
    // armor
    itemMelting(baseUnit * 5, "helmet",     (float)Math.sqrt(5), damageUnit, prefix + "_helmet");
    itemMelting(baseUnit * 8, "chestplate", (float)Math.sqrt(8), damageUnit, prefix + "_chestplate");
    itemMelting(baseUnit * 4, "boots",      (float)Math.sqrt(4), damageUnit, prefix + "_boots");
    // mekanism adds paxels for all vanilla tools, so use a tag to make supporting that easy
    melting(7, "leggings", name.withPath("melting/" + name.getPath() + "/tools_costing_7"), damageUnit, false);
    return this;
  }

  /** Adds vanilla tools with the default prefix for item IDs */
  @Internal
  public SmelteryRecipeBuilder minecraftTools() {
    return minecraftTools(name.getPath());
  }

  /** Adds gear recipes for mekanism gear */
  public SmelteryRecipeBuilder mekanismTools() {
    assert baseUnit != 0;
    mekanismTools = true;
    itemMelting(baseUnit * 6, "shield", (float)Math.sqrt(6), new ResourceLocation("mekanism", name.getPath() + "_shield"), damageUnit);
    return this;
  }

  /** Adds gear recipes for tools complement gear */
  public SmelteryRecipeBuilder toolsComplement() {
    assert baseUnit != 0;
    toolsComplement = true;
    // Knife and sickle are handled via tags
    itemMelting(baseUnit * 11, "excavator", (float)Math.sqrt(11), new ResourceLocation("tools_complement", name.getPath() + "_excavator"), damageUnit);
    itemMelting(baseUnit * 13, "hammer", (float)Math.sqrt(13), new ResourceLocation("tools_complement", name.getPath() + "_hammer"), damageUnit);
    return this;
  }

  /** Adds gear recipes for all standard gear. Call after {@link #toolsComplement()} and {@link #mekanismTools()} */
  public SmelteryRecipeBuilder tools() {
    commonTools(true);
    melting(5, "helmet",     "armors/helmets",     damageUnit, true);
    melting(8, "chestplate", "armors/chestplates", damageUnit, true);
    melting(4, "boots",      "armors/boots",       damageUnit, true);
    // we only fill the 7 cost tag for things with paxels. Anything without
    if (mekanismTools) {
      melting(7, "leggings", name.withPath("melting/" + name.getPath() + "/tools_costing_7"), damageUnit, true);
    } else {
      melting(7, "leggings", "armors/leggings", damageUnit, true);
    }
    return this;
  }
}
