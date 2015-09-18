package de.simonscholz;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.osgi.framework.FrameworkUtil;

public abstract class CodeModifierJob extends Job {

	private List<IJavaElement> selectedElements;

	public CodeModifierJob(List<IJavaElement> selectedElements, String name) {
		super(name);
		this.selectedElements = selectedElements;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, selectedElements.size());
		try {
			for (Object object : selectedElements) {
				if(subMonitor.isCanceled()) {
					return new Status(IStatus.CANCEL, FrameworkUtil.getBundle(getClass())
							.getSymbolicName(), getName() + " has been canceled");
				}
				
				if (object instanceof IJavaProject) {
					getConverter().modify((IJavaProject) object, subMonitor.newChild(1));
				} else if (object instanceof IPackageFragment) {
					getConverter().modify((IPackageFragment) object, subMonitor.newChild(1));
				} else if (object instanceof ICompilationUnit) {
					getConverter().modify((ICompilationUnit) object, subMonitor.newChild(1));
				}
			}
		} catch (MalformedTreeException | BadLocationException | CoreException e) {
			return new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass())
					.getSymbolicName(), e.getLocalizedMessage(), e);
		}
		return Status.OK_STATUS;
	}

	protected abstract ICodeModifier getConverter();

}
