package slimeknights.tconstruct.common.json;

import com.mojang.serialization.MapCodec;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

@NoArgsConstructor
public class BlockOrEntityCondition implements LootItemCondition {
  public static final BlockOrEntityCondition INSTANCE = new BlockOrEntityCondition();
  /** Unit codec always returning the singleton instance */
  public static final MapCodec<BlockOrEntityCondition> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public MapCodec<BlockOrEntityCondition> codec() {
    return CODEC;
  }

  @Override
  public boolean test(LootContext lootContext) {
    return lootContext.hasParameter(LootContextParams.THIS_ENTITY) || lootContext.hasParameter(LootContextParams.BLOCK_STATE);
  }
}
