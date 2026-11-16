package slimeknights.tconstruct.common;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;

/**
 * Item tint source that tints a block item using the block colors of its block, without a level or position.
 * Replaces the item color alias that used to be registered through {@code ItemColors}, since item tints are now part of the client item JSON.
 * <p>
 * JSON: {@code {"type": "tconstruct:block_item", "layer": 0}}, where layer is the block tint layer, matching the tint index of the block model.
 * @param layer  Block tint layer to fetch
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public record BlockItemTintSource(int layer) implements ItemTintSource {
  /** ID of this tint source type */
  public static final Identifier ID = TConstruct.getResource("block_item");
  public static final MapCodec<BlockItemTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("layer", 0).forGetter(BlockItemTintSource::layer)
  ).apply(inst, BlockItemTintSource::new));
  /** Tint source for the first layer */
  public static final BlockItemTintSource DEFAULT = new BlockItemTintSource(0);

  @Override
  public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
    if (stack.getItem() instanceof BlockItem blockItem) {
      BlockState state = blockItem.getBlock().defaultBlockState();
      BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, layer);
      if (source != null) {
        return source.color(state);
      }
    }
    return -1;
  }

  @Override
  public MapCodec<BlockItemTintSource> type() {
    return MAP_CODEC;
  }

  /** Registers this tint source type */
  @SubscribeEvent
  static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
    event.register(ID, MAP_CODEC);
  }
}
