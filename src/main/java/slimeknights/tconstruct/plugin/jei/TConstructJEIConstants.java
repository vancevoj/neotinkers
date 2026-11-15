package slimeknights.tconstruct.plugin.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.loadable.LoadableCodec;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.LazyMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.material.IDisplayMaterialRecipe;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayToolTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;

import java.util.function.Function;

public class TConstructJEIConstants {
  public static final Identifier PLUGIN = TConstruct.getResource("jei_plugin");

  // ingredient types
  public static final IIngredientTypeWithSubtypes<Modifier,ModifierEntry> MODIFIER_TYPE = new IIngredientTypeWithSubtypes<>() {
    @Override
    public Class<? extends ModifierEntry> getIngredientClass() {
      return ModifierEntry.class;
    }

    @Override
    public Class<? extends Modifier> getIngredientBaseClass() {
      return Modifier.class;
    }

    @Override
    public Modifier getBase(ModifierEntry ingredient) {
      return ingredient.getModifier();
    }
  };
  public static final IIngredientType<Pattern> PATTERN_TYPE = () -> Pattern.class;
  public static final IIngredientTypeWithSubtypes<SlotType, SlotCount> SLOT_TYPE = new IIngredientTypeWithSubtypes<>() {
    @Override
    public Class<? extends SlotCount> getIngredientClass() {
      return SlotCount.class;
    }

    @Override
    public Class<? extends SlotType> getIngredientBaseClass() {
      return SlotType.class;
    }

    @Override
    public SlotType getBase(SlotCount slots) {
      return slots.type();
    }
  };
  public static final IIngredientTypeWithSubtypes<LazyMaterial, MaterialVariant> MATERIAL_TYPE = new IIngredientTypeWithSubtypes<>() {
    @Override
    public Class<? extends MaterialVariant> getIngredientClass() {
      return MaterialVariant.class;
    }

    @Override
    public Class<? extends LazyMaterial> getIngredientBaseClass() {
      return LazyMaterial.class;
    }

    @Override
    public LazyMaterial getBase(MaterialVariant variant) {
      return variant.getBase();
    }
  };

  // ingredient codecs, JEI uses them for bookmarks and its config
  public static final Codec<ModifierEntry> MODIFIER_CODEC = new LoadableCodec<>(ModifierEntry.LOADABLE);
  public static final Codec<Pattern> PATTERN_CODEC = Identifier.CODEC.xmap(Pattern::new, Function.identity());
  public static final Codec<SlotCount> SLOT_CODEC = new LoadableCodec<>(SlotCount.LOADABLE);
  public static final Codec<MaterialVariant> MATERIAL_CODEC = new LoadableCodec<>(MaterialVariantId.LOADABLE).xmap(MaterialVariant::of, MaterialVariant::getVariant);

  // casting
  public static final IRecipeType<IDisplayableCastingRecipe> CASTING_BASIN = type("casting_basin", IDisplayableCastingRecipe.class);
  public static final IRecipeType<IDisplayableCastingRecipe> CASTING_TABLE = type("casting_table", IDisplayableCastingRecipe.class);
  public static final IRecipeType<MoldingRecipe> MOLDING = type("molding", MoldingRecipe.class);

  // melting
  public static final IRecipeType<IDisplayableMeltingRecipe> MELTING = type("melting", IDisplayableMeltingRecipe.class);
  public static final IRecipeType<EntityMeltingRecipe> ENTITY_MELTING = type("entity_melting", EntityMeltingRecipe.class);
  public static final IRecipeType<AlloyRecipe> ALLOY = type("alloy", AlloyRecipe.class);
  public static final IRecipeType<IDisplayableMeltingRecipe> FOUNDRY = type("foundry", IDisplayableMeltingRecipe.class);
  public static final IRecipeType<MeltingFuel> FUEL = type("fuel", MeltingFuel.class);

  // tinker station
  public static final IRecipeType<IDisplayModifierRecipe> MODIFIERS = type("modifiers", IDisplayModifierRecipe.class);
  public static final IRecipeType<SeveringRecipe> SEVERING = type("severing", SeveringRecipe.class);
  public static final IRecipeType<ToolBuildingRecipe> TOOL_BUILDING = type("tool_recipes", ToolBuildingRecipe.class);
  public static final IRecipeType<IDisplayToolTinkering> TOOL_MODIFICATION = type("tool_modification", IDisplayToolTinkering.class);

  // part builder
  public static final IRecipeType<IDisplayMaterialRecipe> MATERIALS = type("materials", IDisplayMaterialRecipe.class);
  public static final IRecipeType<IDisplayPartBuilderRecipe> PART_BUILDER = type("part_builder", IDisplayPartBuilderRecipe.class);

  // modifier workstation
  public static final IRecipeType<IModifierWorktableRecipe> MODIFIER_WORKTABLE = type("worktable", IModifierWorktableRecipe.class);

  private static <T> IRecipeType<T> type(String name, Class<T> clazz) {
    return IRecipeType.create(TConstruct.MOD_ID, name, clazz);
  }
}
