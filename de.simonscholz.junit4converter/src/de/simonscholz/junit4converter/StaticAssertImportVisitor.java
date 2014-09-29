package de.simonscholz.junit4converter;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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
			ITypeBinding typeBinding = getTypeBinding(node);

			if (typeBinding != null) {
				// check if the assert method is part of the org.junit.Assert
				// class
				if (JUNIT_ASSERT_QUALIFIED_NAME.equals(typeBinding
						.getQualifiedName())) {
					importRewrite.addStaticImport(JUNIT_ASSERT_QUALIFIED_NAME,
							methodInvocationName, false);
				}
			}
		}
		return true;
	}

	protected ITypeBinding getTypeBinding(MethodInvocation node) {
		Expression expression = node.getExpression();
		if (expression != null) {
			ITypeBinding typeBinding = expression.resolveTypeBinding();
			if (typeBinding != null) {
				return typeBinding;
			}
		}

		IMethodBinding methodBinding = node.resolveMethodBinding();
		if (methodBinding != null) {
			ITypeBinding declaringClass = methodBinding.getDeclaringClass();
			return declaringClass;
		}
		return null;
	}
}
