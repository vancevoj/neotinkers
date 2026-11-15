package slimeknights.tconstruct.library.modifiers.fluid;

/**
 * Simulate or execute flag for fluid effects and the internal tool fluid hooks.
 * Replaces NeoForge's removed {@code IFluidHandler.FluidAction} with the same constants and methods, so ports only swap the import.
 * Fluid effects change the world directly (damage, blocks, mob effects), so they cannot be rolled back by a transfer transaction.
 * Use this flag instead of a {@code TransactionContext} wherever "simulate" means "make no changes at all".
 */
public enum FluidAction {
  /** Make no changes, just compute the result */
  SIMULATE,
  /** Apply the changes */
  EXECUTE;

  /** If true, changes should be applied */
  public boolean execute() {
    return this == EXECUTE;
  }

  /** If true, no changes should be applied */
  public boolean simulate() {
    return this == SIMULATE;
  }
}
