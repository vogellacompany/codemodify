package de.simonscholz.lambdaconverter.cleanup;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.junit.After;
import org.junit.Before;

public class JdtTest {
	protected static final String PACKAGE_NAME = "p";
	protected static final String JAVA_VERSION = "1.8";
	protected static final String PROJECT_NAME = "P1";
	protected static final String OUTPUT_FOLDER = "bin";
	protected static final String SOURCE_FOLDER_NAME = "src";
	protected TestingEnvironment env;

	@Before
	public void setUp() {
		env = new TestingEnvironment();
		env.openEmptyWorkspace();
	}

	@After
	public void tearDown() {
		env.resetWorkspace();
	}

	protected CompilationUnit setupEnvironment(String testClass) throws JavaModelException, CoreException {
		return setupEnvironment(testClass, JAVA_VERSION);
	}

	protected CompilationUnit setupEnvironment(String testClass, String javaVersion) throws JavaModelException, CoreException {
		env.resetWorkspace();
		IPath projectPath = env.addProject(PROJECT_NAME, javaVersion);
		env.removePackageFragmentRoot(projectPath, "");
		IPath root = env.addPackageFragmentRoot(projectPath, SOURCE_FOLDER_NAME);
		env.setOutputFolder(projectPath, OUTPUT_FOLDER);

		env.addClass(root, PACKAGE_NAME, "SamplePart", testClass);
		env.addClass(root, "org.eclipse.swt.events", "SelectionListener",
				String.join("\n"
						, "package org.eclipse.swt.events;"
						, "public interface SelectionListener extends SWTEventListener {"
						, "public void widgetSelected(SelectionEvent e);"
						, "public void widgetDefaultSelected(SelectionEvent e);"
						, "public static SelectionListener widgetSelectedAdapter(Consumer<SelectionEvent> c) {"
						, "	return new SelectionAdapter() {"
						, "		@Override"
						, "		public void widgetSelected(SelectionEvent e) {"
						, "			c.accept(e);"
						, "		}"
						, "	};"
						, "}"
						, "public static SelectionListener widgetDefaultSelectedAdapter(Consumer<SelectionEvent> c) {"
						, "	return new SelectionAdapter() {"
						, "		@Override"
						, "		public void widgetDefaultSelected(SelectionEvent e) {"
						, "			c.accept(e);"
						, "		}"
						, "	};"
						, "}"
						, "}"));
		env.addClass(root, "org.eclipse.swt.events", "SelectionAdapter",
				String.join("\n"
						, "package org.eclipse.swt.events;"
						, "public abstract class SelectionAdapter implements SelectionListener {"
						, "@Override"
						, "public void widgetSelected(SelectionEvent e) {"
						, "}"
						, "@Override"
						, "public void widgetDefaultSelected(SelectionEvent e) {"
						, "}"
						, "}"));

		env.addJavaLibrariesTo(PROJECT_NAME);
		IJavaProject javaProject = env.getJavaProject(PROJECT_NAME);
		ICompilationUnit icu = javaProject.findPackageFragment(new Path(File.separator + PROJECT_NAME + File.separator + SOURCE_FOLDER_NAME + File.separator + PACKAGE_NAME)).getCompilationUnits()[0];
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);
		parser.setSource(icu);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
		return astRoot;
	}


}
