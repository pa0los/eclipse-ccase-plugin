package net.sourceforge.eclipseccase.ui.actions;

import net.sourceforge.eclipseccase.views.historyviewer.JFHistoryViewer;

import net.sourceforge.eclipseccase.ClearCasePlugin;

import java.util.Vector;
import net.sourceforge.clearcase.*;
import net.sourceforge.eclipseccase.ClearCaseProvider;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 * Pulls up the clearcase history
 */
public class HistoryAction extends ClearCaseWorkspaceAction {
	IResource[] resources = null;
	String fileVersion = null;
	IResource forceResource = null;
	
	public void setResource(IResource Resource) {
		this.forceResource = Resource;
	}
	
	public void setFileVersion(String fileVersion) {
		this.fileVersion = fileVersion;
	}

	private JFHistoryViewer view = null;

	/**
	 * {@inheritDoc

	 */
	@Override
	public boolean isEnabled() {
		IResource[] resources = getSelectedResources();
		if (resources.length == 0)
			return false;
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			ClearCaseProvider provider = ClearCaseProvider.getClearCaseProvider(resource);
			if (provider == null || provider.isUnknownState(resource) || provider.isIgnored(resource) || !provider.isClearCaseElement(resource))
				return false;
		}
		return true;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void execute(IAction action) {
		
		if(forceResource != null)
		{
			resources = new IResource[1];
			resources[0] = forceResource;
		}
		else
		{
			resources = getSelectedResources();
		}
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			ClearCaseProvider provider = ClearCaseProvider.getClearCaseProvider(resource);
			if (provider == null) {
				return;
			}
		}
		try {
			view = (JFHistoryViewer) PlatformUI.getWorkbench().getActiveWorkbenchWindow().
					getActivePage().showView("net.sourceforge.eclipseccase.views.HistoryViewer.JFHistoryViewer");
		} catch (Exception e) {
			e.printStackTrace();
		}

		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					if (resources != null && resources.length > 0) {
						IResource resource = resources[0];
						String path;
						
						if(fileVersion != null)
						{	
							path = fileVersion;
						}
						else
						{
							path = resource.getLocation().toOSString();
						}
						
						// ClearCaseInterface cci =
						// ClearCase.createInterface(ClearCase.INTERFACE_CLI);
						ClearCaseInterface cci = ClearCasePlugin.getEngine();
						Vector<ElementHistory> result = cci.getElementHistory(path);
						view.setHistoryInformation(resource, result);
//						ModelProvider.INSTANCE.setElements(result);
//						Display.getDefault().asyncExec(new Runnable() {
//				               public void run() {
//									view.getViewer().refresh(true);
//				               }
//				            });
					}
				} catch (Exception e) {
					System.out.println(e);
				} finally {
					monitor.done();
				}
			}
		};

		executeInBackground(runnable, "History");

	}

}
