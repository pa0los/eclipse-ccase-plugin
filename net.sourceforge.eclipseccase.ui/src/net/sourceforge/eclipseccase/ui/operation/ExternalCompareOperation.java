package net.sourceforge.eclipseccase.ui.operation;

import java.io.*;
import net.sourceforge.eclipseccase.*;
import net.sourceforge.eclipseccase.diff.AbstractExternalToolCommands;
import net.sourceforge.eclipseccase.diff.DiffFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

public class ExternalCompareOperation extends Thread {

	private ClearCaseProvider provider;

	private String comparableVersion;

	private IResource resource;

	private boolean differentView = false;

	public ExternalCompareOperation(IResource resource, String comparableVersion, ClearCaseProvider provider, boolean differentView) {
		this.resource = resource;
		this.comparableVersion = comparableVersion;
		this.provider = provider;
		this.differentView = differentView;

	}

	@Override
	public void run() {
		Job job = new Job("Compare") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Compare started...", 10);
				// Run long running task here
				// Add a factory here that can decide which launcher to use.
				AbstractExternalToolCommands diff = DiffFactory.getDiffTool(ClearCasePreferences.getExtDiffTool());
				// Dont use version extended path. Since view selects current
				// version.

				String vExtPath1 = resource.getLocation().toOSString();// File
																		// in
																		// view.
				String vExtPath2;
				if (differentView) {
					// In this case comparableVersion is a view name.
					vExtPath2 = "/view/" + comparableVersion + resource.getLocation().toOSString();
				} else {
					vExtPath2 = resource.getLocation().toOSString() + "@@" + comparableVersion;
				}

				// Since we start eclipse in a view we also start external
				// editor in a view and file not in snapshot view must be
				// loaded.
				File tempFile = null;

				try {
					StateCache cache = StateCacheFactory.getInstance().get(resource);
					if (cache.isSnapShot()) {
						tempFile = File.createTempFile("eclipseccase", null);
						tempFile.delete();
						tempFile.deleteOnExit();
						provider.copyVersionIntoSnapShot(tempFile.getPath(), vExtPath2);
						System.out.println("TempFilePath is " + tempFile.getPath());
						// now we should have the snapshot version.
						diff.twoWayDiff(vExtPath1, tempFile.getPath());
					} else {
						diff.twoWayDiff(vExtPath1, vExtPath2);
					}
				} catch (FileNotFoundException e) {
					return new Status(IStatus.WARNING, "net.sourceforge.eclipseccase.ui.compare", "Internal, could not find file to compare with " + vExtPath1, e);
				} catch (IOException e) {
					return new Status(IStatus.WARNING, "net.sourceforge.eclipseccase.ui.compare", "Internal, Could not create temp file for predecessor: " + vExtPath1, e);
				}

				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);// To make sure job is NOT blocking gui or you are
							// asked to put proc in background.
		job.schedule();

	}

}
