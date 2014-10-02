package de.simonscholz.junit4converter.jobs;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;

import de.simonscholz.CodeModifierJob;
import de.simonscholz.DefaultCodeModifier;
import de.simonscholz.ICodeModifier;
import de.simonscholz.junit4converter.JUnit4Converter;

public class JUnit4ConversionJob extends CodeModifierJob {

	private ICodeModifier jUnit4Converter;

	public JUnit4ConversionJob(List<IJavaElement> selectedElements) {
		super(selectedElements, "Converting to JUnit 4");
	}

	@Override
	protected ICodeModifier getConverter() {
		if (null == jUnit4Converter) {
			jUnit4Converter = new DefaultCodeModifier(new JUnit4Converter());
		}
		return jUnit4Converter;
	}

}
