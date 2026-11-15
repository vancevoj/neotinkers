package slimeknights.tconstruct.tables.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

/**
 * Packet to send the current crafting recipe to a player who opens the tinker station.
 * TODO 1.21: make record.
 */
public class UpdateTinkerStationRecipePacket implements IThreadsafePacket {
  public static final Type<UpdateTinkerStationRecipePacket> TYPE = new Type<>(TConstruct.getResource("update_tinker_station_recipe"));
  public static final StreamCodec<RegistryFriendlyByteBuf,UpdateTinkerStationRecipePacket> STREAM_CODEC = ISimplePacket.codec(UpdateTinkerStationRecipePacket::new);

  private final BlockPos pos;
  private final Identifier recipe;
  public UpdateTinkerStationRecipePacket(BlockPos pos, Identifier recipe) {
    this.pos = pos;
    this.recipe = recipe;
  }

  public UpdateTinkerStationRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.recipe = buffer.readIdentifier();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeIdentifier(recipe);
  }

  @Override
  public Type<UpdateTinkerStationRecipePacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(UpdateTinkerStationRecipePacket packet) {
      Minecraft mc = Minecraft.getInstance();
      Level world = mc.level;
      if (world != null) {
        // start by fetching the recipe, no further work if it's missing
        ITinkerStationRecipe recipe = RecipeHelper.getRecipe(world.getRecipeManager(), packet.recipe, ITinkerStationRecipe.class).orElse(null);
        if (recipe == null) {
          TConstruct.LOG.error("Failed to update Tinker Station Recipe at {}: unknown recipe {}", packet.pos, packet.recipe);
          return;
        }
        // if the screen is open, use that to get the TE and update the screen as we want to update the screen too
        if (mc.screen instanceof TinkerStationScreen stationScreen) {
          TinkerStationBlockEntity te = stationScreen.getTileEntity();
          if (te != null && te.getBlockPos().equals(packet.pos)) {
            te.updateRecipe(packet.recipe, recipe);
            stationScreen.updateDisplay();
            return;
          }
        }
        // if the wrong screen is open or no screen, use the tile directly
        if (BlockEntityPacket.getBlockEntity(world, packet.pos, packet) instanceof TinkerStationBlockEntity be) {
          be.updateRecipe(packet.recipe, recipe);
        } else {
          TConstruct.LOG.error("Failed to update Tinker Station Recipe at {} to {}: unable to find Tinker Station", packet.pos, packet.recipe);
        }
      }
    }
  }
}
