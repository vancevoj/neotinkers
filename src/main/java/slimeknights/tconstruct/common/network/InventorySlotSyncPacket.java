package slimeknights.tconstruct.common.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.TConstruct;

public class InventorySlotSyncPacket implements IThreadsafePacket {
  public static final Type<InventorySlotSyncPacket> TYPE = new Type<>(TConstruct.getResource("inventory_slot_sync"));
  public static final StreamCodec<RegistryFriendlyByteBuf,InventorySlotSyncPacket> STREAM_CODEC = ISimplePacket.codec(InventorySlotSyncPacket::new);

  public final ItemStack itemStack;
  public final int slot;
  public final BlockPos pos;

  public InventorySlotSyncPacket(ItemStack itemStack, int slot, BlockPos pos) {
    this.itemStack = itemStack;
    this.slot = slot;
    this.pos = pos;
  }

  public InventorySlotSyncPacket(FriendlyByteBuf buffer) {
    this.itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    this.slot = buffer.readShort();
    this.pos = buffer.readBlockPos();
  }

  @Override
  public void encode(FriendlyByteBuf packetBuffer) {
    ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) packetBuffer, this.itemStack);
    packetBuffer.writeShort(this.slot);
    packetBuffer.writeBlockPos(this.pos);
  }

  @Override
  public Type<InventorySlotSyncPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(InventorySlotSyncPacket packet) {
      Level world = Minecraft.getInstance().level;
      if (world != null && world.isLoaded(packet.pos)) {
        // Set the item directly on the block entity's container using the RAW slot index. The old
        // path went through the ItemHandler.BLOCK capability, which for the casting table/basin is a
        // SidedInvWrapper(DOWN) that remaps slot indices via getSlotsForFace, so the synced item
        // landed on the wrong slot (or out of range) and the in-world renderer saw an empty slot.
        // Block entities that are not a Container (smeltery, foundry, melter, duct, proxy tank, fluid cannon) still go
        // through the unsided item handler, which for all of them indexes slots the same way as the server module.
        BlockEntity te = world.getBlockEntity(packet.pos);
        boolean updated = false;
        if (te instanceof Container container) {
          if (packet.slot >= 0 && packet.slot < container.getContainerSize()) {
            container.setItem(packet.slot, packet.itemStack);
            updated = true;
          }
        } else if (te != null && world.getCapability(Capabilities.Item.BLOCK, packet.pos, null) instanceof IItemHandlerModifiable modifiable) {
          if (packet.slot >= 0 && packet.slot < modifiable.getSlots()) {
            modifiable.setStackInSlot(packet.slot, packet.itemStack);
            updated = true;
          }
        }
        if (updated) {
          //noinspection ConstantConditions
          Minecraft.getInstance().levelRenderer.blockChanged(null, packet.pos, null, null, 0);
        }
      }
    }
  }
}
