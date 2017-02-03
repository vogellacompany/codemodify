package de.simonscholz.lambdaconverter.cleanup;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jface.text.BadLocationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LambdaConverterCleanUpFixTest extends JdtTest {

	@Test
	public void createChange_multilineClassInstanceCreationWidgetSelectedAdapter_savedMultilineLambda() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
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

		ICleanUpFix cleanUp = LambdaConverterFix.createCleanUp(astRoot, true);
		CompilationUnitChange compilationUnitChange = cleanUp.createChange(new NullProgressMonitor());
		compilationUnitChange.perform(new NullProgressMonitor());
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);

		String expected = String.join("\n"
				, "package p"
				, "import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
				, "        button.setText(\"Text\");"
				, "       button.addSelectionListener(widgetSelectedAdapter(e -> {"
				, "	       System.out.println(\"Hello\");"
				, "	       System.out.println(\"world\");"
				, "	   }));"
				, "    }"
				, "}");
		assertEquals(expected, iCU.getSource());
	}

	@Test
	public void createChange_multilineClassInstanceCreationWidgetDefaultSelectedAdapter_savedMultilineLambda() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
				, "        button.setText(\"Text\");"
				, "       button.addSelectionListener(new SelectionAdapter() {"
				, "	       @Override"
				, "	       public void widgetDefaultSelected(SelectionEvent e) {"
				, "	           System.out.println(\"Hello\");"
				, "	           System.out.println(\"world\");"
				, "	       }"
				, "	   });"
				, "    }"
				, "}"
				));

		ICleanUpFix cleanUp = LambdaConverterFix.createCleanUp(astRoot, true);
		CompilationUnitChange compilationUnitChange = cleanUp.createChange(new NullProgressMonitor());
		compilationUnitChange.perform(new NullProgressMonitor());
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);

		String expected = String.join("\n"
				, "package p"
				, "import static org.eclipse.swt.events.SelectionListener.widgetDefaultSelectedAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
				, "        button.setText(\"Text\");"
				, "       button.addSelectionListener(widgetDefaultSelectedAdapter(e -> {"
				, "	       System.out.println(\"Hello\");"
				, "	       System.out.println(\"world\");"
				, "	   }));"
				, "    }"
				, "}");
		assertEquals(expected, iCU.getSource());
	}

	@Test
	public void createChange_singlelineClassInstanceCreationWidgetSelectedAdapter_savedSinglelineLambda() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
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

		ICleanUpFix cleanUp = LambdaConverterFix.createCleanUp(astRoot, true);
		CompilationUnitChange compilationUnitChange = cleanUp.createChange(new NullProgressMonitor());
		compilationUnitChange.perform(new NullProgressMonitor());
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);

		String expected = String.join("\n"
				, "package p"
				, "import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
				, "        button.setText(\"Text\");"
				, "       button.addSelectionListener(widgetSelectedAdapter(e -> System.out.println(\"Hello\")));"
				, "    }"
				, "}");
		assertEquals(expected, iCU.getSource());
	}
	
	@Test
	public void createChange_selectionAdapterImportStillNeededWidgetSelectedAdapter_selectionAdapterImportPreserved() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
						, "        button.setText(\"Text\");"
						, "        SelectionAdapter adapter = button.addSelectionListener(new SelectionAdapter() {"
						, "	       @Override"
						, "	       public void widgetSelected(SelectionEvent e) {"
						, "	           System.out.println(\"Hello\");"
						, "	       }"
						, "	   });"
						, "    }"
						, "}"
				));

		ICleanUpFix cleanUp = LambdaConverterFix.createCleanUp(astRoot, true);
		CompilationUnitChange compilationUnitChange = cleanUp.createChange(new NullProgressMonitor());
		compilationUnitChange.perform(new NullProgressMonitor());
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);

		String expected = String.join("\n"
				, "package " + PACKAGE_NAME
				, "import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;"
				, ""
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
				, "        button.setText(\"Text\");"
				, "        SelectionAdapter adapter = button.addSelectionListener(widgetSelectedAdapter(e -> System.out.println(\"Hello\")));"
				, "    }"
				, "}");
		assertEquals(expected, iCU.getSource());
	}
	
	@Test
	public void createChange_selectionAdapterImportStillNeededBCofSuperClass_selectionAdapterImportPreserved() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart extends SelectionAdapter {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
						, "        button.setText(\"Text\");"
						, "        button.addSelectionListener(new SelectionAdapter() {"
						, "	       @Override"
						, "	       public void widgetSelected(SelectionEvent e) {"
						, "	           System.out.println(\"Hello\");"
						, "	       }"
						, "	   });"
						, "    }"
						, "}"
				));

		ICleanUpFix cleanUp = LambdaConverterFix.createCleanUp(astRoot, true);
		CompilationUnitChange compilationUnitChange = cleanUp.createChange(new NullProgressMonitor());
		compilationUnitChange.perform(new NullProgressMonitor());
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);

		String expected = String.join("\n"
				, "package " + PACKAGE_NAME
				, "import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;"
				, ""
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart extends SelectionAdapter {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
				, "        button.setText(\"Text\");"
				, "        button.addSelectionListener(widgetSelectedAdapter(e -> System.out.println(\"Hello\")));"
				, "    }"
				, "}");
		assertEquals(expected, iCU.getSource());
	}

	@Test
	public void createChange_selectionAdapterImportStillNeededBCofSuperInterface_selectionAdapterImportPreserved() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
		CompilationUnit astRoot = setupEnvironment(String.join("\n"
				, "package " + PACKAGE_NAME
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart implements SelectionAdapter {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
						, "        button.setText(\"Text\");"
						, "        button.addSelectionListener(new SelectionAdapter() {"
						, "	       @Override"
						, "	       public void widgetSelected(SelectionEvent e) {"
						, "	           System.out.println(\"Hello\");"
						, "	       }"
						, "	   });"
						, "    }"
						, "}"
				));

		ICleanUpFix cleanUp = LambdaConverterFix.createCleanUp(astRoot, true);
		CompilationUnitChange compilationUnitChange = cleanUp.createChange(new NullProgressMonitor());
		compilationUnitChange.perform(new NullProgressMonitor());
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);

		String expected = String.join("\n"
				, "package " + PACKAGE_NAME
				, "import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;"
				, ""
				, "import org.eclipse.swt.events.SelectionAdapter;"
				, "public class SamplePart implements SelectionAdapter {"
				, "    @PostConstruct"
				, "    public void createComposite() {"
				, "        Button button = new Button(parent, SWT.PUSH);"
				, "        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));"
				, "        button.setText(\"Text\");"
				, "        button.addSelectionListener(widgetSelectedAdapter(e -> System.out.println(\"Hello\")));"
				, "    }"
				, "}");
		assertEquals(expected, iCU.getSource());
	}	
}
