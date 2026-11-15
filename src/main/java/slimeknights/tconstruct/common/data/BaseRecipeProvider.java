package slimeknights.tconstruct.common.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NeoForgeConditions;
import slimeknights.mantle.recipe.data.IRecipeHelper;
import slimeknights.tconstruct.TConstruct;

/**
 * Shared logic for each module's recipe provider.
 * Since 26.3 recipes are a reloadable registry, so providers are bootstraps registered with
 * {@code GatherDataRegistryEntriesEvent#recipe(Provider::new)} instead of data providers.
 */
public abstract class BaseRecipeProvider extends RecipeProvider implements IRecipeHelper {
  public BaseRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
    super(recipes, advancements, TConstruct.MOD_ID);
    TConstruct.sealTinkersClass(this, "BaseRecipeProvider", "BaseRecipeProvider is trivial to recreate and directly extending can lead to addon recipes polluting our namespace.");
  }

  @Override
  protected final void buildRecipes() {
    buildRecipes(this.output);
  }

  /** Builds all recipes for this provider */
  protected abstract void buildRecipes(RecipeOutput consumer);

  /** Gets the name of this provider, used for logging */
  public abstract String getName();

  @Override
  public String getModId() {
    return TConstruct.MOD_ID;
  }

  /** Creates a recipe key from the given ID, for saving vanilla recipe builders which no longer accept IDs */
  protected static ResourceKey<Recipe<?>> recipeKey(Identifier id) {
    return ResourceKey.create(Registries.RECIPE, id);
  }


  /* Condition helpers, replacing the methods from NeoForge's removed IConditionBuilder */

  /** Condition requiring all the given conditions */
  public ICondition and(ICondition... values) {
    return NeoForgeConditions.and(values);
  }

  /** Condition that never passes */
  @SuppressWarnings("MethodName")  // matches the old IConditionBuilder name
  public ICondition FALSE() {
    return NeoForgeConditions.never();
  }

  /** Condition that always passes */
  @SuppressWarnings("MethodName")  // matches the old IConditionBuilder name
  public ICondition TRUE() {
    return NeoForgeConditions.always();
  }

  /** Condition inverting the given condition */
  public ICondition not(ICondition value) {
    return NeoForgeConditions.not(value);
  }

  /** Condition requiring any of the given conditions */
  public ICondition or(ICondition... values) {
    return NeoForgeConditions.or(values);
  }

  /** Condition requiring the given item to exist */
  public ICondition itemExists(String namespace, String path) {
    return NeoForgeConditions.itemRegistered(namespace, path);
  }

  /** Condition requiring the given mod */
  public ICondition modLoaded(String modid) {
    return NeoForgeConditions.modLoaded(modid);
  }

  /** Condition requiring the given tag to be empty */
  public ICondition tagEmpty(TagKey<Item> tag) {
    return NeoForgeConditions.tagEmpty(tag);
  }
}
