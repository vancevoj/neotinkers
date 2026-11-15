package slimeknights.tconstruct.library.json.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot condition that only runs if all required values in the given loot context set are present. Good heuristic for using that set.
 * TODO: migrate to Mantle
 */
public record HasLootContextSetCondition(ContextKeySet set) implements LootItemCondition {
  /** Codec for this condition */
  public static final MapCodec<HasLootContextSetCondition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    LootContextParamSets.CODEC.fieldOf("set").forGetter(HasLootContextSetCondition::set)
  ).apply(inst, HasLootContextSetCondition::new));

  /** Creates a new builder instance */
  public static Builder builder(ContextKeySet set) {
    return new Builder(set);
  }

  @Override
  public MapCodec<HasLootContextSetCondition> codec() {
    return CODEC;
  }

  @Override
  public boolean test(LootContext context) {
    for (ContextKey<?> param : set.required()) {
      if (!context.hasParameter(param)) {
        return false;
      }
    }
    return true;
  }

  /** Builder logic for this condition */
  public record Builder(ContextKeySet set) implements LootItemCondition.Builder {
    @Override
    public LootItemCondition build() {
      return new HasLootContextSetCondition(set);
    }
  }
}
