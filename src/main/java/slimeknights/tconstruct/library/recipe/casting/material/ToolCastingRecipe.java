package slimeknights.tconstruct.library.recipe.casting.material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Recipe for casting a tool using molten metal on either a tool part or a non-tool part (2 materials or 1) */
public class ToolCastingRecipe extends AbstractMaterialCastingRecipe implements IMultiRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<ToolCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(),
    ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP, CAST_FIELD, ITEM_COST_FIELD,
    TinkerLoadables.MODIFIABLE_ITEM.requiredField("result", r -> r.result),
    ToolCastingRecipe::new);

  private final IModifiable result;
  public ToolCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient cast, int itemCost, IModifiable result) {
    super(serializer, id, group, cast, itemCost, true, false);
    this.result = result;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    ItemStack cast = inv.getStack();
    if (!this.getCast().test(cast)) {
      return false;
    }
    // if we have a material item input, must have exactly 2 materials, else exactly 1
    List<MaterialStatsId> requirements = ToolMaterialHook.stats(result.getToolDefinition());
    if (cast.getItem() instanceof IMaterialItem) {
      if (requirements.size() != 2) {
        return false;
      }
    } else if (requirements.size() != 1) {
      return false;
    }
    MaterialStatsId requirement = requirements.get(requirements.size() - 1);
    return getCachedMaterialFluid(inv).filter(recipe -> requirement.canUseMaterial(recipe.getOutput().getId())).isPresent();

  }

  @Override
  public ItemStack getResultItem() {
    return new ItemStack(result);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv) {
    MaterialVariant material = getCachedMaterialFluid(inv).map(MaterialFluidRecipe::getOutput).orElse(MaterialVariant.UNKNOWN);
    ItemStack cast = inv.getStack();
    MaterialNBT materials;
    if (cast.getItem() instanceof IMaterialItem materialItem) {
      materials = new MaterialNBT(List.of(MaterialVariant.of(materialItem.getMaterial(cast)), material));
    } else {
      materials = new MaterialNBT(List.of(material));
    }
    return ToolBuildHandler.buildItemFromMaterials(result, materials);
  }


  /* JEI display */
  protected List<IDisplayableCastingRecipe> multiRecipes;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes() {
    if (multiRecipes == null) {
      List<MaterialStatsId> requirements = ToolMaterialHook.stats(result.getToolDefinition());
      if (requirements.isEmpty()) {
        multiRecipes = List.of();
      } else {
        RecipeType<?> type = getType();
        List<ItemStack> castItems = Arrays.asList(getCast().getItems());
        MaterialStatsId requirement = requirements.get(requirements.size() - 1);
        // if we have two item requirement, fill in the part in display
        Function<MaterialVariant,MaterialNBT> materials;
        if (requirements.size() > 1) {
          MaterialVariant firstMaterial = MaterialVariant.of(MaterialRegistry.firstWithStatType(requirements.get(0)));
          materials = mat -> new MaterialNBT(List.of(firstMaterial, mat));
        } else {
          materials = mat -> new MaterialNBT(List.of(mat));
        }
        multiRecipes = MaterialCastingLookup
          .getAllCastingFluids().stream()
          .filter(recipe -> {
            MaterialVariant output = recipe.getOutput();
            return !output.isUnknown() && !output.get().isHidden() && requirement.canUseMaterial(output.getId());
          })
          .map(recipe -> {
            List<FluidStack> fluids = resizeFluids(recipe.getFluids());
            int fluidAmount = fluids.stream().mapToInt(FluidStack::getAmount).max().orElse(0);
            // TODO: would be nice to have a list of outputs based on the different inputs
            return new DisplayCastingRecipe(type, castItems, fluids,
                                            ToolBuildHandler.buildItemFromMaterials(result, materials.apply(recipe.getOutput())),
                                            ICastingRecipe.calcCoolingTime(recipe.getTemperature(), itemCost * fluidAmount), isConsumed());
          })
          .collect(Collectors.toList());
      }
    }
    return multiRecipes;
  }
}
