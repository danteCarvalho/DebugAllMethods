package debugallmethods.handlers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPage activePage = window.getActivePage();
		IEditorPart activeEditor = activePage.getActiveEditor();
		try {
			if (activeEditor != null) {
				IEditorInput input = activeEditor.getEditorInput();
				IFile arquivo = ((FileEditorInput) input).getFile();
				FileEditorInput path = (FileEditorInput) input;
				IPath path2 = path.getPath();
				File file = path2.toFile();
				Scanner scanner = new Scanner(file);
				String classe = null;
				int numeroLinha = 0;
				boolean add = false;
				List<String> debugHints = new ArrayList<String>();
				debugHints.add("(");
				debugHints.add("=");
				debugHints.add("+");
				debugHints.add("-");
				debugHints.add(">");
				debugHints.add("<");
				debugHints.add("return");
				while (scanner.hasNextLine()) {
					numeroLinha++;
					String linha = scanner.nextLine().trim();
					if (linha.isEmpty()) {
						continue;
					}
					if (classe == null) {
						if (linha.contains("package")) {
							classe = linha.replaceAll("package", "").trim().replaceAll(";", "").trim();
							classe += "." + file.getName().split("\\.")[0];
						}
						else {//se a classe não estiver em um pacote
							classe = file.getName().split("\\.")[0];
						}
						continue;
					}
					if (linha.startsWith("//")) {
						continue;
					}
					if (add) {
						if (linha.contains("public") || linha.contains("private") || linha.contains("protected")) {
							continue;
						}
						for (String hint : debugHints) {
							if (linha.contains(hint)) {
								JDIDebugModel.createLineBreakpoint(arquivo, classe, numeroLinha, -1, -1, 0, true, null);
								add = false;
								continue;
							}
						}
					}
					if (linha.contains("public") || linha.contains("private") || linha.contains("protected")) {
						if (linha.contains("(") && (!linha.contains(";"))) {
							add = true;
							continue;
						}
					}
				}
				scanner.close();
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
