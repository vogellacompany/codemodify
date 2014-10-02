package de.simonscholz.nonjavadoc.jobs;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;

import de.simonscholz.CodeModifierJob;
import de.simonscholz.DefaultCodeModifier;
import de.simonscholz.ICodeModifier;
import de.simonscholz.nonjavadoc.NonJavaDocRemover;

public class NonJavaDocJob extends CodeModifierJob {

	private ICodeModifier nonJavaDocRemover;

	public NonJavaDocJob(List<IJavaElement> selectedElements) {
		super(selectedElements, "Deleting {non-javadoc} comments");
	}

	@Override
	protected ICodeModifier getConverter() {
		if (null == nonJavaDocRemover) {
			nonJavaDocRemover = new DefaultCodeModifier(new NonJavaDocRemover());
		}
		return nonJavaDocRemover;
	}

}
