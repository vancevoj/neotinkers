package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.hook.build.CraftCountModifierHook;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.plugin.jei.util.CategoryUtil;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe.SLOT_SIZE;
import static slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe.X_OFFSET;
import static slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe.Y_OFFSET;

public class ToolBuildingCategory extends AbstractRecipeCategory<ToolBuildingRecipe> {
  private static final Identifier BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");
  private static final Component TITLE = TConstruct.makeTranslation("jei", "tinkering.tool_building");
  private static final Component ANVIL = TConstruct.makeTranslation("jei", "tinkering.tool_building.anvil");
  private final IDrawable anvil;
  private static final int WIDTH = 134;
  private static final int HEIGHT = 66;
  private static final int ITEM_SIZE = 16;
  /** Name for the slot containing the tool to help us find it in {@link #onDisplayedIngredientsUpdate(ToolBuildingRecipe, List, IFocusGroup)} */
  private static final String RESULT_SLOT = "result";
  /** Prefix for the slots containing the tool parts to help us find it in {@link #onDisplayedIngredientsUpdate(ToolBuildingRecipe, List, IFocusGroup)} */
  private static final String PART_SLOT_PREFIX = "tool_part_";
  /** Texture size of {@link #BACKGROUND_LOC}, the translucent parts are blit directly as JEI drawables cannot be tinted */
  private static final int TEXTURE_SIZE = 256;
  /** Color for the item cover, makes the tool appear transparent */
  private static final int ITEM_COVER_COLOR = ARGB.white(0.82f);
  /** Color for the slot backgrounds, very transparent */
  private static final int SLOT_BACKGROUND_COLOR = ARGB.white(0.28f);

  public ToolBuildingCategory(IGuiHelper guiHelper) {
    super(TConstructJEIConstants.TOOL_BUILDING, TITLE, guiHelper.createDrawableItemStack(TinkerTools.pickaxe.get().getRenderTool()), WIDTH, HEIGHT);
    this.anvil = guiHelper.createDrawable(BACKGROUND_LOC, 128, 61, ITEM_SIZE, ITEM_SIZE);
  }

  @Override
  public void createRecipeExtras(IRecipeExtrasBuilder builder, ToolBuildingRecipe recipe, IFocusGroup focuses) {
    builder.addRecipeArrowWidget().setPosition(74, 22);
    if (recipe.requiresAnvil()) {
      builder.addDrawableWidget(anvil).setPosition(76, 44).setTooltip(ANVIL);
    }
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, ToolBuildingRecipe recipe, IFocusGroup focuses) {
    List<List<ItemStack>> toolParts = recipe.getAllToolParts();
    List<List<ItemStack>> partsAndExtras = Stream.concat(
      toolParts.stream(),
      recipe.getExtraRequirements().stream().map(IngredientHelper::getStacks)
    ).collect(Collectors.toList()); // using collectors.toList to ensure we can mutate the list

    // if we have parts, and there is a focus, use that to override the materials on the parts
    int partCount = toolParts.size();
    if (partCount > 0) {
      ItemStack focus = CategoryUtil.getResultItemFocus(focuses);
      // if we have an output focus, set the input slots to match
      if (!focus.isEmpty()) {
        List<IToolPart> parts = recipe.getToolParts();
        MaterialIdNBT materials = MaterialIdNBT.from(focus);
        for (int i = 0; i < partCount; i++) {
          IMaterialItem part = parts.get(i);
          MaterialVariantId material = materials.getMaterial(i);
          if (part.canUseMaterial(material.getId())) {
            partsAndExtras.set(i, List.of(part.withMaterialForDisplay(material)));
          }
        }
      }
    }

    // start building the layout
    List<LayoutSlot> layoutSlots = recipe.getLayoutSlots();
    int missingSlots = partsAndExtras.size() - layoutSlots.size();

    if (missingSlots < 0) {
      partsAndExtras = new ArrayList<>(partsAndExtras);
      for (int additionalItem = 0; additionalItem > missingSlots; additionalItem--){
        // just add nothing to fill the empty slots
        partsAndExtras.add(List.of(ItemStack.EMPTY));
      }
    }

    IRecipeSlotBuilder firstSlot = null;
    for (int i = 0; i < layoutSlots.size(); i++) {
      IRecipeSlotBuilder slot = builder.addInputSlot(layoutSlots.get(i).getX() + X_OFFSET, layoutSlots.get(i).getY() + Y_OFFSET)
        .addItemStacks(partsAndExtras.get(i));
      if (i < partCount) {
        slot.setSlotName(PART_SLOT_PREFIX + i);
      }
      if (i == 0) {
        firstSlot = slot;
      }
    }

    // create a focus link between result and first slot if same size
    List<ItemStack> result = recipe.getDisplayOutput();
    IRecipeSlotBuilder resultSlot = builder.addOutputSlot(WIDTH - 26, 23)
      .addItemStacks(result).setSlotName(RESULT_SLOT).setOutputSlotBackground();
    if (partCount == 0 && result.size() > 1 && partsAndExtras.get(0).size() == result.size()) {
      builder.createFocusLink(resultSlot, firstSlot);
    }

    // add hidden materials as a hidden input for recipe lookup
    List<ItemStack> hiddenInputs = recipe.getHiddenInputs();
    if (!hiddenInputs.isEmpty()) {
      builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(hiddenInputs);
    }
  }

  @Override
  public void onDisplayedIngredientsUpdate(ToolBuildingRecipe recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
    // no input slots means we may be using a focus link instead, either way no work to do
    IRecipeSlotDrawable resultSlot = CategoryUtil.findSlot(recipeSlots, RESULT_SLOT);
    List<IRecipeSlotDrawable> inputSlots = CategoryUtil.filterSlots(recipeSlots, PART_SLOT_PREFIX);
    if (resultSlot != null && !inputSlots.isEmpty()) {
      List<IToolPart> parts = recipe.getToolParts();
      List<MaterialVariantId> variants = new ArrayList<>(parts.size());
      for (int i = 0; i < parts.size(); i++) {
        variants.add(parts.get(i).getMaterial(inputSlots.get(i).getDisplayedItemStack().orElse(ItemStack.EMPTY)));
      }
      variants.addAll(recipe.getExtraMaterials());
      ItemStack stack = new MaterialIdNBT(variants).updateStack(new ItemStack(recipe.getOutput()));
      if (stack.getMaxStackSize() > 1) {
        ToolStack tool = ToolStack.from(stack);
        tool.rebuildStats();
        stack.setCount(CraftCountModifierHook.maxStackSize(tool, recipe.shrinkToolSlotBy()));
      }

      resultSlot.createDisplayOverrides().add(CraftCountModifierHook.copyMaterials(new MaterialIdNBT(variants), recipe.getOutput(), recipe.getOutputCount()));
    }
  }

  @Override
  public void draw(ToolBuildingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
    // first, draw the item background
    ItemStack outputStack = recipe.getOutput() instanceof IModifiableDisplay modifiable ? modifiable.getRenderTool() : recipe.getOutput().asItem().getDefaultInstance();
    Matrix3x2fStack renderPose = graphics.pose();
    renderPose.pushMatrix();
    renderPose.translate(5, 6.5f);
    renderPose.scale(3.7f, 3.7f);
    graphics.item(outputStack, 0, 0);
    renderPose.popMatrix();

    // next, overlay the item with transparent grey, makes it appear transparent
    // no depth in the 26.3 GUI, later draws are on top of the item
    graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_LOC, 5, 6, 122, 77, 70, 60, TEXTURE_SIZE, TEXTURE_SIZE, ITEM_COVER_COLOR);

    // next, draw slot backgrounds very transparent
    for (LayoutSlot layoutSlot : recipe.getLayoutSlots()) {
      // need to offset by 1 because the inventory slot icons are 18x18
      graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_LOC, layoutSlot.getX() + X_OFFSET - 1, layoutSlot.getY() + Y_OFFSET - 1, 144, 59, SLOT_SIZE, SLOT_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, SLOT_BACKGROUND_COLOR);
    }
    // finally, draw slot borders opaque
    for (LayoutSlot layoutSlot : recipe.getLayoutSlots()) {
      // need to offset by 1 because the inventory slot icons are 18x18
      graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_LOC, layoutSlot.getX() + X_OFFSET - 1, layoutSlot.getY() + Y_OFFSET - 1, 162, 59, SLOT_SIZE, SLOT_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }
  }

  @Override
  public Identifier getIdentifier(ToolBuildingRecipe recipe) {
    return recipe.getId();
  }
}
