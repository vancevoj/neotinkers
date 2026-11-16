package slimeknights.tconstruct.smeltery.network;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

/**
 * Sent to clients to activate the faucet animation clientside.
 * TODO 1.21: make record.
 */
@RequiredArgsConstructor
@ToString
public class FaucetActivationPacket implements BlockEntityPacket<FaucetBlockEntity> {
  public static final Type<FaucetActivationPacket> TYPE = new Type<>(TConstruct.getResource("faucet_activation"));
  public static final StreamCodec<RegistryFriendlyByteBuf,FaucetActivationPacket> STREAM_CODEC = ISimplePacket.codec(FaucetActivationPacket::new);

  protected final BlockPos pos;
  protected final FluidStack fluid;
  private final boolean isPouring;

  public FaucetActivationPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    this.isPouring = buffer.readBoolean();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    FluidStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, fluid);
    buffer.writeBoolean(isPouring);
  }

  @Override
  public Type<FaucetActivationPacket> type() {
    return TYPE;
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<FaucetBlockEntity> blockEntityType() {
    return FaucetBlockEntity.class;
  }

  @Override
  public void handleBlockEntity(IPayloadContext context, FaucetBlockEntity be) {
    be.onActivationPacket(fluid, isPouring);
  }
}
