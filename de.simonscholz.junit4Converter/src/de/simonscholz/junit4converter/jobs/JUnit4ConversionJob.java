package de.simonscholz.junit4converter.jobs;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.osgi.framework.FrameworkUtil;

import de.simonscholz.junit4converter.JUnit4Converter;

public class JUnit4ConversionJob extends Job {

	private JUnit4Converter jUnit4Converter;
	private List selectedElements;

	public JUnit4ConversionJob(List selectedElements) {
		super("Converting to JUnit 4");
		this.selectedElements = selectedElements;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		try {
			for (Object object : selectedElements) {
				if (object instanceof IJavaProject) {
					getConverter().convert((IJavaProject) object, monitor);
				} else if (object instanceof IPackageFragment) {
					getConverter().convert((IPackageFragment) object, monitor);
				} else if (object instanceof ICompilationUnit) {
					getConverter().convert((ICompilationUnit) object);
				}
			}
		} catch (MalformedTreeException | BadLocationException | CoreException e) {
			e.printStackTrace();
			return new Status(Status.ERROR, FrameworkUtil.getBundle(getClass())
					.getSymbolicName(), e.getLocalizedMessage(), e);
		}
		return Status.OK_STATUS;
	}

	protected JUnit4Converter getConverter() {
		if (null == jUnit4Converter) {
			jUnit4Converter = new JUnit4Converter();
		}
		return jUnit4Converter;
	}

}
