package de.simonscholz;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

public interface ICodeModifier {

	public void modify(IJavaProject javaProject,
			IProgressMonitor progressMonitor) throws MalformedTreeException,
			BadLocationException, CoreException;

	public void modify(IPackageFragment packageFragment,
			IProgressMonitor progressMonitor) throws MalformedTreeException,
			BadLocationException, CoreException;

	public void modify(ICompilationUnit cu, IProgressMonitor monitor)
			throws MalformedTreeException, BadLocationException, CoreException;
}
