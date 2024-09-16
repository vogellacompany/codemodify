package de.simonscholz.nonjavadoc.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;

import de.simonscholz.nonjavadoc.jobs.NonJavaDocJob;
import jakarta.inject.Named;

public class NonJavaDocRemoverHandler {

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection) {
		if (selection != null && !selection.isEmpty()) {
			NonJavaDocJob job = new NonJavaDocJob(selection.toList());
			job.schedule();
		}

	}

}
