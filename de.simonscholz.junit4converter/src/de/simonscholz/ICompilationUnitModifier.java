package de.simonscholz;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;

public interface ICompilationUnitModifier {

	public void modifyCompilationUnit(CompilationUnit astRoot,
			IProgressMonitor monitor) throws JavaModelException, CoreException, BadLocationException;
}
