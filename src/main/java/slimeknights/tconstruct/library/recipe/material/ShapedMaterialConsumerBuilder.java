package slimeknights.tconstruct.library.recipe.material;

import lombok.NoArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.recipe.data.ConsumerWrapperBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Special variant of {@link ConsumerWrapperBuilder} for {@link ShapedMaterialRecipe}.
 * @deprecated use {@link MaterialsConsumerBuilder}, which requires specifying the ingredients for each part.
 */
@Deprecated
@NoArgsConstructor(staticName = "wrap")
public class ShapedMaterialConsumerBuilder {
  private final List<MaterialVariantId> materials = new ArrayList<>();

  /** Adds a material to the builder */
  public ShapedMaterialConsumerBuilder material(MaterialVariantId material) {
    materials.add(material);
    return this;
  }

  /** Builds the wrapped output */
  public RecipeOutput build(RecipeOutput output) {
    return new Wrapped(output, List.copyOf(materials));
  }

  private record Wrapped(RecipeOutput original, List<MaterialVariantId> materials) implements RecipeOutput {
    @Override
    public Advancement.Builder advancement() {
      return original.advancement();
    }

    @Override
    public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
      return original.lookup(key);
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
      return original.listContextElements(key);
    }

    @Override
    public <S> Optional<HolderLookup<S>> holderLookup(ResourceKey<? extends Registry<? extends S>> registry) {
      return original.holderLookup(registry);
    }

    @Override
    public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
      if (recipe instanceof ShapedRecipe shaped) {
        original.accept(id, new ShapedMaterialRecipe(shaped, materials), advancement, conditions);
      } else {
        TConstruct.LOG.error("Shaped material consumer builder for {} expected a shaped recipe but got {}", id.identifier(), recipe.getClass().getName());
        throw new IllegalStateException("Shaped material consumer builder for " + id.identifier() + " expected a shaped recipe");
      }
    }
  }
}
