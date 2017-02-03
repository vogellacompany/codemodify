package de.simonscholz.lambdaconverter.cleanup;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.junit.Test;

public class LambdaConverterCleanUpTest extends JdtTest {

	@Test
	public void modifyCompilation_javaVersionBewow18_noConversionPrepared() throws JavaModelException, CoreException, BadLocationException, IOException, URISyntaxException {
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
				), "1.7");

		LambdaConverterCleanUp lambdaConverterCleanUp = new LambdaConverterCleanUp();
		ICompilationUnit iCU = (ICompilationUnit) astRoot.getJavaElement().getAdapter(IOpenable.class);
		RefactoringStatus refactoringStatus = lambdaConverterCleanUp.checkPreConditions(env.getJavaProject(PROJECT_NAME), (ICompilationUnit[]) Arrays.asList(iCU).toArray(), new NullProgressMonitor());
		
		assertEquals(4, refactoringStatus.getSeverity());
	}
}
