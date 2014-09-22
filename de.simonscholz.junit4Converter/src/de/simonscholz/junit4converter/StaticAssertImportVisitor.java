package de.simonscholz.junit4converter;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

public class StaticAssertImportVisitor extends ASTVisitor {

	private ImportRewrite importRewrite;

	public StaticAssertImportVisitor(ImportRewrite importRewrite) {
		this.importRewrite = importRewrite;

	}

	@Override
	public boolean visit(MethodInvocation node) {
		SimpleName methodName = node.getName();
		String methodInvocationName = methodName.getFullyQualifiedName();
		if (methodInvocationName.startsWith("assert")) {
			importRewrite.addStaticImport("org.junit.Assert",
					methodInvocationName, false);
		}
		return true;
	}
}
