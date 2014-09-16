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
package net.sourceforge.eclipseccase.ui;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A ClearCase operation that can be run in the foreground or as background job.
 * 
 * @author Gunnar Wagenknecht (gunnar@wagenknecht.org)
 */
public class ClearCaseOperation extends TeamOperation {

	/** the job name */
	private String jobName;

	/** the scheduling rule */
	private ISchedulingRule rule;

	/** indicates if this is a background job */
	private boolean runAsJob;

	/** the workspace runnable */
	private IWorkspaceRunnable runnable;

	/**
	 * Creates a new instance.
	 * 
	 * @param part
	 */
	public ClearCaseOperation(IWorkbenchPart part, ISchedulingRule rule, IWorkspaceRunnable runnable, boolean runAsJob, String jobName) {
		super(part);
		this.rule = rule;
		this.runnable = runnable;
		this.runAsJob = runAsJob;
		this.jobName = jobName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
	 */
	@Override
	protected boolean canRunAsJob() {
		return runAsJob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#getJobName()
	 */
	@Override
	protected String getJobName() {
		if (null == jobName)
			return super.getJobName();

		return jobName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#getSchedulingRule()
	 */
	@Override
	protected ISchedulingRule getSchedulingRule() {
		return rule;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#isPostponeAutobuild()
	 */
	@Override
	protected boolean isPostponeAutobuild() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core
	 * .runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			ResourcesPlugin.getWorkspace().run(runnable, rule, IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException ex) {
			throw new InvocationTargetException(ex, jobName + ": " + ex.getMessage());
		} catch (OperationCanceledException ex) {
			throw new InterruptedException();
		}
	}
}