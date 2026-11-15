package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.function.Consumer;

/**
 * Effect for rendering the charge up when you start using a helmet.
 * Extends {@link NoMilkEffect} as the effect has no cures (it only tracks the charge), and so the client extensions are registered with the other Tinker effects.
 */
public class HelmetChargingEffect extends NoMilkEffect {
  public HelmetChargingEffect() {
    super(MobEffectCategory.NEUTRAL, -1, true);
  }

  @Override
  public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
    consumer.accept(new IClientMobEffectExtensions() {
      /** Bar sprite, mob effect textures are part of the GUI atlas under the mob_effect prefix */
      private static final Identifier BAR_KEY = TConstruct.getResource("mob_effect/helmet_charging_bar");
      private final Minecraft mc = Minecraft.getInstance();

      @Override
      public boolean isVisibleInInventory(MobEffectInstance instance) {
        return false;
      }

      @Override
      public boolean extractHudIcon(MobEffectInstance instance, Hud hud, GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        // start by drawing the original texture, skip alpha
        // x and y are already offset to the icon position
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(instance.getEffect()), x, y, 18, 18);

        // if the helmet has a drawtime, render the extra bar
        if (mc.player != null) {
          ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
          if (!helmet.isEmpty()) {
            // if we have a drawtime, draw the bar overlaying the icon
            int duration = instance.getDuration();
            int drawtime = ModifierUtil.getPersistentInt(helmet, GeneralInteractionModifierHook.KEY_DRAWTIME, 0);
            int dd = drawtime + 20;
            if (drawtime > 0 && duration < dd) {
              int barHeight;
              if (duration < 20) {
                barHeight = 18;
              } else {
                barHeight = (dd - duration) * 18 / drawtime;
              }
              // draw the bottom part of the bar sprite, growing upwards
              int yOffset = (18 - barHeight);
              graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_KEY, 18, 18, 0, yOffset, x, y + yOffset, 18, barHeight);
            }
          }
        }
        return true;
      }
    });
  }


  /* Helpers */

  /** Starts using the helmet with the charge time rendering */
  public static int startUsingHelmet(IToolStackView tool, LivingEntity living, float speedFactor) {
    int time = GeneralInteractionModifierHook.startDrawing(tool, living, speedFactor);
    // helmetCharging is a DeferredHolder<? super HelmetChargingEffect, HelmetChargingEffect> (wildcard), so wrap the effect instance as a Holder<MobEffect>
    living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TinkerModifiers.helmetCharging.get()), time + 20, 0, true, false, true));
    return time;
  }
}
