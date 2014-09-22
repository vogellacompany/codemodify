package de.simonscholz.junit4converter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
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
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class JUnit4Converter {

	private static final String TEST_CASE_CLASSNAME = "TestCase";
	private static final String SET_UP_METHOD_NAME = "setUp";
	private static final String TEAR_DOWN_METHOD_NAME = "tearDown";
	private static final String TEST_ANNOTATION_NAME = "Test";
	private static final String BEFORE_ANNOTATION_NAME = "Before";
	private static final String AFTER_ANNOTATION_NAME = "After";
	private static final String TEST_METHOD_PREFIX = "test";
	private boolean modifiedDocument;

	public void convert(IJavaProject javaProject,
			IProgressMonitor progressMonitor) throws MalformedTreeException,
			BadLocationException, CoreException {
		IPackageFragment[] packageFragments = javaProject.getPackageFragments();
		progressMonitor.beginTask(
				"Converting JavaProject " + javaProject.getElementName(),
				packageFragments.length);
		AtomicInteger atomicInteger = new AtomicInteger();
		for (IPackageFragment packageFragment : packageFragments) {
			convert(packageFragment, progressMonitor);
			progressMonitor.worked(atomicInteger.incrementAndGet());
		}
	}

	public void convert(IPackageFragment packageFragment,
			IProgressMonitor progressMonitor) throws MalformedTreeException,
			BadLocationException, CoreException {
		ICompilationUnit[] compilationUnits = packageFragment
				.getCompilationUnits();
		progressMonitor.beginTask("Converting CompilationsUnits of package"
				+ packageFragment.getElementName(), compilationUnits.length);
		AtomicInteger atomicInteger = new AtomicInteger();
		for (ICompilationUnit compilationUnit : compilationUnits) {
			convert(compilationUnit, progressMonitor);
			progressMonitor.worked(atomicInteger.incrementAndGet());
		}
	}

	public void convert(ICompilationUnit cu, IProgressMonitor monitor) throws MalformedTreeException,
			BadLocationException, CoreException, BadLocationException {

		// parse compilation unit
		final ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(cu);
		final CompilationUnit astRoot = (CompilationUnit) parser
				.createAST(null);
		// create a ASTRewrite
		final AST ast = astRoot.getAST();
		final ASTRewrite rewriter = ASTRewrite.create(ast);
		ImportRewrite importRewrite = ImportRewrite.create(astRoot, true);
		modifiedDocument = false;

		List types = astRoot.types();

		for (Object object : types) {
			if (object instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) object;

				removeTestCaseSuperclass(rewriter, importRewrite,
						typeDeclaration);

				convertTestMethods(ast, rewriter, importRewrite,
						typeDeclaration);
			}
		}

		saveChanges(cu, monitor, rewriter, importRewrite);
	}

	protected void saveChanges(ICompilationUnit cu, IProgressMonitor monitor,
			final ASTRewrite rewriter, ImportRewrite importRewrite)
			throws CoreException, JavaModelException, BadLocationException {
		if (modifiedDocument) {
			TextEdit importEdits = importRewrite.rewriteImports(monitor);
			TextEdit edits = rewriter.rewriteAST();
			importEdits.addChild(edits);

			// apply the text edits to the compilation unit
			Document document = new Document(cu.getSource());
			importEdits.apply(document);
			// edits.apply(document);

			// this is the code for adding statements
			cu.getBuffer().setContents(document.get());
			cu.save(null, true);
		}
	}

	protected void convertTestMethods(final AST ast, final ASTRewrite rewriter,
			final ImportRewrite importRewrite, TypeDeclaration typeDeclaration) {
		MethodDeclaration[] methods = typeDeclaration.getMethods();
		for (MethodDeclaration methodDeclaration : methods) {
			SimpleName name = methodDeclaration.getName();
			String fullyQualifiedName = name.getFullyQualifiedName();
			if (fullyQualifiedName.toLowerCase().startsWith(
					TEST_METHOD_PREFIX)) {
				createMarkerAnnotation(ast, rewriter,
						methodDeclaration, TEST_ANNOTATION_NAME);
				importRewrite.addImport("org.junit.Test");
				methodDeclaration.accept(new StaticAssertImportVisitor(
						importRewrite));
				modifiedDocument = true;
			} else if (SET_UP_METHOD_NAME.equals(fullyQualifiedName)) {
				createMarkerAnnotation(ast, rewriter,
						methodDeclaration, BEFORE_ANNOTATION_NAME);
				convertProtectedToPublic(ast, rewriter,
						methodDeclaration);
				importRewrite.addImport("org.junit.Before");
				modifiedDocument = true;
			} else if (TEAR_DOWN_METHOD_NAME.equals(fullyQualifiedName)) {
				createMarkerAnnotation(ast, rewriter,
						methodDeclaration, AFTER_ANNOTATION_NAME);
				convertProtectedToPublic(ast, rewriter,
						methodDeclaration);
				importRewrite.addImport("org.junit.After");
				modifiedDocument = true;
			}
		}
	}

	protected void removeTestCaseSuperclass(ASTRewrite rewriter,
			ImportRewrite importRewrite,
			TypeDeclaration typeDeclaration) {
		Type superclassType = typeDeclaration.getSuperclassType();
		if (superclassType != null && superclassType.isSimpleType()) {
			SimpleType superType = (SimpleType) superclassType;
			if (TEST_CASE_CLASSNAME.equals(superType.getName()
					.getFullyQualifiedName())) {
				rewriter.remove(superType, null);
				importRewrite.removeImport("junit.framework.TestCase");
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
				if (ModifierKeyword.PROTECTED_KEYWORD
						.equals(modifier.getKeyword())) {
					Modifier publicModifier = ast
							.newModifier(ModifierKeyword.PUBLIC_KEYWORD);
					rewriter.replace(modifier, publicModifier,
							null);
				}
			}
		}
	}

	protected void createMarkerAnnotation(final AST ast,
			final ASTRewrite rewriter, MethodDeclaration methodDeclaration,
			String annotationName) {
		if (!isAnnotationExisting(methodDeclaration.modifiers(), annotationName)) {
			MarkerAnnotation testAnnotation = ast.newMarkerAnnotation();
			testAnnotation.setTypeName(ast.newName(annotationName));

			ListRewrite listRewrite = rewriter.getListRewrite(
					methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
			listRewrite.insertFirst(testAnnotation, null);
		}
	}

	protected boolean isAnnotationExisting(List modifiers, String annotationName) {
		for (Object modifier : modifiers) {
			if (modifier instanceof MarkerAnnotation) {
				MarkerAnnotation markerAnnotation = (MarkerAnnotation) modifier;
				Name typeName = markerAnnotation.getTypeName();
				String fullyQualifiedName = typeName.getFullyQualifiedName();
				return annotationName.equals(fullyQualifiedName);
			}
		}
		return false;
	}
}
