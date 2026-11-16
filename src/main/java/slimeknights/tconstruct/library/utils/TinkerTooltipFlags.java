package slimeknights.tconstruct.library.utils;

import net.minecraft.world.item.TooltipFlag;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;

/** Pseudo-copy of the vanilla flag, extended with screen context */
public enum TinkerTooltipFlags implements TooltipFlag {
  /** Display in a station screen */
  TINKER_STATION;

  @Override
  public boolean isAdvanced() {
    return true;
  }

  @Override
  public boolean isCreative() {
    return false;
  }

  /* Modifier keys are part of the flag since 1.21.11, report the client state like the NeoForge client flag does */

  @Override
  public boolean hasShiftDown() {
    return SafeClientAccess.getTooltipKey() == TooltipKey.SHIFT;
  }

  @Override
  public boolean hasControlDown() {
    return SafeClientAccess.getTooltipKey() == TooltipKey.CONTROL;
  }

  @Override
  public boolean hasAltDown() {
    return SafeClientAccess.getTooltipKey() == TooltipKey.ALT;
  }
}
