package slimeknights.tconstruct.tables.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;

/**
 * Packet to send the current crafting recipe to a player who opens the crafting station.
 * TODO 1.21: make record.
 */
public class UpdateCraftingRecipePacket implements BlockEntityPacket<CraftingStationBlockEntity> {
  public static final Type<UpdateCraftingRecipePacket> TYPE = new Type<>(TConstruct.getResource("update_crafting_recipe"));
  public static final StreamCodec<RegistryFriendlyByteBuf,UpdateCraftingRecipePacket> STREAM_CODEC = ISimplePacket.codec(UpdateCraftingRecipePacket::new);

  private final BlockPos pos;
  private final Identifier recipe;
  public UpdateCraftingRecipePacket(BlockPos pos, RecipeHolder<CraftingRecipe> recipe) {
    this.pos = pos;
    this.recipe = recipe.id().identifier();
  }

  public UpdateCraftingRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.recipe = buffer.readIdentifier();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeIdentifier(recipe);
  }

  @Override
  public Type<UpdateCraftingRecipePacket> type() {
    return TYPE;
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<CraftingStationBlockEntity> blockEntityType() {
    return CraftingStationBlockEntity.class;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handleBlockEntity(IPayloadContext context, CraftingStationBlockEntity be) {
    Level level = be.getLevel();
    assert level != null;
    // the client only has the recipes synced by the server, crafting recipes are requested by both Mantle and Tinkers
    RecipeHolder<?> holder = RecipeHelper.recipes(level).byKey(RecipeHelper.key(this.recipe));
    if (holder != null && holder.value() instanceof CraftingRecipe) {
      be.updateRecipe((RecipeHolder<CraftingRecipe>) holder);
    } else {
      TConstruct.LOG.error("Failed to update Crafting Station Recipe at {}: unknown recipe {}", pos, this.recipe);
    }
  }
}
