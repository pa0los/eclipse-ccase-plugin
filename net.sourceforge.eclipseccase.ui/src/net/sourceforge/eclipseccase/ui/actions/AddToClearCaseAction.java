package net.sourceforge.eclipseccase.ui.actions;

import net.sourceforge.eclipseccase.*;
import net.sourceforge.eclipseccase.ui.CommentDialog;
import net.sourceforge.eclipseccase.ui.UcmActivity;
import net.sourceforge.eclipseccase.ui.console.ConsoleOperationListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;

public class AddToClearCaseAction extends ClearCaseWorkspaceAction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void execute(IAction action) {
		final IResource[] resources = getSelectedResources();
		final ClearCaseProvider provider = ClearCaseProvider.getClearCaseProvider(resources[0]);

		String maybeComment = "";
		int maybeDepth = IResource.DEPTH_ZERO;

		if (!ClearCasePreferences.isUseClearDlg() && ClearCasePreferences.isCommentAdd()) {
			CommentDialog dlg = new CommentDialog(getShell(), "Add to ClearCase comment");
			if (dlg.open() == Window.CANCEL)
				return;
			maybeComment = dlg.getComment();
			maybeDepth = dlg.isRecursive() ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO;
		}

		final String comment = maybeComment;
		final int depth = maybeDepth;

		// UCM checkout.
		if (ClearCasePreferences.isUCM() && !ClearCasePreferences.isUseClearDlg()) {
			if (UcmActivity.checkoutWithActivity(provider, resources, getShell()))
				// no checkout
				return;
		}

		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					beginTask(monitor, "Adding...", resources.length);
					ConsoleOperationListener opListener = new ConsoleOperationListener(monitor);
					if (provider != null) {
						if (ClearCasePreferences.isUseClearDlg()) {
							monitor.subTask("Executing ClearCase user interface...");
							ClearDlgHelper.add(resources);
						} else {
							provider.setComment(comment);
							provider.setOperationListener(opListener);
							provider.add(resources, depth, subMonitor(monitor));
						}
					}
				} finally {
					updateActionEnablement();
					monitor.done();
				}
			}

		};

		executeInBackground(runnable, "Adding resources to ClearCase");
	}

	private static final String DEBUG_ID = "AddToClearCaseAction";

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
			if (provider == null || provider.isUnknownState(resource) || provider.isIgnored(resource))
				return false;

			// Projects may be the view directory containing the VOBS, if so,
			// don't want to be able to add em, or any resource diretcly under
			// them
			if (resource.getType() == IResource.PROJECT && !provider.isClearCaseElement(resource)) {
				ClearCasePlugin.debug(DEBUG_ID, "disabled for project outside CC: " + resource);
				return false;
			}
			if (resource.getParent().getType() == IResource.PROJECT && !provider.isClearCaseElement(resource.getParent())) {
				ClearCasePlugin.debug(DEBUG_ID, "disabled for " + resource + " because parent is project outside CC: " + resource.getParent());
				return false;
			}
			if (provider.isClearCaseElement(resource)) {
				ClearCasePlugin.debug(DEBUG_ID, "disabled for " + resource + " because it already is CC element");
				return false;
			}
		}
		return true;
	}

}