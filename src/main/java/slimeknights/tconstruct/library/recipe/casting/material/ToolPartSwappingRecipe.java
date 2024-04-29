package slimeknights.tconstruct.library.recipe.casting.material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.casting.AbstractCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;

/** Recipe for casting a tool using molten metal on either a tool part or a non-tool part (2 materials or 1) */
public class ToolPartSwappingRecipe extends AbstractMaterialCastingRecipe {
  public static final RecordLoadable<ToolPartSwappingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(),
    ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("tool", AbstractCastingRecipe::getCast),
    ITEM_COST_FIELD,
    IntLoadable.FROM_ONE.requiredField("material_index", r -> r.materialIndex),
    ToolPartSwappingRecipe::new);

  private final int materialIndex;
  public ToolPartSwappingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient tools, int itemCost, int materialIndex) {
    super(serializer, id, group, tools, itemCost, true, false);
    this.materialIndex = materialIndex;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    ItemStack cast = inv.getStack();
    if (this.getCast().test(cast) && cast.getItem() instanceof IModifiable modifiable) {
      // if we have a material item input, must have exactly 2 materials, else exactly 1
      List<MaterialStatsId> requirements = ToolMaterialHook.stats(modifiable.getToolDefinition());
      if (materialIndex < requirements.size()) {
        MaterialStatsId requirement = requirements.get(materialIndex);
        return getCachedMaterialFluid(inv).filter(recipe -> requirement.canUseMaterial(recipe.getOutput().getId())).isPresent();
      }
    }
    return false;

  }

  @Deprecated(forRemoval = true)
  @Override
  public ItemStack getResultItem() {
    return ItemStack.EMPTY;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv) {
    MaterialVariant material = getCachedMaterialFluid(inv).map(MaterialFluidRecipe::getOutput).orElse(MaterialVariant.UNKNOWN);
    ToolStack tool = ToolStack.from(inv.getStack());
    tool.replaceMaterial(materialIndex, material.getVariant());
    return tool.createStack();
  }
}
