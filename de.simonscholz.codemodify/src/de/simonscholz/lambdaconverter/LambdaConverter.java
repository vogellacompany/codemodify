package de.simonscholz.lambdaconverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
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
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
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
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import de.simonscholz.ICompilationUnitModifier;

public class LambdaConverter implements ICompilationUnitModifier {
	private static final String ADAPTER_METHOD_POSTFIX = "Adapter";
	private static final String CLASS_INSTANCE_CREATION_TYPE = "SelectionAdapter";
	private static final String TEXT_EDIT_GROUP_NAME = "Convert to lambda expression";
	private static boolean conversionRemovesAnnotations;
	
	private static final class FunctionalAnonymousClassesFinder extends ASTVisitor {
		private final ArrayList<ClassInstanceCreation> nodes = new ArrayList<>();
		
		public static ArrayList<ClassInstanceCreation> perform(ASTNode node) {
			FunctionalAnonymousClassesFinder finder = new FunctionalAnonymousClassesFinder();
			node.accept(finder);
			return finder.nodes;
		}
		
		@Override
		public boolean visit(ClassInstanceCreation node) {
			if (isFunctionalAnonymous(node) && !conversionRemovesAnnotations) {
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
	  public List <MethodDeclaration> getMethods() {
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
	
	// this definition doesn't matches our case, otherwise we could just use this solution, doh!
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
		
		// Check if annotations other than @Override and @Deprecated will be removed
		checkAnnotationsRemoval(methodBinding);

		return true;
	}
	
	private static void checkAnnotationsRemoval(IMethodBinding methodBinding) {
		conversionRemovesAnnotations = false;
		IAnnotationBinding[] declarationAnnotations = methodBinding.getAnnotations();
		for (IAnnotationBinding declarationAnnotation : declarationAnnotations) {
			ITypeBinding annotationType = declarationAnnotation.getAnnotationType();
			if (annotationType != null) {
				String qualifiedName = annotationType.getQualifiedName();
				System.out.println("Qualified name of annotation: " + qualifiedName);
				if (!"java.lang.Override".equals(qualifiedName) && !"java.lang.Deprecated".equals(qualifiedName)) { //$NON-NLS-1$ //$NON-NLS-2$
					conversionRemovesAnnotations = true;
					return;
				}
			}
		}
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
			if (node.getQualifier() == null)
				throw new AbortSearchException();
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

	@Override
	public void modifyCompilationUnit(CompilationUnit astRoot, IProgressMonitor monitor) throws JavaModelException, CoreException, BadLocationException {
		AST ast = astRoot.getAST();
		List<?> types = astRoot.types();
		SubMonitor subMonitor = SubMonitor.convert(monitor, types.size());
		ASTRewrite rewriter = ASTRewrite.create(ast);
		ImportRewrite importRewrite = ImportRewrite.create(astRoot, true);
		ArrayList<ClassInstanceCreation> classInstanceCreations = FunctionalAnonymousClassesFinder.perform(astRoot);
		boolean modifiedDocument = false;
		for (ClassInstanceCreation classInstanceCreation : classInstanceCreations) {
			modifiedDocument = modifiedDocument || convertToLambda(astRoot, ast, rewriter, importRewrite, classInstanceCreation);
		}
		if (modifiedDocument) {
			ICompilationUnit adapter = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);
			if (adapter != null) {
				saveChanges(adapter, subMonitor.newChild(1), rewriter, importRewrite);
			}
		}
	}

	private boolean convertToLambda(CompilationUnit cu, AST ast, ASTRewrite rewriter, ImportRewrite importRewrite, ClassInstanceCreation classInstanceCreation) {
		TextEditGroup group = new TextEditGroup(TEXT_EDIT_GROUP_NAME);
		AnonymousClassDeclaration anonymTypeDecl = classInstanceCreation.getAnonymousClassDeclaration();
		List<BodyDeclaration> bodyDeclarations = anonymTypeDecl.bodyDeclarations();
		Object object = bodyDeclarations.get(0);
		if (!(object instanceof MethodDeclaration)) {
			return false;
		}
		MethodDeclaration methodDeclaration = (MethodDeclaration) object;
		List<SingleVariableDeclaration> methodParameters = methodDeclaration.parameters();
		Optional<ITypeBinding> interFace = findInterfaceContaining(classInstanceCreation, methodDeclaration.getName());
		if (!interFace.isPresent()) {
			return false;
		}
		LambdaExpression lambdaExpression = ast.newLambdaExpression();
		// use short form with inferred parameter types and without parentheses if possible
		boolean createExplicitlyTypedParameters = hasAnnotation(methodParameters);
		prepareLambdaParameters(ast, rewriter, methodParameters, createExplicitlyTypedParameters, lambdaExpression);
		Block body = methodDeclaration.getBody();
		List<Statement> statements = body.statements();
		ASTNode lambdaBody = prepareSingleOrMultiLineLambdaBody(body, statements);
		lambdaExpression.setBody(getCopyOrReplacement(rewriter, lambdaBody, group));
        MethodInvocation methodInvocation = prepareMethodInvocation(ast, methodDeclaration, interFace, lambdaExpression);
		
		// TODO(fap): insert cast if necessary
		
		executeChanges(rewriter, importRewrite, classInstanceCreation, group, interFace, methodInvocation);
		return true;
	}

	private void executeChanges(ASTRewrite rewriter, ImportRewrite importRewrite,
			ClassInstanceCreation classInstanceCreation, TextEditGroup group, Optional<ITypeBinding> interFace,
			MethodInvocation methodInvocation) {
		rewriter.replace(classInstanceCreation, methodInvocation, group);
		importRewrite.addImport(interFace.get());
		ITypeBinding typeBinding = classInstanceCreation.resolveTypeBinding();
		ITypeBinding superclass = typeBinding.getSuperclass();
		importRewrite.removeImport(superclass.getQualifiedName());
	}

	private MethodInvocation prepareMethodInvocation(AST ast, MethodDeclaration methodDeclaration,
			Optional<ITypeBinding> interFace, LambdaExpression lambdaExpression) {
		MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(ast.newSimpleName(interFace.get().getName()));
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

	private ASTNode prepareSingleOrMultiLineLambdaBody(Block body, List<Statement> statements) {
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
	private Optional<ITypeBinding> findInterfaceContaining(ClassInstanceCreation classInstanceCreation, SimpleName methodName) {
		ITypeBinding typeBinding = classInstanceCreation.resolveTypeBinding();
		ITypeBinding superclass = typeBinding.getSuperclass();
		for (ITypeBinding interFace : superclass.getInterfaces()) {
			IMethodBinding[] declaredMethods = interFace.getDeclaredMethods();
			for (IMethodBinding iMethodBinding : declaredMethods) {
				String declaredMethodName = iMethodBinding.getName();
				if (declaredMethodName.toString().equals(methodName.toString())) {
					return Optional.of(interFace);
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
	
	private HashSet<String> makeNamesUnique(HashSet<String> excludedNames, MethodDeclaration methodDeclaration, ASTRewrite rewrite, TextEditGroup group) {
		HashSet<String> newNames = new HashSet<>();
		excludedNames.addAll(ASTNodes.getVisibleLocalVariablesInScope(methodDeclaration));
		List<SimpleName> simpleNamesInMethod = getNamesInMethod(methodDeclaration);
		List<String> namesInMethod = new ArrayList<>();
		for (SimpleName name : simpleNamesInMethod) {
			namesInMethod.add(name.getIdentifier());
		}

		for (int i= 0; i < simpleNamesInMethod.size(); i++) {
			SimpleName name= simpleNamesInMethod.get(i);
			String identifier = namesInMethod.get(i);
			HashSet<String> allNamesToExclude = getNamesToExclude(excludedNames, namesInMethod, i);
			if (allNamesToExclude.contains(identifier)) {
				String newIdentifier = createName(identifier, allNamesToExclude);
				excludedNames.add(newIdentifier);
				newNames.add(newIdentifier);
				SimpleName[] references = LinkedNodeFinder.findByNode(name.getRoot(), name);
				for (SimpleName ref : references) {
					rewrite.set(ref, SimpleName.IDENTIFIER_PROPERTY, newIdentifier, group);
				}
			}
		}

		return newNames;
	}

	private HashSet<String> getNamesToExclude(HashSet<String> excludedNames, List<String> namesInMethod, int i) {
		HashSet<String> allNamesToExclude= new HashSet<>(excludedNames);
		allNamesToExclude.addAll(namesInMethod.subList(0, i));
		allNamesToExclude.addAll(namesInMethod.subList(i + 1, namesInMethod.size()));
		return allNamesToExclude;
	}

	private List<SimpleName> getNamesInMethod(MethodDeclaration methodDeclaration) {
		class NamesCollector extends HierarchicalASTVisitor {
			private int fTypeCounter;

			private List<SimpleName> fNames= new ArrayList<>();

			@Override
			public boolean visit(AbstractTypeDeclaration node) {
				if (fTypeCounter++ == 0) {
					fNames.add(node.getName());
				}
				return true;
			}

			@Override
			public void endVisit(AbstractTypeDeclaration node) {
				fTypeCounter--;
			}

			@Override
			public boolean visit(AnonymousClassDeclaration node) {
				fTypeCounter++;
				return true;
			}

			@Override
			public void endVisit(AnonymousClassDeclaration node) {
				fTypeCounter--;
			}

			@Override
			public boolean visit(VariableDeclaration node) {
				if (fTypeCounter == 0)
					fNames.add(node.getName());
				return true;
			}
		}

		NamesCollector namesCollector= new NamesCollector();
		methodDeclaration.accept(namesCollector);
		return namesCollector.fNames;
	}

	private String createName(String candidate, HashSet<String> excludedNames) {
		int i= 1;
		String result= candidate;
		while (excludedNames.contains(result)) {
			result= candidate + i++;
		}
		return result;
	}

	protected void saveChanges(ICompilationUnit cu, IProgressMonitor monitor, final ASTRewrite rewriter,
			ImportRewrite importRewrite) throws CoreException, JavaModelException, BadLocationException {
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
}
