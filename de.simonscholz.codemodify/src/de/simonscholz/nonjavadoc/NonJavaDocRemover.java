package de.simonscholz.nonjavadoc;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import de.simonscholz.ICompilationUnitModifier;

public class NonJavaDocRemover implements ICompilationUnitModifier {

	private static final int LINE_DELIMITER_LENGTH = 1;

	private int offset;
	private IDocument document;

	@Override
	public void modifyCompilationUnit(CompilationUnit astRoot,
			IProgressMonitor monitor) throws JavaModelException, CoreException,
			BadLocationException {
		final ICompilationUnit adapter = (ICompilationUnit) astRoot
				.getJavaElement().getAdapter(IOpenable.class);
		if (adapter != null) {
			document = new Document(adapter.getSource());
			List<Comment> commentList = astRoot.getCommentList();
			offset = 0;
			for (Comment comment : commentList) {
				comment.accept(new ASTVisitor() {
					@Override
					public boolean visit(BlockComment node) {
						int startPosition = node.getStartPosition();
						int endPosition = startPosition + node.getLength();

						try {
							int lineOfOffset = document
									.getLineOfOffset(startPosition - offset);
							startPosition = document
									.getLineOffset(lineOfOffset);
							document.replace(startPosition
									- LINE_DELIMITER_LENGTH, endPosition
									- startPosition + LINE_DELIMITER_LENGTH
									- offset, "");
						} catch (BadLocationException e) {
							e.printStackTrace();
						}

						offset = endPosition - startPosition
								+ LINE_DELIMITER_LENGTH;
						return super.visit(node);
					}
				});
			}
			adapter.getBuffer().setContents(document.get());
			adapter.save(monitor, true);
		}
	}
}
