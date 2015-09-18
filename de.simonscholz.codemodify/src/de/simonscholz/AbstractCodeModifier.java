package de.simonscholz;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

public abstract class AbstractCodeModifier implements ICodeModifier {

	@Override
	public void modify(IJavaProject javaProject,
			IProgressMonitor monitor) throws MalformedTreeException,
			BadLocationException, CoreException {
		IPackageFragment[] packageFragments = javaProject.getPackageFragments();
		SubMonitor subMonitor = SubMonitor.convert(monitor, packageFragments.length);
		subMonitor.beginTask(
				"Converting JavaProject " + javaProject.getElementName(),
				packageFragments.length);
		for (IPackageFragment packageFragment : packageFragments) {
			modify(packageFragment, subMonitor.newChild(1));
		}
	}

	@Override
	public void modify(IPackageFragment packageFragment,
			IProgressMonitor progressMonitor) throws MalformedTreeException,
			BadLocationException, CoreException {
		ICompilationUnit[] compilationUnits = packageFragment
				.getCompilationUnits();
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor, compilationUnits.length);
		subMonitor.beginTask("Converting CompilationsUnits of package"
				+ packageFragment.getElementName(), compilationUnits.length);
		for (ICompilationUnit compilationUnit : compilationUnits) {
			modify(compilationUnit, subMonitor.newChild(1));
		}
	}

}
