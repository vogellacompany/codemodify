package de.simonscholz;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

public class DefaultCodeModifier extends AbstractCodeModifier {

	private List<ICompilationUnitModifier> modifiers;

	public DefaultCodeModifier(ICompilationUnitModifier... modifiers) {
		this.modifiers = Arrays.asList(modifiers);
	}
	
	public DefaultCodeModifier(List<ICompilationUnitModifier> modifiers) {
		this.modifiers = modifiers;
	}

	@Override
	public void modify(ICompilationUnit cu, IProgressMonitor monitor)
			throws MalformedTreeException, BadLocationException, CoreException {
		// parse compilation unit
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(cu);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(monitor);

		for (ICompilationUnitModifier compilationUnitModifier : modifiers) {
			compilationUnitModifier.modifyCompilationUnit(astRoot, monitor);
		}

	}

	public void addCompilationUnitModifier(
			ICompilationUnitModifier compilationUnitModifier) {
		modifiers.add(compilationUnitModifier);
	}

	public void removeCompilationUnitModifier(
			ICompilationUnitModifier compilationUnitModifier) {
		modifiers.remove(compilationUnitModifier);
	}

}
