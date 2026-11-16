package slimeknights.tconstruct.library.tools.stat;

import net.minecraft.network.chat.Component;

/** Tool stat which contains a numeric value */
public interface INumericToolStat<T extends Number> extends IToolStat<T> {
  /**
   * Updates the base value to the passed value. If applied multiple times, consecutive update calls act identical to {@link #add(ModifierStatsBuilder, double)}
   * @param builder  Builder instance
   * @param value    Amount to add
   */
  @Override
  void update(ModifierStatsBuilder builder, T value);

  /**
   * Adds the given value to the stat
   * @param builder  Builder instance
   * @param value    Amount to add
   */
  void add(ModifierStatsBuilder builder, double value);

  /**
   * Adds the given percentage to the tool. If called multiple times, values get summed rather than multiplied together.
   * @param builder  Builder instance
   * @param value    Percentage to add. For example. 0.25 will multiply the final value by 1.25
   */
  void percent(ModifierStatsBuilder builder, double value);

  /**
   * Multiplies the stat by the given value. Multiplication is applied after all addition
   * @param builder  Builder instance
   * @param factor   Amount to multiply. For example. 0.25 will multiply the final value by 0.25
   */
  void multiply(ModifierStatsBuilder builder, double factor);

  /**
   * Multiplies the stat by the given value, both among current stats and all future modifiers.
   * Note that this can have an extreme effect on stats, so use very carefully.
   * @param builder  Builder instance
   * @param factor   Amount to multiply
   */
  void multiplyAll(ModifierStatsBuilder builder, double factor);

  /** Formats the value using this tool stat */
  Component formatValue(float value);


  /** Implementations */

  @Override
  default Component formatValue(T value) {
    return formatValue(value.floatValue());
  }
}
