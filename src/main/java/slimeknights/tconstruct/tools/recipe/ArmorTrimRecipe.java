package slimeknights.tconstruct.tools.recipe;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.ClientRecipeCache;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modules.cosmetic.TrimModule;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArmorTrimRecipe implements ITinkerStationRecipe, IMultiRecipe<IDisplayModifierRecipe> {
  protected static final String KEY_INVALID_MATERIAL = TConstruct.makeTranslationKey("recipe", "modifier.armor_trim.invalid_material");
  protected static final String KEY_INVALID_PATTERN = TConstruct.makeTranslationKey("recipe", "modifier.armor_trim.invalid_pattern");
  /** Suffix of vanilla style trim template item IDs, used as a fallback when no smithing recipe knows the template */
  private static final String TEMPLATE_SUFFIX = "_armor_trim_smithing_template";


  @Getter
  private final Identifier id;

  public ArmorTrimRecipe(Identifier id) {
    this.id = id;
    ModifierRecipeLookup.addRecipeModifier(null, TinkerModifiers.trim);
  }

  /** Match for the trim item finding method */
  private record TrimItems(ItemStack template, ItemStack material) {}

  /**
   * Gets the recipes on the current side. The tinker station result is computed without a level, and since 1.21.2 the client only has synced recipes.
   * Mantle syncs the smithing recipes to the client.
   */
  private static RecipeMap currentRecipes() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
      return server.getRecipeManager().recipeMap();
    }
    return ClientRecipeCache.get();
  }

  /** Checks if the given stack is a vanilla style trim template based on its ID */
  private static boolean isNamedTemplate(ItemStack stack) {
    return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith(TEMPLATE_SUFFIX);
  }

  /**
   * Finds the smithing trim recipe pattern for the given template. Since 1.21.5 trim patterns no longer know their template item,
   * and the vanilla template tag was removed, so the smithing trim recipes are the source of truth.
   * @param recipes   Recipe map to search
   * @param template  Template stack
   * @return  Trim pattern, or empty if the stack is not a template of any trim recipe
   */
  private static Optional<Holder<TrimPattern>> findRecipePattern(RecipeMap recipes, ItemStack template) {
    for (RecipeHolder<SmithingRecipe> holder : recipes.byType(RecipeType.SMITHING)) {
      if (holder.value() instanceof SmithingTrimRecipe trim && trim.templateIngredient().map(ingredient -> ingredient.test(template)).orElse(false)) {
        for (RecipeDisplay display : trim.display()) {
          if (display.result() instanceof SlotDisplay.SmithingTrimDemoSlotDisplay demo) {
            return Optional.of(demo.pattern());
          }
        }
      }
    }
    return Optional.empty();
  }

  /** Checks if the given stack is a trim template */
  private static boolean isTemplate(RecipeMap recipes, ItemStack stack) {
    return isNamedTemplate(stack) || findRecipePattern(recipes, stack).isPresent();
  }

  /**
   * Gets the trim pattern for the given template.
   * @param access    Registry access
   * @param template  Template stack
   * @return  Trim pattern ID, or null if not found
   */
  @Nullable
  private static Identifier getPattern(RegistryAccess access, ItemStack template) {
    Optional<Holder<TrimPattern>> pattern = findRecipePattern(currentRecipes(), template);
    if (pattern.isPresent()) {
      return pattern.get().unwrapKey().map(ResourceKey::identifier).orElse(null);
    }
    // fallback for vanilla style template names, in case a datapack removed the smithing recipe
    Identifier itemId = BuiltInRegistries.ITEM.getKey(template.getItem());
    String path = itemId.getPath();
    if (path.endsWith(TEMPLATE_SUFFIX)) {
      Identifier patternId = itemId.withPath(path.substring(0, path.length() - TEMPLATE_SUFFIX.length()));
      if (access.lookupOrThrow(Registries.TRIM_PATTERN).get(ResourceKey.create(Registries.TRIM_PATTERN, patternId)).isPresent()) {
        return patternId;
      }
    }
    return null;
  }

  /** Finds the trim template and material */
  @Nullable
  private static TrimItems findInputs(ITinkerStationContainer inv, RecipeMap recipes) {
    ItemStack template = ItemStack.EMPTY;
    ItemStack material = ItemStack.EMPTY;
    for (int i = 0; i < inv.getInputCount(); i++) {
      ItemStack stack = inv.getInput(i);
      if (!stack.isEmpty()) {
        // find the two matching tags, but ensure no duplicates
        if (isTemplate(recipes, stack)) {
          if (!template.isEmpty()) {
            return null;
          }
          template = stack;
        }
        if (stack.is(ItemTags.TRIM_MATERIALS)) {
          if (!material.isEmpty()) {
            return null;
          }
          material = stack;
        }
      }
    }
    // if we found both, we match
    if (!material.isEmpty() && !template.isEmpty()) {
      return new TrimItems(template, material);
    }
    return null;
  }

  @Override
  public boolean matches(ITinkerStationContainer inv, Level world) {
    // ensure this modifier can be applied
    if (!inv.getTinkerableStack().is(TinkerTags.Items.TRIM)) {
      return false;
    }
    // need to locate two things: the trim material, and the trim template
    return findInputs(inv, RecipeHelper.recipes(world)) != null;
  }

  @Override
  public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer inv, RegistryAccess access) {
    // first need to find our trim and material instances
    TrimItems trimItems = findInputs(inv, currentRecipes());
    // should never happen
    if (trimItems == null) {
      return RecipeResult.pass();
    }
    // validate the material nad pattern items
    Holder<TrimMaterial> materialHolder = trimItems.material.get(DataComponents.PROVIDES_TRIM_MATERIAL);
    Identifier material = materialHolder == null ? null : materialHolder.unwrapKey().map(ResourceKey::identifier).orElse(null);
    if (material == null) {
      return RecipeResult.failure(KEY_INVALID_MATERIAL, trimItems.material.getDisplayName());
    }
    ToolStack original = inv.getTinkerable();
    Identifier pattern = null;
    if (!original.hasTag(TinkerTags.Items.TRIM_NO_PATTERN)) {
      pattern = getPattern(access, trimItems.template);
      if (pattern == null) {
        return RecipeResult.failure(KEY_INVALID_PATTERN, trimItems.template.getDisplayName());
      }
    }

    // store into tool NBT
    ToolStack tool = inv.getTinkerable().copy();
    ModDataNBT persistentData = tool.getPersistentData();
    ModifierId modifier = TinkerModifiers.trim.getId();
    persistentData.putString(TrimModule.materialKey(modifier), material.toString());
    if (pattern != null) {
      persistentData.putString(TrimModule.patternKey(modifier), pattern.toString());
    }

    // add the modifier if missing
    if (tool.getModifierLevel(modifier) == 0) {
      tool.addModifier(modifier, 1);
    }
    return ITinkerStationRecipe.success(tool, inv);
  }

  @Override
  public RecipeSerializer<? extends Recipe<ITinkerStationContainer>> getSerializer() {
    return TinkerModifiers.armorTrimSerializer.get();
  }


  /* JEI */

  private List<IDisplayModifierRecipe> displayRecipes = null;

  @SuppressWarnings("deprecation")
  @Override
  public List<IDisplayModifierRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      // the template tag is gone, so list the templates of all smithing trim recipes
      Map<Item,ItemStack> templates = new LinkedHashMap<>();
      for (RecipeHolder<SmithingRecipe> holder : currentRecipes().byType(RecipeType.SMITHING)) {
        if (holder.value() instanceof SmithingTrimRecipe trim) {
          for (Ingredient ingredient : trim.templateIngredient().stream().toList()) {
            for (ItemStack stack : IngredientHelper.getStacks(ingredient)) {
              templates.putIfAbsent(stack.getItem(), stack);
            }
          }
        }
      }
      List<ItemStack> trims = List.copyOf(templates.values());
      List<ItemStack> toolInputs = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, TinkerTags.Items.TRIM)
                                                 .map(IModifiableDisplay::getDisplayStack).toList();
      if (!trims.isEmpty() && !toolInputs.isEmpty()) {
        Identifier id = getId();
        // trim materials no longer know their item, so find the items providing each material
        Map<ResourceKey<TrimMaterial>,List<ItemStack>> materialItems = new LinkedHashMap<>();
        RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, ItemTags.TRIM_MATERIALS).forEach(item -> {
          ItemStack stack = new ItemStack(item);
          Holder<TrimMaterial> provided = stack.get(DataComponents.PROVIDES_TRIM_MATERIAL);
          if (provided != null) {
            provided.unwrapKey().ifPresent(key -> materialItems.computeIfAbsent(key, k -> new ArrayList<>()).add(stack));
          }
        });
        // one recipe per material
        // TODO: worth using just a single material? focus link material to output tool?
        displayRecipes = access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements()
          .filter(material -> materialItems.containsKey(material.key()))
          .map(material -> new DisplayRecipe(id, toolInputs, trims, material, List.copyOf(materialItems.get(material.key()))))
          .collect(Collectors.toList());
      } else {
        displayRecipes = List.of();
      }
    }
    return displayRecipes;
  }

  private static class DisplayRecipe implements IDisplayModifierRecipe {
    private static final IntRange LEVELS = new IntRange(1, 1);
    private final ModifierEntry RESULT = new ModifierEntry(TinkerModifiers.trim, 1);

    @Getter
    private final Identifier recipeId;
    @Getter
    private final List<ItemStack> toolWithoutModifier;
    @Getter
    private final List<ItemStack> toolWithModifier;
    private final List<ItemStack> trim;
    private final Reference<TrimMaterial> material;
    private final List<ItemStack> materialItem;
    @Getter
    private final Component variant;

    public DisplayRecipe(Identifier id, List<ItemStack> tools, List<ItemStack> trim, Reference<TrimMaterial> holder, List<ItemStack> materialItem) {
      this.recipeId = id;
      this.material = holder;
      toolWithoutModifier = tools;
      this.trim = trim;
      TrimMaterial material = holder.value();
      this.materialItem = materialItem;
      this.variant = material.description().plainCopy();

      String materialName = holder.key().identifier().toString();
      List<ModifierEntry> results = List.of(RESULT);
      Identifier key = TrimModule.materialKey(TinkerModifiers.trim.getId());
      toolWithModifier = tools.stream().map(stack -> IDisplayModifierRecipe.withModifiers(stack, results, data -> data.putString(key, materialName))).toList();

    }

    @Override
    public int getInputCount() {
      return 2;
    }

    @Override
    public List<ItemStack> getDisplayItems(int slot) {
      return switch (slot) {
        case 0 -> trim;
        case 1 -> materialItem;
        default -> List.of();
      };
    }

    @Override
    public ModifierEntry getDisplayResult() {
      return RESULT;
    }

    @Override
    public IntRange getLevel() {
      return LEVELS;
    }


    /* Dynamic */

    @Override
    public boolean isTool(ItemStack check) {
      return check.is(TinkerTags.Items.TRIM);
    }

    @Nullable
    @Override
    public Component canApply(IToolStackView tool) {
      // just say we can always apply trim for simplicity
      return null;
    }

    @Override
    public void applyModifier(ToolStack tool) {
      // store into tool NBT
      ModDataNBT persistentData = tool.getPersistentData();
      ModifierId modifier = TinkerModifiers.trim.getId();
      persistentData.putString(TrimModule.materialKey(modifier), material.key().identifier().toString());
      // skip pattern, it doesn't show on the item, and it saves us a lookup

      // add the modifier if missing
      if (tool.getModifierLevel(modifier) == 0) {
        tool.addModifier(modifier, 1);
      }
    }

    @Override
    public boolean shouldDisplayValidate() {
      return false;
    }
  }
}
