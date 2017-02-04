package de.simonscholz.lambdaconverter.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;


public class LambdaConverterFix implements ICleanUpFix {

	private CompilationUnit compilationUnit;

	private static final String ADAPTER_METHOD_POSTFIX = "Adapter";
	private static final String CLASS_INSTANCE_CREATION_TYPE = "SelectionAdapter";
	private static final String TEXT_EDIT_GROUP_NAME = "Convert to lambda expression";

	private ArrayList<ClassInstanceCreation> classInstanceCreations;

	private TextEditGroup textEditGroup;

	public static final class FunctionalAnonymousClassesFinder extends ASTVisitor {
		private final ArrayList<ClassInstanceCreation> nodes = new ArrayList<>();

		public static ArrayList<ClassInstanceCreation> perform(ASTNode node) {
			FunctionalAnonymousClassesFinder finder = new FunctionalAnonymousClassesFinder();
			node.accept(finder);
			return finder.nodes;
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			if (isFunctionalAnonymous(node)) {
				nodes.add(node);
			}
			return true;
		}
	}


	public static final class MethodDeclarationFinder extends ASTVisitor {
		private final List <MethodDeclaration> methods = new ArrayList <> ();

		public static List<MethodDeclaration> perform(ASTNode node) {
			MethodDeclarationFinder finder = new MethodDeclarationFinder();
			node.accept(finder);
			return finder.getMethods();
		}

		@Override
		public boolean visit (final MethodDeclaration method) {
			methods.add (method);
			return super.visit(method);
		}

		/**
		 * @return an immutable list view of the methods discovered by this visitor
		 */
		public List<MethodDeclaration> getMethods() {
			return Collections.unmodifiableList(methods);
		}
	}

	private static final class AnnotationsFinder extends ASTVisitor {
		static boolean hasAnnotations(SingleVariableDeclaration methodParameter) {
			try {
				AnnotationsFinder finder= new AnnotationsFinder();
				methodParameter.accept(finder);
			} catch (AbortSearchException e) {
				return true;
			}
			return false;
		}

		@Override
		public boolean visit(MarkerAnnotation node) {
			throw new AbortSearchException();
		}

		@Override
		public boolean visit(NormalAnnotation node) {
			throw new AbortSearchException();
		}

		@Override
		public boolean visit(SingleMemberAnnotation node) {
			throw new AbortSearchException();
		}
	}

	private static final class ImportRelevanceFinder extends ASTVisitor {
		private static String qualifiedName;

		static boolean isStillNeeded(CompilationUnit cu, String qualifiedName) {
			ImportRelevanceFinder.qualifiedName = qualifiedName;
			try {
				ImportRelevanceFinder finder = new ImportRelevanceFinder();
				cu.accept(finder);
			} catch (AbortSearchException e) {
				return true;
			}
			return false;
		}

		@Override
		public boolean visit(VariableDeclarationFragment node) {
			IVariableBinding resolveBinding = node.resolveBinding();
			if (null == resolveBinding) {
				return super.visit(node);
			}
			String qualifiedName2 = resolveBinding.getType().getQualifiedName();
			if (qualifiedName2.equals(qualifiedName)) {
				throw new AbortSearchException();
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(SingleVariableDeclaration node) {
			String qualifiedName2 = node.resolveBinding().getType().getQualifiedName();
			if (qualifiedName2.equals(qualifiedName)) {
				throw new AbortSearchException();
			}
			SelectionAdapter adapter = new SelectionAdapter() {
			};
			return super.visit(node);
		}

		@Override
		public boolean visit(SimpleType node) {
			String qualifiedName2 = node.resolveBinding().getQualifiedName();
			if (qualifiedName2.equals(qualifiedName)) {
				throw new AbortSearchException();
			}
			return super.visit(node);
		}
	}

	static boolean isFunctionalAnonymous(ClassInstanceCreation node) {
		ITypeBinding typeBinding= node.resolveTypeBinding();
		if (typeBinding == null) {
			return false;
		}
		// TODO(fap): remove hardcoding for SelectionAdapter
		if (!CLASS_INSTANCE_CREATION_TYPE.equals(node.getType().toString())) {
			return false;
		}

		AnonymousClassDeclaration anonymTypeDecl = node.getAnonymousClassDeclaration();
		if (anonymTypeDecl == null || anonymTypeDecl.resolveBinding() == null) {
			return false;
		}

		List<BodyDeclaration> bodyDeclarations = anonymTypeDecl.bodyDeclarations();
		// cannot convert if there are fields or additional methods
		if (bodyDeclarations.size() != 1) {
			return false;
		}
		BodyDeclaration bodyDeclaration = bodyDeclarations.get(0);
		if (!(bodyDeclaration instanceof MethodDeclaration)) {
			return false;
		}

		MethodDeclaration methodDecl = (MethodDeclaration) bodyDeclaration;
		IMethodBinding methodBinding = methodDecl.resolveBinding();

		if (methodBinding == null) {
			return false;
		}
		// generic lambda expressions are not allowed
		if (methodBinding.isGenericMethod()) {
			return false;
		}

		int modifiers= methodBinding.getModifiers();
		if (Modifier.isSynchronized(modifiers) || Modifier.isStrictfp(modifiers)) {
			return false;
		}

		// lambda cannot refer to 'this'/'super' literals
		if (SuperThisReferenceFinder.hasReference(methodDecl)) {
			return false;
		}

		//		if (ASTNodes.getTargetType(node) == null) // #isInTargetTypeContext
		//			return false;

		return !hasAnnotationExcept(methodBinding, Stream.of("java.lang.Override", "java.lang.Deprecated"));
	}

	private static boolean hasAnnotationExcept(IMethodBinding methodBinding, Stream<String> exceptedAnnotations) {
		IAnnotationBinding[] declarationAnnotations = methodBinding.getAnnotations();
		for (IAnnotationBinding declarationAnnotation : declarationAnnotations) {
			ITypeBinding annotationType = declarationAnnotation.getAnnotationType();
			if (annotationType != null) {
				String qualifiedName = annotationType.getQualifiedName();
				if (!exceptedAnnotations.anyMatch(exceptedAnnotation -> exceptedAnnotation.equals(qualifiedName))) {
					return true;
				}
			}
		}
		return false;
	}

	private static class AbortSearchException extends RuntimeException {
		private static final long serialVersionUID= 1L;
	}

	private static final class SuperThisReferenceFinder extends HierarchicalASTVisitor {
		private ITypeBinding functionalInterface;
		private MethodDeclaration methodDeclaration;

		static boolean hasReference(MethodDeclaration node) {
			try {
				SuperThisReferenceFinder finder = new SuperThisReferenceFinder();
				ClassInstanceCreation cic = (ClassInstanceCreation) node.getParent().getParent();
				finder.functionalInterface = cic.getType().resolveBinding();
				finder.methodDeclaration = node;
				node.accept(finder);
			} catch (AbortSearchException e) {
				return true;
			}
			return false;
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(BodyDeclaration node) {
			return false;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			return node == methodDeclaration;
		}

		@Override
		public boolean visit(ThisExpression node) {
			if (node.getQualifier() == null) {
				throw new AbortSearchException();
			}
			return true; // references to outer scope are harmless
		}

		@Override
		public boolean visit(SuperMethodInvocation node) {
			if (node.getQualifier() == null) {
				throw new AbortSearchException();
			} else {
				IBinding qualifierType = node.getQualifier().resolveBinding();
				if (qualifierType instanceof ITypeBinding && ((ITypeBinding) qualifierType).isInterface()) {
					throw new AbortSearchException(); // JLS8: new overloaded meaning of 'interface'.super.'method'(..)
				}
			}
			return true; // references to outer scopes are harmless
		}

		@Override
		public boolean visit(SuperFieldAccess node) {
			if (node.getQualifier() == null)
				throw new AbortSearchException();
			return true; // references to outer scope are harmless
		}

		@Override
		public boolean visit(MethodInvocation node) {
			IMethodBinding binding = node.resolveMethodBinding();
			if (binding != null && !JdtFlags.isStatic(binding) && node.getExpression() == null
					&& Bindings.isSuperType(binding.getDeclaringClass(), functionalInterface, false))
				throw new AbortSearchException();
			return true;
		}
	}

	public TextEdit createLambdaConversionTextEdit(CompilationUnit astRoot, IProgressMonitor monitor) throws JavaModelException, CoreException {
		AST ast = astRoot.getAST();
		List<?> types = astRoot.types();
		SubMonitor subMonitor = SubMonitor.convert(monitor, types.size());
		ASTRewrite rewriter = ASTRewrite.create(ast);
		ImportRewrite importRewrite = ImportRewrite.create(astRoot, true);
		boolean modifiedDocument = false;
		for (ClassInstanceCreation classInstanceCreation : classInstanceCreations) {
			boolean converted = prepareConversionToLambda(astRoot, ast, rewriter, importRewrite, classInstanceCreation);
			modifiedDocument = modifiedDocument || converted;
		}
		if (modifiedDocument) {
			ICompilationUnit adapter = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);
			if (adapter != null) {
				return createTextEdit(adapter, subMonitor.newChild(1), rewriter, importRewrite);
			}
		}
		return null;
	}

	protected TextEdit createTextEdit(ICompilationUnit cu, IProgressMonitor monitor, final ASTRewrite rewriter,
			ImportRewrite importRewrite) throws CoreException, JavaModelException {
		TextEdit importEdits = importRewrite.rewriteImports(monitor);
		TextEdit edits = rewriter.rewriteAST();
		importEdits.addChild(edits);
		return importEdits;
	}

	private boolean prepareConversionToLambda(CompilationUnit cu, AST ast, ASTRewrite rewriter, ImportRewrite importRewrite, ClassInstanceCreation classInstanceCreation) {
		AnonymousClassDeclaration anonymTypeDecl = classInstanceCreation.getAnonymousClassDeclaration();
		List<BodyDeclaration> bodyDeclarations = anonymTypeDecl.bodyDeclarations();
		Object object = bodyDeclarations.get(0);
		if (!(object instanceof MethodDeclaration)) {
			return false;
		}
		MethodDeclaration methodDeclaration = (MethodDeclaration) object;
		Optional<IMethodBinding> methodInInterface = findMethodInInterface(classInstanceCreation, ast.newSimpleName(methodDeclaration.getName() + ADAPTER_METHOD_POSTFIX), methodDeclaration.parameters());
		if (!methodInInterface.isPresent()) {
			return false;
		}
		LambdaExpression lambdaExpression = ast.newLambdaExpression();
		List<SingleVariableDeclaration> methodParameters = methodDeclaration.parameters();
		// use short form with inferred parameter types and without parentheses if possible
		boolean createExplicitlyTypedParameters = hasAnnotation(methodParameters);
		prepareLambdaParameters(ast, rewriter, methodParameters, createExplicitlyTypedParameters, lambdaExpression);
		Block body = methodDeclaration.getBody();
		List<Statement> statements = body.statements();
		ASTNode lambdaBody = prepareLambdaBody(body, statements);
		lambdaExpression.setBody(getCopyOrReplacement(rewriter, lambdaBody, textEditGroup));
		MethodInvocation methodInvocation = prepareMethodInvocation(ast, methodDeclaration, lambdaExpression);

		// TODO(fap): insert cast if necessary

		try {
			prepareChanges(cu, rewriter, importRewrite, classInstanceCreation, textEditGroup, methodInInterface.get(), methodInvocation);
		} catch (MalformedTreeException | CoreException | BadLocationException e) {
			return false;
		}
		return true;
	}

	private void prepareChanges(CompilationUnit cu, ASTRewrite rewriter, ImportRewrite importRewrite,
			ClassInstanceCreation classInstanceCreation, TextEditGroup group, IMethodBinding methodInInterface,
			MethodInvocation methodInvocation) throws CoreException, MalformedTreeException, BadLocationException {
		rewriter.replace(classInstanceCreation, methodInvocation, group);
		importRewrite.addStaticImport(methodInInterface);
		ITypeBinding typeBinding = classInstanceCreation.resolveTypeBinding();
		ITypeBinding superclass = typeBinding.getSuperclass();
		handleImportRemoval(cu, rewriter, importRewrite, superclass);
	}

	private void handleImportRemoval(CompilationUnit cu, ASTRewrite rewriter, ImportRewrite importRewrite,
			ITypeBinding superclass) throws CoreException, JavaModelException, BadLocationException {
		ICompilationUnit icu = (ICompilationUnit) cu.getJavaElement().getAdapter(IOpenable.class);
		TextEdit importEdits = importRewrite.rewriteImports(new NullProgressMonitor());
		TextEdit edits = rewriter.rewriteAST();
		importEdits.addChild(edits);
		
		// apply the text edits to the compilation unit
		String source = icu.getSource();
		Document document = new Document(source);
		importEdits.apply(document);

		String oldContents = icu.getBuffer().getContents();
		icu.getBuffer().setContents(document.get());
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);
		parser.setSource(icu);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
		if (!ImportRelevanceFinder.isStillNeeded(astRoot, superclass.getQualifiedName())) {
			importRewrite.removeImport(superclass.getQualifiedName());
		}
		icu.getBuffer().setContents(oldContents);
	}

	private MethodInvocation prepareMethodInvocation(AST ast, MethodDeclaration methodDeclaration, LambdaExpression lambdaExpression) {
		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodInvocation.setName(ast.newSimpleName(methodDeclaration.getName().toString() + ADAPTER_METHOD_POSTFIX));
		methodInvocation.arguments().add(lambdaExpression);
		return methodInvocation;
	}

	private void prepareLambdaParameters(AST ast, ASTRewrite rewriter, List<SingleVariableDeclaration> methodParameters,
			boolean createExplicitlyTypedParameters, LambdaExpression lambdaExpression) {
		List<VariableDeclaration> lambdaParameters = lambdaExpression.parameters();
		lambdaExpression.setParentheses(createExplicitlyTypedParameters || methodParameters.size() != 1);
		for (SingleVariableDeclaration methodParameter : methodParameters) {
			if (createExplicitlyTypedParameters) {
				lambdaParameters.add((SingleVariableDeclaration) rewriter.createCopyTarget(methodParameter));
				// TODO(fap): handle import
			} else {
				VariableDeclarationFragment lambdaParameter = ast.newVariableDeclarationFragment();
				SimpleName name = (SimpleName) rewriter.createCopyTarget(methodParameter.getName());
				lambdaParameter.setName(name);
				lambdaParameters.add(lambdaParameter);
			}
		}
	}

	private ASTNode prepareLambdaBody(Block body, List<Statement> statements) {
		ASTNode lambdaBody = body;
		if (statements.size() == 1) {
			// use short form with just an expression body if possible
			Statement statement = statements.get(0);
			if (statement instanceof ExpressionStatement) {
				lambdaBody = ((ExpressionStatement) statement).getExpression();
			} else if (statement instanceof ReturnStatement) {
				Expression returnExpression = ((ReturnStatement) statement).getExpression();
				if (returnExpression != null) {
					lambdaBody = returnExpression;
				}
			}
		}
		return lambdaBody;
	}

	private boolean hasAnnotation(List<SingleVariableDeclaration> methodParameters) {
		for (SingleVariableDeclaration methodParameter : methodParameters) {
			if (AnnotationsFinder.hasAnnotations(methodParameter)) {
				return true;
			}
		}
		return false;
	}

	// TODO(fap): compare methodname + method args?
	private Optional<IMethodBinding> findMethodInInterface(ClassInstanceCreation classInstanceCreation, SimpleName methodName, List<SingleVariableDeclaration> parameters) {
		ITypeBinding typeBinding = classInstanceCreation.resolveTypeBinding();
		ITypeBinding superclass = typeBinding.getSuperclass();
		for (ITypeBinding interFace : superclass.getInterfaces()) {
			IMethodBinding[] declaredMethods = interFace.getDeclaredMethods();
			for (IMethodBinding declaredMethod : declaredMethods) {
				if (declaredMethod.getName().toString().equals(methodName.toString())) {
					return Optional.of(declaredMethod);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * If the given <code>node</code> has already been rewritten, undo that rewrite and return the
	 * replacement version of the node. Otherwise, return the result of
	 * {@link ASTRewrite#createCopyTarget(ASTNode)}.
	 *
	 * @param rewrite ASTRewrite for the given node
	 * @param node the node to get the replacement or to create a copy placeholder for
	 * @param group the edit group which collects the corresponding text edits, or <code>null</code>
	 *            if ungrouped
	 * @return the replacement node if the given <code>node</code> has already been rewritten or the
	 *         new copy placeholder node
	 */
	public ASTNode getCopyOrReplacement(ASTRewrite rewrite, ASTNode node, TextEditGroup group) {
		ASTNode rewrittenNode = (ASTNode) rewrite.get(node.getParent(), node.getLocationInParent());
		if (rewrittenNode != node) {
			// Undo previous rewrite to avoid the problem that the same node would be inserted in two places:
			rewrite.replace(rewrittenNode, node, group);
			return rewrittenNode;
		}
		return rewrite.createCopyTarget(node);
	}

	public LambdaConverterFix(CompilationUnit compilationUnit, ArrayList<ClassInstanceCreation> classInstanceCreations, TextEditGroup categorizedTextEditGroup) {
		this.compilationUnit = compilationUnit;
		this.classInstanceCreations = classInstanceCreations;
		this.textEditGroup = categorizedTextEditGroup;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		TextEdit edit = createLambdaConversionTextEdit(compilationUnit, progressMonitor);
		CompilationUnitChange result= new CompilationUnitChange(TEXT_EDIT_GROUP_NAME, (ICompilationUnit) compilationUnit.getJavaElement());
		if (null == edit) {
			return result;
		}
		result.setEdit(edit);
		result.addTextEditGroup(textEditGroup);
		return result;
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean enabled) {
		ArrayList<ClassInstanceCreation> classInstanceCreations = FunctionalAnonymousClassesFinder.perform(compilationUnit);
		if (!enabled || (classInstanceCreations.size() <= 0)) {
			return null;
		}
		return new LambdaConverterFix(compilationUnit, classInstanceCreations, new CategorizedTextEditGroup(TEXT_EDIT_GROUP_NAME, new GroupCategorySet(new GroupCategory(TEXT_EDIT_GROUP_NAME, TEXT_EDIT_GROUP_NAME, TEXT_EDIT_GROUP_NAME))));
	}

}
