package slimeknights.tconstruct.library.modifiers.fluid.block;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;

/** Effect to place a block in using logic similar to block item placement. */
public record PlaceBlockFluidEffect(Block block) implements FluidEffect<FluidEffectContext.Block> {
  public static final RecordLoadable<PlaceBlockFluidEffect> LOADER = RecordLoadable.create(Loadables.BLOCK.requiredField("block", e -> e.block), PlaceBlockFluidEffect::new);

  @Override
  public RecordLoadable<PlaceBlockFluidEffect> getLoader() {
    return LOADER;
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Block context, FluidAction action) {
    if (level.isFull()) {
      // build the context
      BlockPlaceContext placeContext = new BlockPlaceContext(context.getLevel(), context.getPlayer(), InteractionHand.MAIN_HAND, new ItemStack(block), context.getHitResult());
      if (placeContext.canPlace()) {
        // if we have a blockitem, we can offload a lot of the logic to it
        if (block.asItem() instanceof BlockItem blockItem) {
          if (action.execute()) {
            return blockItem.place(placeContext).consumesAction() ? 1 : 0;
          }
          // simulating is trickier but the methods exist
          placeContext = blockItem.updatePlacementContext(placeContext);
          if (placeContext == null) {
            return 0;
          }
        }
        // following code is based on block item, with notably differences of not calling block item methods (as if we had one we'd use it above)
        // we do notably call this logic in simulation as we need to stop the block item logic early, differences are noted in comments with their vanilla impacts

        // simulate note: we don't ask the block item for its state for placement as that method is protected, this notably affects signs/banners (unlikely need)
        BlockState state = block.getStateForPlacement(placeContext);
        if (state == null) {
          return 0;
        }
        // simulate note: we don't call BlockItem#canPlace as its protected, though never overridden in vanilla
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos clicked = placeContext.getClickedPos();
        if (!state.canSurvive(world, clicked) || !world.isUnobstructed(state, clicked, player == null ? CollisionContext.empty() : CollisionContext.of(player))) {
          return 0;
        }
        // at this point the only check we are missing on simulate is actually placing the block failing
        if (action.execute()) {
          // actually place the block
          if (!world.setBlock(clicked, state, Block.UPDATE_ALL_IMMEDIATE)) {
            return 0;
          }
          // if its the expected block, run some criteria stuffs
          BlockState placed = world.getBlockState(clicked);
          if (placed.is(block)) {
            // difference from BlockItem: do not update block state or block entity from tag as we have no tag
            // it might however be worth passing in a set of properties to set here as part of JSON
            // setPlacedBy probably won't be useful as w lack a tag, but who knw
            ItemStack dummyStack = placeContext.getItemInHand();
            block.setPlacedBy(world, clicked, placed, player, dummyStack);
            if (player instanceof ServerPlayer serverPlayer) {
              CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, clicked, dummyStack);
            }
          }

          // resulting events
          LivingEntity placer = context.getEntity(); // possible that living is nonnull when player is null
          world.gameEvent(GameEvent.BLOCK_PLACE, clicked, GameEvent.Context.of(placer, placed));
          SoundType sound = placed.getSoundType(world, clicked, placer);
          world.playSound(null, clicked, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }
        return 1;
      }
    }
    return 0;
  }
}
