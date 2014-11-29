package de.simonscholz.junit4converter;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

public class JUnit4BisonConverter {

	private static final String NODB_TESTRULES_PROVIDER = "NoDBTestRulesProvider";
	private static final String NODB_TESTRULES_PROVIDER_QUALIFIEDNAME = "com.foo.res.NoDBTestRulesProvider";
	private static final String NODB_TESTCASE_CLASSNAME = "NoDBTestCase";
	private static final String NODB_TESTCASE_QUALIFIEDNAME = "com.foo.res.NoDBTestCase";

	private final ImportRewrite importRewriter;
	private final ASTRewrite rewriter;
	private final AST ast;
	private boolean _wasModified;

	JUnit4BisonConverter(AST ast, ASTRewrite rewriter,
			ImportRewrite importRewriter) {
		this.ast = ast;
		this.rewriter = rewriter;
		this.importRewriter = importRewriter;
	}

	void convert(TypeDeclaration typeDeclaration) {
		convertNoDBTestCase(typeDeclaration.getSuperclassType());
	}

	boolean wasConverted() {
		return _wasModified;
	}

	private void convertNoDBTestCase(Type superclassType) {
		if (superclassType != null && superclassType.isSimpleType()) {
			SimpleType superType = (SimpleType) superclassType;
			if (NODB_TESTCASE_CLASSNAME.equals(superType.getName()
					.getFullyQualifiedName())) {
				SimpleType newNoDBTestRulesProviderSuperType = ast
						.newSimpleType(ast
								.newSimpleName(NODB_TESTRULES_PROVIDER));
				rewriter.replace(superType, newNoDBTestRulesProviderSuperType,
						null);
				importRewriter.removeImport(NODB_TESTCASE_QUALIFIEDNAME);
				importRewriter.addImport(NODB_TESTRULES_PROVIDER_QUALIFIEDNAME);
				_wasModified = true;
			}
		}

	}
}
