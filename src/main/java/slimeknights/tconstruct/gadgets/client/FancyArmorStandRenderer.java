package slimeknights.tconstruct.gadgets.client;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity.StandType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Renderer for fancy armor stands */
public class FancyArmorStandRenderer extends ArmorStandRenderer {
  private static final Map<StandType, Identifier> TEXTURES = new EnumMap<>(StandType.class);
  static {
    Identifier base = TConstruct.getResource("textures/entity/armorstand/");
    for (StandType type : StandType.values()) {
      TEXTURES.put(type, base.withSuffix(type.name().toLowerCase(Locale.ROOT) + ".png"));
    }
  }

  public FancyArmorStandRenderer(Context context) {
    super(context);
  }

  @Override
  public ArmorStandRenderState createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(ArmorStand entity, ArmorStandRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    // armor and held item layers only skip the baby equipment models for the vanilla armor stand type, so small fancy stands
    // would get baby armor and item offsets. Report the vanilla type so they render like vanilla armor stands.
    state.entityType = EntityTypes.ARMOR_STAND;
    if (entity instanceof FancyArmorStandEntity stand && state instanceof State fancy) {
      fancy.standType = stand.getStandType();
      fancy.clearHidden = stand.isClearHidden();
      if (stand.getStandType().isFullbright()) {
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
      }
    }
  }

  @Override
  public Identifier getTextureLocation(ArmorStandRenderState state) {
    if (state instanceof State fancy) {
      return TEXTURES.get(fancy.standType);
    }
    return super.getTextureLocation(state);
  }

  @Override
  protected boolean isBodyVisible(ArmorStandRenderState state) {
    return !state.isInvisible && !(state instanceof State fancy && fancy.clearHidden);
  }

  /** Render state for fancy armor stands */
  public static class State extends ArmorStandRenderState {
    /** Type of the stand, determines the texture */
    public StandType standType = StandType.values()[0];
    /** If true, the clear stand body is hidden */
    public boolean clearHidden = false;
  }
}
