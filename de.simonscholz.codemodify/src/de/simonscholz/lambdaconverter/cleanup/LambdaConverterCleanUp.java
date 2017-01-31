package de.simonscholz.lambdaconverter.cleanup;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class LambdaConverterCleanUp implements ICleanUp {

	public static final String CLEANUP_CONVERT_TO_LAMBDA = "cleanup.convert_to_lambda";// $NON-NLS-1$
	private CleanUpOptions fOptions;
	private RefactoringStatus fStatus;

	@Override
	public void setOptions(CleanUpOptions options) {
		Assert.isLegal(options != null);
		Assert.isTrue(fOptions == null);
		fOptions = options;
	}

	@Override
	public String[] getStepDescriptions() {
		if (fOptions.isEnabled(CLEANUP_CONVERT_TO_LAMBDA)) {// $NON-NLS-1$
			return new String[] { "Converting anonymous classes to lambdas" };//$NON-NLS-1$
		}
		return null;
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean changedRegionsRequired = false;
		Map<String, String> compilerOptions = null;
		boolean isActivated = fOptions.isEnabled(CLEANUP_CONVERT_TO_LAMBDA);// $NON-NLS-1$
		return new CleanUpRequirements(isActivated, isActivated, changedRegionsRequired, compilerOptions);
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit = context.getAST();
		if (compilationUnit == null) {
			return null;
		}
		return LambdaConverterFix.createCleanUp(compilationUnit, fOptions.isEnabled(CLEANUP_CONVERT_TO_LAMBDA));// $NON-NLS-1$
	}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		try {
			if (fStatus == null || fStatus.isOK()) {
				return new RefactoringStatus();
			} else {
				return fStatus;
			}
		} finally {
			fStatus = null;
		}
	}

}
