package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.shared.TinkerAttributes;

import java.util.Set;

/** Condition to check if the enemy has the chrysophilite modifier */
public class ChrysophiliteLootCondition implements LootItemCondition {
  public static final ChrysophiliteLootCondition INSTANCE = new ChrysophiliteLootCondition();
  /** Codec for this condition, it has no fields so always returns the singleton */
  public static final MapCodec<ChrysophiliteLootCondition> CODEC = MapCodec.unit(INSTANCE);

  private ChrysophiliteLootCondition() {}

  @Override
  public boolean test(LootContext context) {
    return context.getOptional(LootContextParams.THIS_ENTITY) instanceof LivingEntity living && living.getAttributeValue(TinkerAttributes.CHRYSOPHILITE) >= 1;
  }

  @Override
  public Set<ContextKey<?>> getReferencedContextParams() {
    return ImmutableSet.of(LootContextParams.THIS_ENTITY);
  }

  @Override
  public MapCodec<ChrysophiliteLootCondition> codec() {
    return CODEC;
  }
}
