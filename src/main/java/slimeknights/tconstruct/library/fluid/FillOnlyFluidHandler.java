package slimeknights.tconstruct.library.fluid;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Fluid handler wrapper that only allows filling
 */
public class FillOnlyFluidHandler extends DelegatingResourceHandler<FluidResource> {
	public FillOnlyFluidHandler(ResourceHandler<FluidResource> parent) {
		super(parent);
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		// matches the old handler, which reported no fluid as valid while still accepting fills
		return false;
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return 0;
	}

	@Override
	public int extract(FluidResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return 0;
	}
}
