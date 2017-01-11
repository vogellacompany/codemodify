package de.simonscholz.lambdaconverter;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.eclipse.jface.text.BadLocationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LambdaConverterTest {
	private static final String PACKAGE_NAME = "p";
	private static final String JAVA_VERSION = "1.8";
	private static final String PROJECT_NAME = "P1";
	private static final String OUTPUT_FOLDER = "bin";
	private static final String SOURCE_FOLDER_NAME = "src";
	private TestingEnvironment env;
	
	@Before
	public void setUp() {
		env = new TestingEnvironment();
		env.openEmptyWorkspace();
	}
	
	@After
	public void tearDown() {
		env.resetWorkspace();
	}

	@Test
	public void modifyCompilationUnit_multilineClassInstanceCreation_savedMultilineLambda() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER,\n" + 
						"false, false));"
				, "        button.setText(\"Text\");"
		         , "       button.addSelectionListener(new SelectionAdapter() {"
		         , "	       @Override"
		         , "	       public void widgetSelected(SelectionEvent e) {"
		         , "	           System.out.println(\"Hello\");"
		         , "	           System.out.println(\"world\");"
		         , "	       }"
		         , "	   });"
		         , "    }"
		         , "}"
				));
		new LambdaConverter().modifyCompilationUnit(astRoot, null);
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);
		String expected = String.join("\n"
				, "package p"
				, "import org.eclipse.swt.events.SelectionListener;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER,\n" + 
						"false, false));"
				, "        button.setText(\"Text\");"
		         , "       button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {"
		         , "	       System.out.println(\"Hello\");"
		         , "	       System.out.println(\"world\");"
		         , "	   }));"
		         , "    }"
		         , "}");
		assertEquals(expected, iCU.getSource());
	}
	
	@Test
	public void modifyCompilationUnit_singlelineClassInstanceCreation_savedSinglelineLambda() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER,\n" + 
						"false, false));"
				, "        button.setText(\"Text\");"
		         , "       button.addSelectionListener(new SelectionAdapter() {"
		         , "	       @Override"
		         , "	       public void widgetSelected(SelectionEvent e) {"
		         , "	           System.out.println(\"Hello\");"
		         , "	       }"
		         , "	   });"
		         , "    }"
		         , "}"
				));
		new LambdaConverter().modifyCompilationUnit(astRoot, null);
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);
		String expected = String.join("\n"
				, "package p"
				, "import org.eclipse.swt.events.SelectionListener;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER,\n" + 
						"false, false));"
				, "        button.setText(\"Text\");"
		         , "       button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> System.out.println(\"Hello\")));"
		         , "    }"
		         , "}");
		assertEquals(expected, iCU.getSource());
	}

	private CompilationUnit setupEnvironment(String testClass) throws JavaModelException, CoreException {
		env.resetWorkspace();
		IPath projectPath = env.addProject(PROJECT_NAME, JAVA_VERSION);
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
