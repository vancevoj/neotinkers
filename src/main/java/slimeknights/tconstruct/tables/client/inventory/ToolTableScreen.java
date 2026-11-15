package slimeknights.tconstruct.tables.client.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.util.ModifierTooltip;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TinkerTooltipFlags;
import slimeknights.tconstruct.tables.client.inventory.module.InfoPanelScreen;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Shared logic for the tinker station and the modifier worktable */
public abstract class ToolTableScreen<T extends BlockEntity, C extends TabbedContainerMenu<T>> extends BaseTabbedScreen<T,C> {
  private static final Component MODIFIERS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.modifiers");
  private static final Component UPGRADES_TEXT = TConstruct.makeTranslation("gui", "tinker_station.upgrades");
  private static final Component TRAITS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.traits");

  private static final Identifier ICON_TEXTURE = TConstruct.getResource("textures/gui/icons.png");

  /** Side panels, for tools and modifiers */
  protected final InfoPanelScreen<ToolTableScreen<T,C>,C> tinkerInfo;
  protected final InfoPanelScreen<ToolTableScreen<T,C>,C> modifierInfo;

  protected final Player player;
  @Nullable
  protected ArmorStand armorStandPreview;
  protected boolean enableArmorStandPreview = true;
  protected boolean clickedOnArmorStand = false;

  protected int armorStandX = 0;
  protected int armorStandY = 0;
  protected int armorStandScale = 10;
  protected float armorStandAngle = 0;
  protected double armorStandLastMouseX = -1;

  public ToolTableScreen(C c, Inventory playerInventory, Component title) {
    super(c, playerInventory, title);
    this.player = playerInventory.player;

    this.tinkerInfo = new InfoPanelScreen<>(this, c, playerInventory, title);
    this.tinkerInfo.setTextScale(8/9f);
    this.addModule(this.tinkerInfo);

    this.modifierInfo = new InfoPanelScreen<>(this, c, playerInventory, title);
    this.modifierInfo.setTextScale(7/9f);
    this.addModule(this.modifierInfo);
  }

  @Override
  protected void init() {
    super.init();
    if (enableArmorStandPreview) {
      assert this.minecraft != null;
      assert this.minecraft.level != null;
      this.armorStandPreview = new ArmorStand(this.minecraft.level, 0.0D, 0.0D, 0.0D);
      this.armorStandPreview.setNoBasePlate(true);
      this.armorStandPreview.setShowArms(true);
      this.armorStandPreview.yBodyRot = 210.0F;
      this.armorStandPreview.setXRot(25.0F);
      this.armorStandPreview.yHeadRot = this.armorStandPreview.getYRot();
      this.armorStandPreview.yHeadRotO = this.armorStandPreview.getYRot();
    }
  }

  /** Base armor stand orientation, matching vanilla {@code SmithingScreen.ARMOR_STAND_ANGLE} (private in 1.21) */
  private static final Quaternionf ARMOR_STAND_ANGLE = new Quaternionf().rotationXYZ(0.43633232f, 0.0f, 3.1415927f);
  /**
   * Translation from the center of the entity preview area to the feet of the armor stand, in blocks (scaled by the stand size).
   * The preview area below is centered one block above the feet, same as vanilla {@code SmithingScreen.ARMOR_STAND_TRANSLATION}.
   */
  private static final Vector3fc ARMOR_STAND_TRANSLATION = new Vector3f(0.0f, 1.0f, 0.0f);

  /**
   * Renders the armor stand
   * @param graphics  Graphics instance
   */
  protected void renderArmorStand(GuiGraphicsExtractor graphics) {
    if (this.armorStandPreview != null) {
      // 26.3: drawn first as entities are drawn into a separate texture layered by draw order. Before, the entity used depth
      // to render in front of the platform icon drawn after it.
      graphics.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, armorStandX - 16, armorStandY - 16, 0, 184, 32, 32, 256, 256);

      // 26.3: entities render as picture in picture, centered in the given area instead of at a point.
      // The area is centered one block above the feet (offset by the translation), and is large enough to hold the stand
      // and held items in any rotation (1.5 blocks to each side, 2.5 blocks above the feet and half a block below).
      Quaternionf pose = new Quaternionf(ARMOR_STAND_ANGLE).rotateY(this.armorStandAngle);
      EntityRenderState renderState = this.minecraft.getEntityRenderDispatcher().getRenderer(this.armorStandPreview).createRenderState(this.armorStandPreview, 1.0F);
      renderState.shadowPieces.clear();
      renderState.outlineColor = 0;
      int scale = this.armorStandScale;
      graphics.entity(renderState, scale, ARMOR_STAND_TRANSLATION, pose, null,
                      this.armorStandX - scale * 3 / 2, this.armorStandY - scale * 5 / 2,
                      this.armorStandX + scale * 3 / 2, this.armorStandY + scale / 2);
    }
  }

 /**
  * Setup clickable areas and position for the armor stand
  * @param x         Stand X position
  * @param y         Stand Y position
  * @param scale     Stand size
  */
  protected void setupArmorStandPreview(int x, int y, int scale) {
    this.armorStandX = this.cornerX + x;
    this.armorStandY = this.cornerY + y;
    this.armorStandScale = scale;
  }

  /** Updates the item displayed on the armor stand */
  protected void updateArmorStandPreview(ItemStack stack) {
    if (this.armorStandPreview != null) {
      for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
        this.armorStandPreview.setItemSlot(equipmentslot, ItemStack.EMPTY);
      }

      if (!stack.isEmpty()) {
        ItemStack copy = stack.copy();
        // 1.21.5 removed ArmorItem, armor is any item equippable in a humanoid armor slot
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
          this.armorStandPreview.setItemSlot(equippable.slot(), copy);
        } else {
          this.armorStandPreview.setItemSlot(EquipmentSlot.OFFHAND, copy);
        }
      }

    }
  }

  /** Updates the tool panel area */
  protected void updateToolPanel(LazyToolStack lazyToolStack) {
    ToolStack tool = lazyToolStack.getTool();
    if (tool.getItem() instanceof ITinkerStationDisplay display) {
      tinkerInfo.setCaption(display.getLocalizedName());
      tinkerInfo.setText(display.getStatInformation(tool, Minecraft.getInstance().player, new ArrayList<>(), SafeClientAccess.getTooltipKey(), TinkerTooltipFlags.TINKER_STATION));
    }
    else {
      ItemStack result = lazyToolStack.getStack();
      tinkerInfo.setCaption(result.getHoverName());
      List<Component> list = new ArrayList<>();
      result.getItem().appendHoverText(result, Item.TooltipContext.of(Minecraft.getInstance().level), result.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT), list::add, Default.NORMAL);
      tinkerInfo.setText(list);
    }
  }

  /** Updates the modifier panel with relevant info */
  protected void updateModifierPanel(ToolStack tool) {
    RegistryAccess access = player.level().registryAccess();
    List<Component> modifierNames = new ArrayList<>();
    List<Component> modifierTooltip = new ArrayList<>();
    Component title;
    // control displays just traits, bit trickier to do
    if (this.minecraft.hasControlDown()) {
      title = TRAITS_TEXT;
      Map<Modifier,Integer> upgrades = tool.getUpgrades().getModifiers().stream()
                                           .collect(Collectors.toMap(ModifierEntry::getModifier, ModifierEntry::getLevel, Integer::sum));
      for (ModifierEntry entry : tool.getModifierList()) {
        Modifier mod = entry.getModifier();
        if (mod.shouldDisplay(ModifierTooltip.TINKER_STATION)) {
          int level = entry.getLevel() - upgrades.getOrDefault(mod, 0);
          if (level > 0) {
            ModifierEntry trait = new ModifierEntry(entry.getModifier(), level);
            modifierNames.add(mod.getDisplayName(tool, trait, access));
            modifierTooltip.add(mod.getDescription(tool, trait));
          }
        }
      }
    } else {
      // shift is just upgrades/abilities, otherwise all
      List<ModifierEntry> modifiers;
      if (this.minecraft.hasShiftDown()) {
        modifiers = tool.getUpgrades().getModifiers();
        title = UPGRADES_TEXT;
      } else {
        modifiers = tool.getModifierList();
        title = MODIFIERS_TEXT;
      }
      for (ModifierEntry entry : modifiers) {
        Modifier mod = entry.getModifier();
        if (mod.shouldDisplay(ModifierTooltip.TINKER_STATION)) {
          modifierNames.add(mod.getDisplayName(tool, entry, access));
          modifierTooltip.add(mod.getDescription(tool, entry));
        }
      }
    }

    modifierInfo.setCaption(title);
    modifierInfo.setText(modifierNames, modifierTooltip);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    int armorStandBoxW = this.armorStandScale + 30;
    int armorStandBoxH = this.armorStandScale * 2;
    int armorStandBoxX = this.armorStandX - armorStandBoxW / 2;
    int armorStandBoxY = this.armorStandY - armorStandBoxH + 5;
    this.clickedOnArmorStand = this.enableArmorStandPreview && GuiUtil.isHovered((int) event.x(), (int) event.y(), armorStandBoxX, armorStandBoxY, armorStandBoxW, armorStandBoxH);

    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    this.clickedOnArmorStand = false;
    this.armorStandLastMouseX = -1;

    return super.mouseReleased(event);
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
    double mouseX = event.x();
    if (this.enableArmorStandPreview && this.clickedOnArmorStand && this.armorStandLastMouseX != -1) {
      this.armorStandAngle += (float) (mouseX - this.armorStandLastMouseX) / 10f;
    }
    this.armorStandLastMouseX = mouseX;

    return super.mouseDragged(event, dragX, dragY);
  }
}
