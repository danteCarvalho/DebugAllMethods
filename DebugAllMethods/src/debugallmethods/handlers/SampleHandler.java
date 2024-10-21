package debugallmethods.handlers;
import java.util.List;
import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IWorkbenchWindow iWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			IWorkbenchPage iWorkbenchPage = iWorkbenchWindow.getActivePage();
			IEditorPart iEditorPart = iWorkbenchPage.getActiveEditor();
			if (iEditorPart != null) {
				IEditorInput iEditorInput = iEditorPart.getEditorInput();
				FileEditorInput fileEditorInput = (FileEditorInput) iEditorInput;
				IFile iFile = fileEditorInput.getFile();
				IJavaElement element = JavaUI.getEditorInputJavaElement(iEditorInput);
				ICompilationUnit iCompilationUnit = (ICompilationUnit) element;
				iCompilationUnit.becomeWorkingCopy(null);
				ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
				parser.setResolveBindings(true);
				parser.setBindingsRecovery(true);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				Map<String, String> options = JavaCore.getOptions();
				parser.setCompilerOptions(options);
				parser.setSource(iCompilationUnit);
				CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
				IType primaryType = iCompilationUnit.findPrimaryType();
				String classe = primaryType.getFullyQualifiedName();
				compilationUnit.accept(new ASTVisitor() {

					@Override
					public boolean visit(MethodDeclaration node) {
						try {
							Block methodBody = node.getBody();
							if (!analyzeBody(methodBody)) {
								int startLine = compilationUnit.getLineNumber(node.getStartPosition());
								String methodName = node.getName().getIdentifier();
								IMethodBinding methodBinding = node.resolveBinding();
								JDIDebugModel.createMethodBreakpoint(iFile, methodBinding.getDeclaringClass().getQualifiedName(), methodName, null, true, false, false, startLine, -1, -1, 0, true, null);
							}
							return super.visit(node);
						}
						catch (Exception e) {
							System.out.println("Error retrieving line number: " + e.getMessage());
						}
						return super.visit(node);
					}

					public boolean analyzeBody(Block methodBody) throws CoreException {
						if (methodBody != null && !methodBody.statements().isEmpty()) {
							List statements = methodBody.statements();
							for (Object object : statements) {
								Statement statement = (Statement) object;
								if (statement instanceof VariableDeclarationStatement) {
									VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
									List fragments = variableDeclarationStatement.fragments();
									for (Object object2 : fragments) {
										VariableDeclarationFragment fragment = (VariableDeclarationFragment) object2;
										if (fragment.getInitializer() != null) {
											int firstDebuggableLine = compilationUnit.getLineNumber(statement.getStartPosition());
											JDIDebugModel.createLineBreakpoint(iFile, classe, firstDebuggableLine, -1, -1, 0, true, null);
											return true;
										}
									}
								}
								else if (statement instanceof TryStatement) {
									TryStatement tryStatement = (TryStatement) statement;
									Block body = tryStatement.getBody();
									if (analyzeBody(body)) {
										return true;
									}
								}
								else {
									int firstDebuggableLine = compilationUnit.getLineNumber(statement.getStartPosition());
									JDIDebugModel.createLineBreakpoint(iFile, classe, firstDebuggableLine, -1, -1, 0, true, null);
									return true;
								}
							}
						}
						return false;
					}

				});
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

}
