package de.simonscholz.lambdaconverter.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;

import de.simonscholz.junit4converter.jobs.JUnit4ConversionJob;
import de.simonscholz.lambdaconverter.jobs.LambdaConversionJob;

public class ConvertToLambdaHandler {

	@Execute
	public Object execute(
			@Optional @Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection) {
		if (selection != null && !selection.isEmpty()) {
			LambdaConversionJob job = new LambdaConversionJob(
					selection.toList());
			job.schedule();
		}

		return null;
	}

}
