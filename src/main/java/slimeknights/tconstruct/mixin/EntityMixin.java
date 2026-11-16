package slimeknights.tconstruct.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;

/**
 * Restores projectile modifiers saved by Neo Tinkers for 1.21.1.
 * <p>
 * The modifiers attachment used to be saved as a bare list. 26.3 attachments only read compounds and get no access to the surrounding data,
 * and no NeoForge event exposes the entity save data, so the old list is read once the entity finished loading.
 * Covers every projectile, including vanilla arrows, fireballs and spit that Tinkers' launchers attach modifiers to.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
  @Inject(method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"), require = 0)
  private void tconstruct$readLegacyModifiers(ValueInput input, CallbackInfo ci) {
    EntityModifierCapability.readLegacyModifiers((Entity)(Object)this, input);
  }
}
