package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.InputFile;
import name.abuchen.portfolio.datatransfer.json.JsonSecurityExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

public class ImportJsonHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, //
                    IEclipseContext context)
    {
        MenuHelper.getActiveClient(part).ifPresent(client -> runImport(part, shell, client));
    }

    private void runImport(MPart part, Shell shell, Client client)
    {

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
        fileDialog.setFilterExtensions(new String[] { "*.json", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        String fileName = fileDialog.open();

        if (fileName == null)
            return;

        JsonSecurityExtractor extractor = new JsonSecurityExtractor(client);
        
        Map<Extractor, List<Extractor.Item>> result = new HashMap<>();
        ArrayList<Exception> errors = new ArrayList<>();
        
        List<InputFile> files = Collections.singletonList(new Extractor.InputFile(new File(fileName)));
        List<Extractor.Item> items = extractor.extract(files, errors);
        
        result.put(extractor, items);

        Map<File, List<Exception>> e = new HashMap<>();
        if (!errors.isEmpty())
            e.put(files.get(0).getFile(), errors);

        IPreferenceStore preferences = ((PortfolioPart) part.getObject()).getPreferenceStore();
        ImportExtractedItemsWizard wizard = new ImportExtractedItemsWizard(client, preferences, result, e);
        Dialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
        dialog.open();
    }
}
