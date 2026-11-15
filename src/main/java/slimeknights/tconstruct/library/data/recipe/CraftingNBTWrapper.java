package slimeknights.tconstruct.library.data.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Helper to add data components to vanilla recipe results.
 * <p>
 * In 1.20.1 this added raw NBT to the serialized recipe JSON; in 1.21 recipe results are {@link ItemStack}s carrying
 * {@link net.minecraft.core.component.DataComponentMap data components}. Since 1.21.2 recipe results are immutable
 * {@link ItemStackTemplate}s, so this wrapper serializes the recipe, adds the given {@link DataComponentPatch} to its
 * {@code result} and parses it back before passing it to the base output. The recipe must have a {@code result} field
 * holding an item stack template, as all crafting recipes do.
 * Where possible, prefer building the recipe with an {@link ItemStackTemplate} result holding the components instead.
 */
public record CraftingNBTWrapper(RecipeOutput base, DataComponentPatch components) implements RecipeOutput {
  @Override
  public Advancement.Builder advancement() {
    return base.advancement();
  }

  @Override
  public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
    return base.lookup(key);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
    return base.listContextElements(key);
  }

  @Override
  public <S> Optional<HolderLookup<S>> holderLookup(ResourceKey<? extends Registry<? extends S>> registry) {
    return base.holderLookup(registry);
  }

  @Override
  public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
    if (!components.isEmpty()) {
      recipe = applyComponents(id, recipe);
    }
    base.accept(id, recipe, advancement, conditions);
  }

  /** Adds our components to the result of the given recipe through a codec round trip */
  private Recipe<?> applyComponents(ResourceKey<Recipe<?>> id, Recipe<?> recipe) {
    RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, new RegistryOps.RegistryInfoLookup() {
      @Override
      public <T> Optional<HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
        return Optional.of(base.lookup(registryKey));
      }
    });
    JsonElement json = Recipe.DIRECT_CODEC.encodeStart(ops, recipe).getOrThrow(error -> new IllegalStateException("Failed to serialize recipe " + id.identifier() + ": " + error));
    if (!(json instanceof JsonObject object) || !object.has("result")) {
      throw new IllegalArgumentException("Cannot add components to recipe " + id.identifier() + " as it has no result");
    }
    ItemStackTemplate result = ItemStackTemplate.CODEC.parse(ops, object.get("result")).getOrThrow(error -> new IllegalStateException("Failed to parse result of recipe " + id.identifier() + ": " + error));
    // merge our patch on top of the existing components
    DataComponentPatch.Builder patch = DataComponentPatch.builder();
    applyPatch(patch, result.components());
    applyPatch(patch, components);
    ItemStackTemplate updated = new ItemStackTemplate(result.item(), result.count(), patch.build());
    object.add("result", ItemStackTemplate.CODEC.encodeStart(ops, updated).getOrThrow(error -> new IllegalStateException("Failed to serialize result of recipe " + id.identifier() + ": " + error)));
    return Recipe.DIRECT_CODEC.parse(ops, object).getOrThrow(error -> new IllegalStateException("Failed to parse recipe " + id.identifier() + ": " + error));
  }

  /** Copies all changes from the given patch into the builder */
  private static void applyPatch(DataComponentPatch.Builder builder, DataComponentPatch patch) {
    DataComponentPatch.SplitResult split = patch.split();
    for (TypedDataComponent<?> component : split.added()) {
      builder.set(component);
    }
    for (DataComponentType<?> removed : split.removed()) {
      builder.remove(removed);
    }
  }

  /** Creates a wrapped recipe output, applying the given data components to results passed through it */
  public static RecipeOutput wrap(RecipeOutput base, DataComponentPatch components) {
    return new CraftingNBTWrapper(base, components);
  }
}
