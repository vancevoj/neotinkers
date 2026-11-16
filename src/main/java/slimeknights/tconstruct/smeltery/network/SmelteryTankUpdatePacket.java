package slimeknights.tconstruct.smeltery.network;

import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent whenever the contents of the smeltery tank change.
 * TODO 1.21: make record
 */
@AllArgsConstructor
public class SmelteryTankUpdatePacket implements BlockEntityPacket<ISmelteryTankHandler> {
  public static final Type<SmelteryTankUpdatePacket> TYPE = new Type<>(TConstruct.getResource("smeltery_tank_update"));
  public static final StreamCodec<RegistryFriendlyByteBuf,SmelteryTankUpdatePacket> STREAM_CODEC = ISimplePacket.codec(SmelteryTankUpdatePacket::new);

  private final BlockPos pos;
  private final List<FluidStack> fluids;

  public SmelteryTankUpdatePacket(FriendlyByteBuf buffer) {
    pos = buffer.readBlockPos();
    int size = buffer.readVarInt();
    fluids = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
    }
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeVarInt(fluids.size());
    for (FluidStack fluid : fluids) {
      FluidStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, fluid);
    }
  }

  @Override
  public Type<SmelteryTankUpdatePacket> type() {
    return TYPE;
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<ISmelteryTankHandler> blockEntityType() {
    return ISmelteryTankHandler.class;
  }

  @Override
  public void handleBlockEntity(IPayloadContext context, ISmelteryTankHandler be) {
    be.updateFluidsFromPacket(fluids);
  }
}
