package slimeknights.tconstruct.shared;

import net.minecraft.client.gui.Font;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.client.model.UniqueGuiModel;
import slimeknights.tconstruct.library.utils.DomainDisplayName;
import slimeknights.tconstruct.shared.client.FluidParticle;

@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class CommonsClientEvents extends ClientEventBase {
  @SubscribeEvent
  static void addResourceListeners(RegisterClientReloadListenersEvent event) {
    DomainDisplayName.addResourceListener(event);
  }

  @SubscribeEvent
  static void registerModelLoaders(RegisterGeometryLoaders event) {
    event.register(TConstruct.getResource("gui"), UniqueGuiModel.LOADER);
  }

  @SubscribeEvent
  static void clientSetup(final FMLClientSetupEvent event) {
    Font unicode = BookScreen.getUniformFont();
    TinkerBook.MATERIALS_AND_YOU.fontRenderer = unicode;
    TinkerBook.TINKERS_GADGETRY.fontRenderer = unicode;
    TinkerBook.PUNY_SMELTING.fontRenderer = unicode;
    TinkerBook.MIGHTY_SMELTING.fontRenderer = unicode;
    TinkerBook.FANTASTIC_FOUNDRY.fontRenderer = unicode;
    TinkerBook.ENCYCLOPEDIA.fontRenderer = unicode;
  }

  @SubscribeEvent
  static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    event.registerSpecial(TinkerCommons.fluidParticle.get(), new FluidParticle.Factory());
  }

  /** @deprecated use {@link BookScreen#getUniformFont()} */
  @Deprecated(forRemoval = true)
  public static Font unicodeFontRender() {
    return BookScreen.getUniformFont();
  }
}
