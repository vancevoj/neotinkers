package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient that only matches tools with a specific hook */
public class ToolHookIngredient implements ICustomIngredient {
  public static final Identifier ID = TConstruct.getResource("tool_hook");
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<ToolHookIngredient> LOADABLE = RecordLoadable.create(
    Loadables.ITEM_TAG.defaultField("tag", TinkerTags.Items.MODIFIABLE, i -> i.tag),
    ToolHooks.LOADER.requiredField("hook", i -> i.hook),
    ToolHookIngredient::new);
  /** Ingredient type instance, registered under {@link #ID} in {@link slimeknights.tconstruct.tools.TinkerTools} */
  public static final IngredientType<ToolHookIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  private final TagKey<Item> tag;
  private final ModuleHook<?> hook;

  protected ToolHookIngredient(TagKey<Item> tag, ModuleHook<?> hook) {
    this.tag = tag;
    this.hook = hook;
  }

  public static ToolHookIngredient of(TagKey<Item> tag, ModuleHook<?> hook) {
    return new ToolHookIngredient(tag, hook);
  }

  public static ToolHookIngredient of(ModuleHook<?> hook) {
    return of(TinkerTags.Items.MODIFIABLE, hook);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && stack.is(tag) && stack.getItem() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(hook);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  /** Gets all tools in the tag with the hook */
  private List<Holder<Item>> getMatchingItems() {
    // filtered version of tag values
    List<Holder<Item>> items = new ArrayList<>();
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (holder.value() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(hook)) {
        items.add(holder);
      }
    }
    return items;
  }

  /** Lists the matching tools. If there are none, lists a barrier so the ingredient is not considered empty, as before 1.21.2. */
  @SuppressWarnings("deprecation")
  @Override
  public Stream<Holder<Item>> items() {
    List<Holder<Item>> items = getMatchingItems();
    if (items.isEmpty()) {
      return Stream.of(Items.BARRIER.builtInRegistryHolder());
    }
    return items.stream();
  }

  /** Displays the matching tools, or a barrier named after the tag if there are none */
  @Override
  public SlotDisplay display() {
    List<Holder<Item>> items = getMatchingItems();
    if (items.isEmpty()) {
      ItemStack barrier = new ItemStack(Items.BARRIER);
      barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + tag.location()));
      return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(barrier));
    }
    return new SlotDisplay.Composite(items.stream().map(Ingredient::displayForSingleItem).toList());
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof ToolHookIngredient ingredient && this.tag.equals(ingredient.tag) && this.hook.equals(ingredient.hook);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ToolHookIngredient.class, tag, hook);
  }
}
