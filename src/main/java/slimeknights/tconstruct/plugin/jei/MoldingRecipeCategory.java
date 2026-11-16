package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.List;

/** Recipe category for molding casts */
public class MoldingRecipeCategory extends AbstractRecipeCategory<MoldingRecipe> {
  private static final Identifier BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/casting.png");
  private static final Component TITLE = TConstruct.makeTranslation("jei", "molding.title");
  private static final Component TOOLTIP_PATTERN_CONSUMED = Component.translatable(TConstruct.makeTranslationKey("jei", "molding.pattern_consumed"));
  private static final IRecipeSlotRichTooltipCallback PATTERN_CONSUMED_TOOLTIP = (slot, tooltip) -> tooltip.add(TOOLTIP_PATTERN_CONSUMED);

  private final IDrawable table, basin, downArrow, upArrow;
  public MoldingRecipeCategory(IGuiHelper helper) {
    super(TConstructJEIConstants.MOLDING, TITLE, helper.createDrawableItemLike(TinkerSmeltery.blankSandCast), 70, 57);
    this.table = helper.createDrawable(BACKGROUND_LOC, 117, 0, 16, 16);
    this.basin = helper.createDrawable(BACKGROUND_LOC, 117, 16, 16, 16);
    this.downArrow = helper.createDrawable(BACKGROUND_LOC, 70, 55, 6, 6);
    this.upArrow = helper.createDrawable(BACKGROUND_LOC, 76, 55, 6, 6);
  }

  @Override
  public void createRecipeExtras(IRecipeExtrasBuilder builder, MoldingRecipe recipe, IFocusGroup focuses) {
    builder.addRecipeArrowWidget().setPosition(24, 23);
    // the pattern defaults to Mantle's empty ingredient, as vanilla ingredients cannot be empty
    boolean hasPattern = !IngredientLoadable.isEmpty(recipe.getPattern());
    builder.addDrawableWidget(hasPattern ? downArrow : upArrow).setPosition(8, 17);
    IDrawable block = recipe.getType() == TinkerRecipeTypes.MOLDING_BASIN.get() ? basin : table;
    builder.addDrawableWidget(block).setPosition(3, 40);

    // if no mold, we "pickup" the item, so draw no table
    if (hasPattern) {
      builder.addDrawableWidget(block).setPosition(51, 40);
    }
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, MoldingRecipe recipe, IFocusGroup focuses) {
    // basic input output
    builder.addInputSlot(3, 24).addItemStacks(IngredientHelper.getStacks(recipe.getMaterial()));
    RegistryAccess access = SafeClientAccess.getRegistryAccess();
    if (access != null) {
      builder.addOutputSlot(51, 24).add(recipe.getResultItem(access));
    }

    // if we have a mold, we are pressing into the table, so draw pressed item on input and output
    Ingredient pattern = recipe.getPattern();
    if (!IngredientLoadable.isEmpty(pattern)) {
      boolean patternConsumed = recipe.isPatternConsumed();
      List<ItemStack> patternStacks = IngredientHelper.getStacks(pattern);
      IRecipeSlotBuilder inputSlot = builder.addSlot(patternConsumed ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CRAFTING_STATION, 3, 1)
        .addItemStacks(patternStacks);
      if (patternConsumed) {
        inputSlot.addRichTooltipCallback(PATTERN_CONSUMED_TOOLTIP);
      } else {
        IRecipeSlotBuilder preservedSlot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 51, 8).addItemStacks(patternStacks);
        builder.createFocusLink(inputSlot, preservedSlot);
      }
    }
  }

  @Override
  public Identifier getIdentifier(MoldingRecipe recipe) {
    return recipe.getId();
  }
}
