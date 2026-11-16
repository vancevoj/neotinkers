package slimeknights.tconstruct.tables.client.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;

public class CraftingStationScreen extends BaseTabbedScreen<CraftingStationBlockEntity,CraftingStationContainerMenu> {
  private static final Identifier CRAFTING_TABLE_GUI_TEXTURES = Identifier.parse("textures/gui/container/crafting_table.png");

  public CraftingStationScreen(CraftingStationContainerMenu container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title);
    addChestSideInventory(playerInventory);
  }

  @Override
  protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
    this.drawBackground(graphics, CRAFTING_TABLE_GUI_TEXTURES);
    super.renderBg(graphics, partialTicks, mouseX, mouseY);
  }
}
