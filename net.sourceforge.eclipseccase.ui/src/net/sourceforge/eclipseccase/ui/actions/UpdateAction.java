package net.sourceforge.eclipseccase.ui.actions;

import net.sourceforge.eclipseccase.ClearCaseProvider;
import net.sourceforge.eclipseccase.ui.console.ConsoleOperationListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;

public class UpdateAction extends ClearCaseWorkspaceAction {
	@Override
	public void execute(IAction action) {
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					IResource[] resources = getSelectedResources();
					beginTask(monitor, "Updating...", resources.length);
					ConsoleOperationListener opListener = new ConsoleOperationListener(monitor);
					for (int i = 0; i < resources.length; i++) {
						IResource resource = resources[i];
						ClearCaseProvider provider = ClearCaseProvider.getClearCaseProvider(resource);
						if (provider != null) {
							provider.setOperationListener(opListener);
							provider.get(new IResource[] { resource }, IResource.DEPTH_ZERO, subMonitor(monitor));
						}
					}
				} finally {
					monitor.done();
				}
			}
		};
		executeInBackground(runnable, "Updating resources from ClearCase");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
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
			if (!provider.isSnapShot(resource))
				return false;
		}
		return true;
	}

}