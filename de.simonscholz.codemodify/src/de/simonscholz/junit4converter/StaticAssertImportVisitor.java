package de.simonscholz.junit4converter;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

public class StaticAssertImportVisitor extends ASTVisitor {

	private static final String JUNIT_ASSERT_QUALIFIED_NAME = "org.junit.Assert";

	private static final Set<String> assertMethodNames = new HashSet<>();

	static {
		assertMethodNames.add("assertTrue");
		assertMethodNames.add("assertFalse");
		assertMethodNames.add("fail");
		assertMethodNames.add("assertEquals");
		assertMethodNames.add("assertNotEquals");
		assertMethodNames.add("assertArrayEquals");
		assertMethodNames.add("assertNull");
		assertMethodNames.add("assertNotNull");
		assertMethodNames.add("assertSame");
		assertMethodNames.add("assertNotSame");
		assertMethodNames.add("assertThat");
	}

	private ImportRewrite importRewrite;

	public StaticAssertImportVisitor(ImportRewrite importRewrite) {
		this.importRewrite = importRewrite;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		SimpleName methodName = node.getName();
		String methodInvocationName = methodName.getFullyQualifiedName();
		if (assertMethodNames.contains(methodInvocationName)) {
			importRewrite.addStaticImport(JUNIT_ASSERT_QUALIFIED_NAME,
					methodInvocationName, false);
		}
		return true;
	}
}
