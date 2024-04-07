package slimeknights.tconstruct.library.modifiers.fluid;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.data.loadable.Streamable;
import slimeknights.mantle.network.packet.IThreadsafePacket;

import java.util.List;

/** Packet to sync fluid predicates to the client */
@RequiredArgsConstructor
public class UpdateFluidEffectsPacket implements IThreadsafePacket {
  private static final List<FluidEffect<? super FluidEffectContext.Block>> EMPTY_BLOCK = List.of(FluidEffect.EMPTY);
  private static final List<FluidEffect<? super FluidEffectContext.Entity>> EMPTY_ENTITY = List.of(FluidEffect.EMPTY);
  /** Network syncing ignores effects in the fluid */
  private static final Streamable<List<FluidEffects>> NETWORK = FluidEffects.LOADABLE.list(0);

  private final List<FluidEffects> fluids;

  /** Clientside constructor, sets ingredients */
  public UpdateFluidEffectsPacket(FriendlyByteBuf buffer) {
    this.fluids = NETWORK.decode(buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    NETWORK.encode(buffer, fluids);
  }

  @Override
  public void handleThreadsafe(Context context) {
    FluidEffectManager.INSTANCE.updateFromServer(fluids);
  }
}
