package slimeknights.tconstruct.library.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Armor model that wraps another armor model */
public class ArmorModelHelper {
  /** Cache of armor render types */
  private static final Map<ResourceLocation,RenderType> RENDER_CACHE = new HashMap<>();
  /** Function to get armor render type */
  private static final Function<ResourceLocation,RenderType> CACHE_GETTER = RenderType::entityCutoutNoCullZOffset;
  /** Listener to clear render type cache */
  @Internal
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> {
    RENDER_CACHE.clear();
  };

  /** Buffer from the render living event, stored as we lose access to it later */
  @Nullable
  public static MultiBufferSource buffer;

  /** Iniitalizes the wrapper */
  public static void init() {
    // register listeners to set and clear the buffer
    MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderLivingEvent.Pre.class, event -> buffer = event.getMultiBufferSource());
    MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderLivingEvent.Post.class, event -> buffer = null);
  }

  /** Gets a render type for the given texture */
  public static RenderType getRenderType(ResourceLocation texture) {
    return RENDER_CACHE.computeIfAbsent(texture, CACHE_GETTER);
  }

  /** Handles the unchecked cast to copy entity model properties */
  @SuppressWarnings("unchecked")
  public static <T extends LivingEntity> void copyProperties(EntityModel<T> base, EntityModel<?> other) {
    base.copyPropertiesTo((EntityModel<T>)other);
  }
}
