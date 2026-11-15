package slimeknights.tconstruct.world.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.world.item.ItemStack;

/** Render state for Tinkers slimes, adds the data needed by {@link SlimeArmorLayer} and the metal texture */
public class ArmoredSlimeRenderState extends SlimeRenderState {
  /** Helmet worn by the slime, a copy of the stack in the head slot */
  public ItemStack helmet = ItemStack.EMPTY;
  /** If true, the slime uses its metal texture */
  public boolean metal = false;
  /** State used to pose the humanoid armor model drawn for helmets. Stays in its default pose like the old copied model properties */
  public final HumanoidRenderState armorState = new HumanoidRenderState();
}
