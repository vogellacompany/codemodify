package de.simonscholz.lambdaconverter.cleanup;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

public class LambdaConverterOnSaveOptionsInitializer implements ICleanUpOptionsInitializer {

	@Override
	public void setDefaultOptions(CleanUpOptions options) {
		options.setOption(LambdaConverterCleanUp.CLEANUP_CONVERT_TO_LAMBDA, CleanUpOptions.TRUE);
	}
}