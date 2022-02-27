package name.abuchen.portfolio.ui.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Strings;

import name.abuchen.portfolio.datatransfer.json.JSecurityMetaData;
import name.abuchen.portfolio.datatransfer.json.SecurityMetaDataTransfer;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

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

        SecurityMetaDataTransfer securityMetaDataTransfer = new SecurityMetaDataTransfer();
        List<JSecurityMetaData> securitiesToImport = securityMetaDataTransfer.validate(fileName);

        List<Security> newSecurities = new ArrayList<>();
        List<Security> updateSecurities = new ArrayList<>();
        List<Security> allExistingSecurities = client.getSecurities();
        for (JSecurityMetaData jSecurityMetaData : securitiesToImport)
        {
            String isinToImport = jSecurityMetaData.getIsin();
            if (! Strings.isNullOrEmpty(isinToImport))
            {
                Optional<Security> existingSecurity = allExistingSecurities.stream()
                                .filter(s -> isinToImport.equals(s.getIsin())).findAny();
                if (existingSecurity.isPresent())
                {
                    updateSecurities.add(existingSecurity.get());
                }
                else
                {
                    Security newSecurity = new Security();
                    newSecurity.setIsin(isinToImport);
                    newSecurities.add(newSecurity);
                }
            }
        }

        if (MessageDialog.openConfirm(shell, "Import JSON file", "Import " + newSecurities.size() + " new and "
                        + updateSecurities.size() + " updated securities?"))
        {
            System.out.println("ImportJsonHandler.runImport()");
        }
    }
}
