package de.simonscholz.junit4converter;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

public class StaticAssertImportVisitor extends ASTVisitor {

	private static final String JUNIT_ASSERT_QUALIFIED_NAME = "org.junit.Assert";
	private static final String ASSERT_FAIL_METHOD = "fail";
	private static final String ASSERT_METHOD_PREFIX = "assert";
	private ImportRewrite importRewrite;

	public StaticAssertImportVisitor(ImportRewrite importRewrite) {
		this.importRewrite = importRewrite;

	}

	@Override
	public boolean visit(MethodInvocation node) {
		SimpleName methodName = node.getName();
		String methodInvocationName = methodName.getFullyQualifiedName();
		if (methodInvocationName.startsWith(ASSERT_METHOD_PREFIX)
				|| methodInvocationName.equals(ASSERT_FAIL_METHOD)) {
			importRewrite.addStaticImport(JUNIT_ASSERT_QUALIFIED_NAME,
					methodInvocationName, false);
		}
		return true;
	}
}
