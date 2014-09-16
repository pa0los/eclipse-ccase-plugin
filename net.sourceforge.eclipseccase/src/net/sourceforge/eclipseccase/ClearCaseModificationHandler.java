/*******************************************************************************
 * Copyright (c) 2002, 2004 eclipse-ccase.sourceforge.net.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Matthew Conway - initial API and implementation
 *     IBM Corporation - concepts and ideas from Eclipse
 *     Gunnar Wagenknecht - new features, enhancements and bug fixes
 *******************************************************************************/
package net.sourceforge.eclipseccase;

import java.util.ArrayList;
import java.util.List;



import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Display;

import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;



/**
 * A simple file modification handler for the ClearCase integration.
 * <p>
 * Although this class is public it is not intended to be subclassed,
 * instanciated or called outside the Eclipse ClearCase integration.
 * </p>
 */
public class ClearCaseModificationHandler extends FileModificationValidator {

	/** constant for OK status */
	protected static final IStatus OK = ClearCaseProvider.OK_STATUS;

	/** constant for CANCEL status */
	protected static final IStatus CANCEL = ClearCaseProvider.CANCEL_STATUS;

	/**
	 * Constructor for ClearCaseModificationHandler.
	 * 
	 * @param provider
	 */
	protected ClearCaseModificationHandler() {
		// protected
	}

	/**
	 * Indicates if a file needs to be checked out.
	 * 
	 * @param file
	 * @return <code>true</code> if a file needs to be checked out
	 */
	protected boolean needsCheckout(IFile file) {

		// writable files don't need to be checked out
		if (file.isReadOnly()) {
			ClearCaseProvider provider = ClearCaseProvider
					.getClearCaseProvider(file);

			// if there is no provider, it's not a ClearCase file
			if (null != provider) {

				// ensure resource state is initialized
				provider.ensureInitialized(file);

				// needs checkout if file is managed
				return provider.isClearCaseElement(file);
			}
		}
		return false;
	}

	/**
	 * Returns a list of files that need to be checked out.
	 * 
	 * @param files
	 * @return a list of files that need to be checked out
	 */
	protected IFile[] getFilesToCheckout(IFile[] files) {

		// collect files that need to be checked out
		List<IFile> readOnlys = new ArrayList<IFile>();
		for (int i = 0; i < files.length; i++) {
			IFile iFile = files[i];
			if (needsCheckout(iFile)) {
				readOnlys.add(iFile);
			}
		}
		return readOnlys.toArray(new IFile[readOnlys.size()]);
	}

	/**
	 * Returns the ClearCase Team provider for all files.
	 * <p>
	 * This implementation requires all files to be in the same project.
	 * </p>
	 * 
	 * @param files
	 * @return the ClearCase Team provider for all files
	 */
	protected ClearCaseProvider getProvider(IFile[] files) {
		if (files.length > 0)
			return ClearCaseProvider.getClearCaseProvider(files[0]);
		return null;
	}

	/**
	 * Enables or disables refreshing in the ClearCase provider.
	 * 
	 * @param provider
	 * @param refreshResource
	 * 
	 * @return the old value
	 */
	protected boolean setResourceRefreshing(ClearCaseProvider provider,
			boolean refreshResource) {
		boolean old = provider.refreshResources;
		provider.refreshResources = refreshResource;
		return old;
	}

	/**
	 * Checks out the specified files.
	 * 
	 * @param files
	 * @return a status describing the result
	 */
	private IStatus checkout(final IFile[] files) {
		
		
		ClearCaseProvider provider = getProvider(files);
			
		if (PreventCheckoutHelper.isPreventedFromCheckOut(provider, files, ClearCasePreferences.isSilentPrevent())) {
			return CANCEL;
		}
		
		if(!PreventCheckoutHelper.isPromtedCoTypeOk()){
			return CANCEL;
		}


		if (ClearCasePreferences.isCheckoutAutoNever())
			return CANCEL;

		if (!ClearCasePreferences.isCheckoutAutoAlways()) {
			//CheckoutQuestionRunnable checkoutQuestion = new CheckoutQuestionRunnable();
			CheckoutQuestionRunnable checkoutQuestion = new CheckoutQuestionRunnable(files);
			getDisplay().syncExec(checkoutQuestion);
			int returncode = checkoutQuestion.getResult();
			if (checkoutQuestion.isRemember()) {
				if (returncode == IDialogConstants.YES_ID)
					ClearCasePreferences.setCheckoutAutoAlways();
				else if (returncode == IDialogConstants.NO_ID)
					ClearCasePreferences.setCheckoutAutoNever();
			}
			if (returncode != IDialogConstants.YES_ID)
				return new Status(IStatus.CANCEL, ClearCasePlugin.PLUGIN_ID,
						"Checkout operation failed, operation was cancelled by user.");
		}

		// check for provider
		if (null == provider)
			return new Status(IStatus.ERROR, ClearCaseProvider.ID,
					TeamException.NOT_CHECKED_OUT, "No ClearCase resources!",
					new IllegalStateException("Provider is null!"));

		// checkout
		try {
			synchronized (provider) {
				boolean refreshing = setResourceRefreshing(provider, false);
				try {
					if (ClearCasePreferences.isUseClearDlg()) {
						ClearDlgHelper.checkout(files);
					}
					for (int i = 0; i < files.length; i++) {
						IFile file = files[i];
						if (!ClearCasePreferences.isUseClearDlg()) {
							provider.checkout(new IFile[] { file },
									IResource.DEPTH_ZERO, null);
						}
						file.refreshLocal(IResource.DEPTH_ZERO, null);
					}
				} finally {
					setResourceRefreshing(provider, refreshing);
				}
			}
		} catch (CoreException ex) {
			return ex.getStatus();
		}
		return OK;
	}

	private Display getDisplay() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	private class CheckoutQuestionRunnable implements Runnable {

		private int dialogResult;
		private boolean remember;
		private IFile [] files;
		private StringBuffer fileListFormatted = new StringBuffer();
		
		public CheckoutQuestionRunnable(IFile [] files){
			this.files = files;
			formatFileList();
		}

		public void run() {
			MessageDialogWithToggle checkoutQuestion = new MessageDialogWithToggle(
					getDisplay().getActiveShell(),
					"ClearCase Checkout",
					null,
					"File/-s:\n" +fileListFormatted+"must be checked out to edit.\n\nProceed with checkout?",
					MessageDialog.QUESTION, new String[] {
							IDialogConstants.YES_LABEL,
							IDialogConstants.NO_LABEL }, 0,
					"Remember my decision", false);
			checkoutQuestion.open();
			dialogResult = checkoutQuestion.getReturnCode();
			remember = checkoutQuestion.getToggleState();
		}

		public int getResult() {
			return dialogResult;
		}

		public boolean isRemember() {
			return remember;
		}
		
		private void formatFileList(){
			if(files.length > 0){
				for (int i = 0; i < files.length; i++) {
					fileListFormatted.append(files [i].getFullPath()+"\n");
				}
			}
		}
			
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.team.FileModificationValidator#validateEdit
	 * (org.eclipse.core.resources.IFile[],
	 * org.eclipse.core.resources.team.FileModificationValidationContext)
	 */
	@Override
	public IStatus validateEdit(IFile[] files,
			FileModificationValidationContext context) {
		IFile[] readOnlyFiles = getFilesToCheckout(files);
		if (readOnlyFiles.length == 0)
			return OK;
		return checkout(readOnlyFiles);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IFileModificationValidator#validateSave(org
	 * .eclipse.core.resources.IFile)
	 */
	@Override
	public IStatus validateSave(IFile file) {
		if (!needsCheckout(file))
			return OK;
		return checkout(new IFile[] { file });
	}

	// protected boolean isPreventedFromCheckOut(Shell shell, ClearCaseProvider
	// provider, IResource[] resources,boolean silent) {
	// for (final IResource resource : resources) {
	//
	// if (provider.isPreventCheckout(resource) && !silent) {
	// PreventCheckoutQuestion question = new PreventCheckoutQuestion(resource);
	// PlatformUI.getWorkbench().getDisplay().syncExec(question);
	// if (question.isRemember()) {
	// ClearCasePreferences.setSilentPrevent();
	// }
	// return true;
	// }else if(provider.isPreventCheckout(resource) && silent){
	// //show no message.
	// return true;
	// }
	// }
	//				
	// return false;
	// }
	//	
	// public class PreventCheckoutQuestion implements Runnable {
	// private IResource resource;
	//
	// private int result;
	//
	// private boolean remember;
	//
	// public PreventCheckoutQuestion(IResource resource) {
	// this.resource = resource;
	// }
	//
	// public void run() {
	// MessageDialogWithToggle checkoutQuestion = new
	// MessageDialogWithToggle(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
	// Messages.getString("ClearCaseModificationHandler.infoDialog.title"),
	// null,
	// Messages.getString("ClearCaseModificationHandler.infoDialog.message.part1")+resource.getName()+" "+Messages.getString("ClearCaseModificationHandler.infoDialog.message.part2"),
	// MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0,
	// "Skip this dialog in the future!", false);
	// checkoutQuestion.open();
	// result = checkoutQuestion.getReturnCode();
	// remember = checkoutQuestion.getToggleState();
	// }
	//
	// public int getResult() {
	// return result;
	// }
	//
	// public boolean isRemember() {
	// return remember;
	// }
	//
	// }

}