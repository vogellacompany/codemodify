package de.simonscholz;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

public abstract class AbstractCodeModifier implements ICodeModifier {

	@Override
	public void modify(IJavaProject javaProject,
			IProgressMonitor progressMonitor) throws MalformedTreeException,
			BadLocationException, CoreException {
		IPackageFragment[] packageFragments = javaProject.getPackageFragments();
		progressMonitor.beginTask(
				"Converting JavaProject " + javaProject.getElementName(),
				packageFragments.length);
		AtomicInteger atomicInteger = new AtomicInteger();
		for (IPackageFragment packageFragment : packageFragments) {
			modify(packageFragment, progressMonitor);
			progressMonitor.worked(atomicInteger.incrementAndGet());
		}
	}

	@Override
	public void modify(IPackageFragment packageFragment,
			IProgressMonitor progressMonitor) throws MalformedTreeException,
			BadLocationException, CoreException {
		ICompilationUnit[] compilationUnits = packageFragment
				.getCompilationUnits();
		progressMonitor.beginTask("Converting CompilationsUnits of package"
				+ packageFragment.getElementName(), compilationUnits.length);
		AtomicInteger atomicInteger = new AtomicInteger();
		for (ICompilationUnit compilationUnit : compilationUnits) {
			modify(compilationUnit, progressMonitor);
			progressMonitor.worked(atomicInteger.incrementAndGet());
		}
	}

}
