package slimeknights.tconstruct.library.modifiers;

import com.google.common.math.IntMath;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

import javax.annotation.Nullable;

/**
 * Handles all the logic for partial levels on tools
 */
public class IncrementalModifierEntry extends ModifierEntry {
  private final int amount;
  @Getter
  private final int needed;
  @Getter
  private final float effectiveLevel;

  protected IncrementalModifierEntry(LazyModifier modifier, int level, int amount, int needed) {
    super(modifier, level);
    this.amount = Math.max(0, amount);
    this.needed = Math.max(0, needed);
    if (amount >= needed) {
      effectiveLevel = level;
    } else {
      effectiveLevel = scaleLevel(level, amount, needed);
    }
  }

  /**
   * Creates a new incremental modifier entry
   * @param modifier  Modifier instance
   * @param level     Modifier level, will be effectively as low as 1 less based on amount
   * @param amount    Amount of progress towards the level
   * @param needed    Amount of progress needed for a full level
   * @return  Either an incremental or a regular modifier entry based on parameters
   */
  public static ModifierEntry of(LazyModifier modifier, int level, int amount, int needed) {
    // If we have a full level, no need to be incremental
    if (needed == 0 || amount >= needed) {
      return new ModifierEntry(modifier, level);
    }
    // no sense saving the needed if amount is 0, thats just the lower level
    if (amount <= 0) {
      return new ModifierEntry(modifier, level - 1);
    }
    return new IncrementalModifierEntry(modifier, level, amount, needed);
  }

  /**
   * Creates a new incremental modifier entry
   * @param modifier  Modifier instance
   * @param level     Modifier level, will be effectively as low as 1 less based on amount
   * @param amount    Amount of progress towards the level
   * @param needed    Amount of progress needed for a full level
   * @return  Either an incremental or a regular modifier entry based on parameters
   */
  public static ModifierEntry of(ModifierId modifier, int level, int amount, int needed) {
    return of(new LazyModifier(modifier), level, amount, needed);
  }

  /**
   * Creates a new incremental modifier entry
   * @param modifier  Modifier instance
   * @param level     Modifier level, will be effectively as low as 1 less based on amount
   * @param amount    Amount of progress towards the level
   * @param needed    Amount of progress needed for a full level, if 0 will be treated as full progress
   * @return  Either an incremental or a regular modifier entry based on parameters
   */
  public static ModifierEntry of(Modifier modifier, int level, int amount, int needed) {
    return of(new LazyModifier(modifier), level, amount, needed);
  }


  /* Amount logic */

  @Override
  public int getAmount(int fallback) {
    return amount;
  }

  @Override
  public int intEffectiveLevel() {
    return level - 1;
  }

  @Override
  public Component getDisplayName() {
    Component name = super.getDisplayName();
    if (amount < needed) {
      return addAmountToName(name, amount, needed);
    }
    return name;
  }


  /* Withers */

  @Override
  public ModifierEntry addAmount(int amount, int needed) {
    // if they gave us no input, no need to create a new instance
    if (needed <= 0 || amount <= 0) {
      return this;
    }
    // if passed in a fulfilled need, then we just completed our level, drop the incremental
    if (amount >= needed) {
      return new ModifierEntry(modifier, level);
    }
    // no full level passed, need to validate the scales
    if (needed == this.needed) {
      // same needed? means we can just sum
      amount += this.amount;
    } else {
      // if they are different, will need to scale both to the LCM
      int gcd = IntMath.gcd(needed, this.needed);
      int newMultiplier = this.needed / gcd;
      amount = (this.amount * needed / gcd) + (amount * newMultiplier);
      needed *= newMultiplier;
    }
    // static constructor will now figure out if that lets us ditch incremental
    return IncrementalModifierEntry.of(modifier, level, amount, needed);
  }

  @Override
  public ModifierEntry withLevel(int level) {
    // we made it to an incremental entry with this amount and needed, so no way we won't via the static consrtuctor
    return new IncrementalModifierEntry(modifier, level, amount, needed);
  }

  @Override
  public ModifierEntry merge(ModifierEntry other) {
    if (!this.getId().equals(other.getId())) {
      throw new IllegalArgumentException("Modifiers do not match, have " + getId() + " but was given " + other.getId());
    }
    // we sum logical level instead of level as if we are both incremental, that will drop the level by 1
    // add amount will automatically no-op if other is not incremental
    return withLevel(level + other.intEffectiveLevel()).addAmount(other.getAmount(0), other.getNeeded());
  }


  /* Serialization */

  @Override
  public CompoundTag serializeToNBT() {
    CompoundTag tag = super.serializeToNBT();
    tag.putInt(TAG_AMOUNT, amount);
    tag.putInt(TAG_NEEDED, needed);
    tag.putFloat(TAG_EFFECTIVE, effectiveLevel);
    return tag;
  }

  /** Object */

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IncrementalModifierEntry entry = (IncrementalModifierEntry)o;
    return this.matches(entry.getId()) && level == entry.level && this.amount == entry.amount && this.needed == entry.needed;
  }

  @Override
  public int hashCode() {
    return 31 * (31 * super.hashCode() + amount) + needed;
  }

  @Override
  public String toString() {
    return "IncrementalModifierEntry{" + modifier.getId() + ",level=" + level + ",amount=" + amount + '/' + needed + '}';
  }


  /* Helpers */

  /** Scales the level based on the given amount and needed amount per level */
  public static float scaleLevel(float level, int amount, int neededPerLevel) {
    // if amount == needed per level, returns level
    if (amount < neededPerLevel) {
      // if amount == 0, returns level - 1, otherwise returns some fractional amount
      return level + (amount - neededPerLevel) / (float)neededPerLevel;
    }
    return level;
  }

  /** Gets the display name for an incremental modifier */
  public static Component addAmountToName(Component name, int amount, int neededPerLevel) {
    return name.copy().append(": " + amount + " / " + neededPerLevel);
  }
}
