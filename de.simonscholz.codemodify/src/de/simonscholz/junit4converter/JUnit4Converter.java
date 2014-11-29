package de.simonscholz.junit4converter;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import de.simonscholz.ICompilationUnitModifier;

public class JUnit4Converter implements ICompilationUnitModifier {

	private static final String OVERRIDE_ANNOTATION_NAME = "Override";
	private static final String TEST_ANNOTATION_QUALIFIED_NAME = "org.junit.Test";
	private static final String TEST_ANNOTATION_NAME = "Test";
	private static final String BEFORE_ANNOTATION_QUALIFIED_NAME = "org.junit.Before";
	private static final String BEFORE_ANNOTATION_NAME = "Before";
	private static final String AFTER_ANNOTATION_QUALIFIED_NAME = "org.junit.After";
	private static final String AFTER_ANNOTATION_NAME = "After";

	private static final String TEST_CASE_QUALIFIED_NAME = "junit.framework.TestCase";
	private static final String TEST_CASE_CLASSNAME = "TestCase";

	private static final String SET_UP_METHOD_NAME = "setUp";
	private static final String TEAR_DOWN_METHOD_NAME = "tearDown";

	private static final String TEST_METHOD_PREFIX = "test";

	private boolean modifiedDocument;

	@Override
	public void modifyCompilationUnit(CompilationUnit astRoot,
			IProgressMonitor monitor) throws JavaModelException, CoreException,
			BadLocationException {
		AST ast = astRoot.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		ImportRewrite importRewrite = ImportRewrite.create(astRoot, true);
		modifiedDocument = false;
		JUnit4BisonConverter bisonConverter = new JUnit4BisonConverter(ast,
				rewriter, importRewrite);

		List types = astRoot.types();

		for (Object object : types) {
			if (object instanceof TypeDeclaration) {

				TypeDeclaration typeDeclaration = (TypeDeclaration) object;
				bisonConverter.convert(typeDeclaration);
				modifiedDocument = bisonConverter.wasConverted();

				removeTestCaseSuperclass(rewriter, importRewrite,
						typeDeclaration);

				convertTestMethods(ast, rewriter, importRewrite,
						typeDeclaration);
			}
		}

		if (modifiedDocument) {
			ICompilationUnit adapter = (ICompilationUnit) astRoot
					.getJavaElement().getAdapter(IOpenable.class);
			if (adapter != null) {
				saveChanges(adapter, monitor, rewriter, importRewrite);
			}
		}
	}

	protected void saveChanges(ICompilationUnit cu, IProgressMonitor monitor,
			final ASTRewrite rewriter, ImportRewrite importRewrite)
			throws CoreException, JavaModelException, BadLocationException {
		TextEdit importEdits = importRewrite.rewriteImports(monitor);
		TextEdit edits = rewriter.rewriteAST();
		importEdits.addChild(edits);

		// apply the text edits to the compilation unit
		Document document = new Document(cu.getSource());
		importEdits.apply(document);

		// this is the code for adding statements
		cu.getBuffer().setContents(document.get());
		cu.save(monitor, true);
	}

	protected void convertTestMethods(final AST ast, final ASTRewrite rewriter,
			final ImportRewrite importRewrite, TypeDeclaration typeDeclaration) {
		if (!modifiedDocument) {
			return;
		}
		MethodDeclaration[] methods = typeDeclaration.getMethods();
		for (MethodDeclaration methodDeclaration : methods) {
			SimpleName name = methodDeclaration.getName();
			String fullyQualifiedName = name.getFullyQualifiedName();

			methodDeclaration.accept(new StaticAssertImportVisitor(
					importRewrite));

			if (fullyQualifiedName.toLowerCase().startsWith(TEST_METHOD_PREFIX)) {
				createMarkerAnnotation(ast, rewriter, methodDeclaration,
						TEST_ANNOTATION_NAME);
				importRewrite.addImport(TEST_ANNOTATION_QUALIFIED_NAME);
				modifiedDocument = true;
			} else if (SET_UP_METHOD_NAME.equals(fullyQualifiedName)) {
				removeAnnotation(rewriter, methodDeclaration,
						OVERRIDE_ANNOTATION_NAME);
				createMarkerAnnotation(ast, rewriter, methodDeclaration,
						BEFORE_ANNOTATION_NAME);
				convertProtectedToPublic(ast, rewriter, methodDeclaration);
				importRewrite.addImport(BEFORE_ANNOTATION_QUALIFIED_NAME);
				modifiedDocument = true;
			} else if (TEAR_DOWN_METHOD_NAME.equals(fullyQualifiedName)) {
				removeAnnotation(rewriter, methodDeclaration,
						OVERRIDE_ANNOTATION_NAME);
				createMarkerAnnotation(ast, rewriter, methodDeclaration,
						AFTER_ANNOTATION_NAME);
				convertProtectedToPublic(ast, rewriter, methodDeclaration);
				importRewrite.addImport(AFTER_ANNOTATION_QUALIFIED_NAME);
				modifiedDocument = true;
			}
		}
	}

	protected void removeTestCaseSuperclass(ASTRewrite rewriter,
			ImportRewrite importRewrite, TypeDeclaration typeDeclaration) {
		Type superclassType = typeDeclaration.getSuperclassType();
		if (superclassType != null && superclassType.isSimpleType()) {
			SimpleType superType = (SimpleType) superclassType;
			if (TEST_CASE_CLASSNAME.equals(superType.getName()
					.getFullyQualifiedName())) {
				rewriter.remove(superType, null);
				importRewrite.removeImport(TEST_CASE_QUALIFIED_NAME);
				modifiedDocument = true;
			}
		}
	}

	protected void convertProtectedToPublic(final AST ast,
			final ASTRewrite rewriter, MethodDeclaration methodDeclaration) {
		List modifiers = methodDeclaration.modifiers();
		for (Object object2 : modifiers) {
			if (object2 instanceof Modifier) {
				Modifier modifier = (Modifier) object2;
				if (ModifierKeyword.PROTECTED_KEYWORD.equals(modifier
						.getKeyword())) {
					Modifier publicModifier = ast
							.newModifier(ModifierKeyword.PUBLIC_KEYWORD);
					rewriter.replace(modifier, publicModifier, null);
				}
			}
		}
	}

	protected void createMarkerAnnotation(AST ast, ASTRewrite rewriter,
			MethodDeclaration methodDeclaration, String annotationName) {
		if (!isAnnotationExisting(methodDeclaration.modifiers(), annotationName)) {
			MarkerAnnotation testAnnotation = ast.newMarkerAnnotation();
			testAnnotation.setTypeName(ast.newName(annotationName));

			ListRewrite listRewrite = rewriter.getListRewrite(
					methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(testAnnotation, null);
		}
	}

	protected void removeAnnotation(ASTRewrite rewriter,
			MethodDeclaration methodDeclaration, String annotationName) {
		List modifiers = methodDeclaration.modifiers();
		for (Object object : modifiers) {
			if (object instanceof Annotation) {
				Annotation annotation = (Annotation) object;
				Name typeName = annotation.getTypeName();
				if (annotationName.equals(typeName.getFullyQualifiedName())) {
					rewriter.remove(annotation, null);
				}
			}
		}
	}

	protected boolean isAnnotationExisting(List modifiers, String annotationName) {
		for (Object modifier : modifiers) {
			if (modifier instanceof Annotation) {
				Annotation markerAnnotation = (Annotation) modifier;
				Name typeName = markerAnnotation.getTypeName();
				String fullyQualifiedName = typeName.getFullyQualifiedName();
				return annotationName.equals(fullyQualifiedName);
			}
		}
		return false;
	}
}
