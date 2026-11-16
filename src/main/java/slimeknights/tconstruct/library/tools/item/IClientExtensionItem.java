package slimeknights.tconstruct.library.tools.item;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Consumer;

/**
 * Interface for items that provide client item extensions.
 * NeoForge 26.3 removed {@code Item#initializeClient}, so client extensions are registered in {@link RegisterClientExtensionsEvent}.
 * The client event handler calls {@link #initializeClient(Consumer)} for every registered item implementing this interface.
 */
public interface IClientExtensionItem {
  /**
   * Called on the client to register client item extensions for this item. Same contract as the removed {@code Item#initializeClient}.
   * @param consumer  Consumer to pass the extension instance
   */
  default void initializeClient(Consumer<IClientItemExtensions> consumer) {}
}
