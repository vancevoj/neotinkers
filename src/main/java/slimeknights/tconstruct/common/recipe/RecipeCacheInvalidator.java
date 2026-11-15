package slimeknights.tconstruct.common.recipe;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.data.listener.IEarlySafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class that handles notifying recipe caches that they need to invalidate.
 * <p>
 * Since 26.3 recipes are a reloadable registry: on the server they are parsed (in parallel) before any reload listener runs,
 * and on the client they are decoded before {@link net.neoforged.neoforge.client.event.RecipesReceivedEvent} fires.
 * Both {@link #reload(boolean)} calls therefore happen at the end of a recipe list, see {@link DuelSidedListener} for how caches filled by recipe constructors respond.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeCacheInvalidator implements IEarlySafeManagerReloadListener {
  private static final RecipeCacheInvalidator INSTANCE = new RecipeCacheInvalidator();
  /** Listeners are added from static initializers, which may run on the threads parsing recipes */
  private static final List<BooleanConsumer> listeners = new CopyOnWriteArrayList<>();

  /**
   * Adds a new listener that runs every time the recipes are reloaded
   * @param runnable  Runnable accepting a boolean representing if this is client side
   */
  public static void addReloadListener(BooleanConsumer runnable) {
    listeners.add(runnable);
  }

  /**
   * Registers a listener that properly responds to the client side
   * @param runnable  Runnable to clear cache
   * @return  Object that can clear cache as needed
   */
  public static DuelSidedListener addDuelSidedListener(Runnable runnable) {
    DuelSidedListener listener = new DuelSidedListener(runnable);
    addReloadListener(listener);
    return listener;
  }

  /**
   * Checks if a recipe registering itself into a static cache from its constructor should skip it. In singleplayer and on a LAN host, the
   * integrated client decodes its own copy of every recipe the server already registered (the in memory connection encodes packets since
   * 1.20.5), which would register everything twice and mutate the caches from the netty thread while the server reads them.
   * Dedicated clients have no server in the JVM, so they still register the recipes they decode.
   * @return  True if the recipe should not register
   */
  public static boolean skipRegistration() {
    return RecipeHelper.isNetworkDecode() && ServerLifecycleHooks.getCurrentServer() != null;
  }

  /**
   * Reloads all listeners. Called with false by the server reload listener, and with true when the client receives recipes.
   * @param client  If true, this is the client receiving recipes
   */
  public static void reload(boolean client) {
    for (BooleanConsumer runnable : listeners) {
      runnable.accept(client);
    }
  }

  @Override
  public void onReloadSafe(ResourceManager resourceManager) {
    reload(false);
  }

  /**
   * Called when resource managers reload. Recipes are loaded before reload listeners run, so no ordering against recipes is needed.
   * @param event  Reload event
   */
  public static void onReloadListenerReload(AddServerReloadListenersEvent event) {
    event.addListener(TConstruct.getResource("recipe_cache_invalidator"), INSTANCE);
  }

  /**
   * Logic for caches filled by recipe constructors.
   * <p>
   * In 1.21.1 the server listener ran before recipes were parsed, so it could clear the cache directly. Since 26.3 recipes are parsed before reload listeners
   * on the server, and the client receives recipes before {@link net.neoforged.neoforge.client.event.RecipesReceivedEvent}, so both sides notify this listener
   * at the end of a recipe list. The clear is queued and runs on the first registration of the next recipe list, see {@link #checkClear()}.
   * If nothing registered since the last reload, the new recipe list has no entries for this cache, so the cache is cleared right away.
   * <p>
   * All methods are synchronized, as the server parses recipes in parallel.
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class DuelSidedListener implements BooleanConsumer {
    private final Runnable clearCache;
    /** If true, the cache holds a finished recipe list, so the next registration starts a new one */
    private boolean clearQueued = false;

    @Override
    public synchronized void accept(boolean client) {
      // the integrated client does not register its copy of the recipes (see skipRegistration), the cache belongs to the integrated server
      if (client && ServerLifecycleHooks.getCurrentServer() != null) {
        return;
      }
      // still queued means nothing registered during this recipe load, so the contents are left over from the previous one
      if (clearQueued) {
        clearCache.run();
      }
      // queue clearing to run at the beginning of the next recipe list
      clearQueued = true;
    }

    /**
     * Clears the cache based on the runnable
     */
    public synchronized void clearCache() {
      clearQueued = false;
      clearCache.run();
    }

    /**
     * Clears the cache if a clear is queued. Intended to be called during add
     */
    public synchronized void checkClear() {
      if (clearQueued) {
        clearCache();
      }
    }
  }
}
