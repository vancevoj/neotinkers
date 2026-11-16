package slimeknights.tconstruct.smeltery.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

import javax.annotation.Nullable;

/**
 * Packet to tell a multiblock to render a specific position as the cause of the error
 */
@RequiredArgsConstructor
public class StructureErrorPositionPacket implements BlockEntityPacket<HeatingStructureBlockEntity> {
  public static final Type<StructureErrorPositionPacket> TYPE = new Type<>(TConstruct.getResource("structure_error_position"));
  public static final StreamCodec<RegistryFriendlyByteBuf,StructureErrorPositionPacket> STREAM_CODEC = ISimplePacket.codec(StructureErrorPositionPacket::new);

  private final BlockPos controllerPos;
  @Nullable
  private final BlockPos errorPos;

  public StructureErrorPositionPacket(FriendlyByteBuf buffer) {
    this.controllerPos = buffer.readBlockPos();
    if (buffer.readBoolean()) {
      this.errorPos = buffer.readBlockPos();
    } else {
      this.errorPos = null;
    }
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(controllerPos);
    if (errorPos != null) {
      buffer.writeBoolean(true);
      buffer.writeBlockPos(errorPos);
    } else {
      buffer.writeBoolean(false);
    }
  }

  @Override
  public Type<StructureErrorPositionPacket> type() {
    return TYPE;
  }

  @Override
  public BlockPos pos() {
    return controllerPos;
  }

  @Override
  public Class<HeatingStructureBlockEntity> blockEntityType() {
    return HeatingStructureBlockEntity.class;
  }

  @Override
  public void handleBlockEntity(IPayloadContext context, HeatingStructureBlockEntity be) {
    be.setErrorPos(errorPos);
  }
}
