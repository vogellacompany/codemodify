package de.simonscholz.lambdaconverter.jobs;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;

import de.simonscholz.CodeModifierJob;
import de.simonscholz.DefaultCodeModifier;
import de.simonscholz.ICodeModifier;
import de.simonscholz.junit4converter.JUnit4Converter;
import de.simonscholz.lambdaconverter.LambdaConverter;

public class LambdaConversionJob extends CodeModifierJob {

	private ICodeModifier lambdaConverter;

	public LambdaConversionJob(List<IJavaElement> selectedElements) {
		super(selectedElements, "Converting to lambda");
	}

	@Override
	protected ICodeModifier getConverter() {
		if (null == lambdaConverter) {
			lambdaConverter = new DefaultCodeModifier(new LambdaConverter());
		}
		return lambdaConverter;
	}

}
