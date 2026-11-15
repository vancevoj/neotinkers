package slimeknights.tconstruct.library.client.model;

import lombok.extern.log4j.Log4j2;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import slimeknights.mantle.data.listener.ResourceValidator;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Logic to handle dynamic texture scans. Really just logging missing textures at this point.
 */
@Log4j2
public class DynamicTextureLoader extends ResourceValidator {
  /** Instance to register with the loader */
  private static final DynamicTextureLoader INSTANCE = new DynamicTextureLoader();
  /** ID of the reload listener */
  public static final Identifier ID = TConstruct.getResource("dynamic_texture_loader");

  private DynamicTextureLoader() {
    super("textures/item", "textures", ".png");
  }

  @Override
  public void onReloadSafe(ResourceManager manager) {
    // if we are logging missing textures we can use the vanilla validator instead of needing our own
    if (!Config.CLIENT.logMissingModifierTextures.get()) {
      super.onReloadSafe(manager);
    }
  }

  @Override
  public void prepareSharedState(SharedState state) {
    // load before any listener reloads, ensures the list is ready before models bake
    if (!ModLoader.hasErrors()) {
      onReloadSafe(state.resourceManager());
    }
  }

  @Override
  public CompletableFuture<Void> reload(SharedState state, Executor backgroundExecutor, PreparationBarrier barrier, Executor gameExecutor) {
    // already loaded in shared state, clear once models finished baking to save memory
    return barrier.wait(Unit.INSTANCE).thenRunAsync(this::clear, gameExecutor);
  }

  /** Registers this manager */
  public static void init(AddClientReloadListenersEvent event) {
    event.addListener(ID, INSTANCE);
  }

  /**
   * Gets a consumer to add textures to the given collection
   *
   * @param spriteGetter        Function mapping material names to sprites
   * @param logMissingTextures  If true, log textures that were not found
   * @return  Texture consumer
   */
  public static Predicate<Material> getTextureValidator(Function<Material,TextureAtlasSprite> spriteGetter, boolean logMissingTextures) {
    if (logMissingTextures || INSTANCE.resources.isEmpty()) {
      // this logs due to the vanilla sprite getter logging
      return mat -> !MissingTextureAtlasSprite.getLocation().equals(spriteGetter.apply(mat).contents().name());
    } else {
      return mat -> {
        // to suppress logging, need to load from our own list. We just load it for `textures/item`, the material baker picks the atlas
        Identifier texture = mat.sprite();
        if (texture.getPath().startsWith("item/")) {
          return INSTANCE.test(texture);
        }
        // failed preconditions? can't stop logging even if the boolean says to
        return !MissingTextureAtlasSprite.getLocation().equals(spriteGetter.apply(mat).contents().name());
      };
    }
  }
}
