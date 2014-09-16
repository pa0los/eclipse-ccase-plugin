/*******************************************************************************
 * Copyright (c) 2002, 2004 eclipse-ccase.sourceforge.net.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Matthew Conway - initial API and implementation
 *     IBM Corporation - concepts and ideas taken from Eclipse code
 *     Gunnar Wagenknecht - reworked to Eclipse 3.0 API and code clean-up
 *******************************************************************************/
package net.sourceforge.eclipseccase;





import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.clearcase.ClearCase;
import net.sourceforge.clearcase.ClearCaseException;
import net.sourceforge.clearcase.ClearCaseInterface;
import net.sourceforge.eclipseccase.tools.XMLWriter;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * The main plugin class to be used in the desktop.
 */
@SuppressWarnings("deprecation")
public class ClearCasePlugin extends Plugin {

	private static final BASE64Decoder BASE64_DECODER = new BASE64Decoder();

	private static final BASE64Encoder BASE64_ENCODER = new BASE64Encoder();

	/** job family for all clearcase operations */
	public static final Object FAMILY_CLEARCASE_OPERATION = new Object();

	/** the scheduling rule for the whole clearcase engine */
	public static final ISchedulingRule RULE_CLEARCASE_ENGING = new ISchedulingRule() {

		public boolean contains(ISchedulingRule rule) {
			return RULE_CLEARCASE_ENGING == rule;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return RULE_CLEARCASE_ENGING == rule;
		}
	};

	/** the scheduling rule for the refresh jobs */
	public static final ISchedulingRule RULE_CLEARCASE_REFRESH = new ISchedulingRule() {

		public boolean contains(ISchedulingRule rule) {
			// can contain engine and refresh rules
			return RULE_CLEARCASE_ENGING == rule
					|| RULE_CLEARCASE_REFRESH == rule;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			// conflict with engine
			// conflict with refresh
			// conflict with workspace (fix for 1055293)
			return RULE_CLEARCASE_ENGING == rule
					|| RULE_CLEARCASE_REFRESH == rule
					|| getWorkspace().getRuleFactory().buildRule() == rule;
		}
	};

	/** file name fo the history file */
	private static final String COMMENT_HIST_FILE = "commentHistory.xml"; //$NON-NLS-1$

	private static IPath debug = null;

	/** xml element name */
	static final String ELEMENT_COMMENT = "comment"; //$NON-NLS-1$

	/** xml element name */
	static final String ELEMENT_COMMENT_HISTORY = "comments"; //$NON-NLS-1$

	/** maximum comments to remember */
	static final int MAX_COMMENTS = 10;

	/** the shared instance */
	private static ClearCasePlugin plugin;

	/** the plugin id */
	public static final String PLUGIN_ID = "net.sourceforge.eclipseccase"; //$NON-NLS-1$

	/** The previously remembered comment */
	static LinkedList<String> previousComments = new LinkedList<String>();

	/** constant (value <code>UTF-8</code>) */
	public static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

	/** debug option */
	private static final String DEBUG_OPTION_PROVIDER = ClearCasePlugin.PLUGIN_ID
			+ "/debug/provider"; //$NON-NLS-1$

	/** debug option */
	private static final String DEBUG_OPTION_PROVIDER_IGNORED_RESOURCES = ClearCasePlugin.PLUGIN_ID
			+ "/debug/provider/ignoredResources"; //$NON-NLS-1$

	/** debug option */
	private static final String DEBUG_OPTION_PLUGIN = ClearCasePlugin.PLUGIN_ID
			+ "/debug/plugin"; //$NON-NLS-1$

	/** debug option */
	private static final String DEBUG_OPTION_STATE_CACHE = ClearCasePlugin.PLUGIN_ID
			+ "/debug/stateCache"; //$NON-NLS-1$

	/** debug option */
	private static final String DEBUG_OPTION_UPDATE_QUEUE = ClearCasePlugin.PLUGIN_ID
			+ "/debug/updateQueue"; //$NON-NLS-1$

	/** debug option */
	private static final String DEBUG_OPTION_SUBPROCESS = ClearCasePlugin.PLUGIN_ID
			+ "/debug/subprocess"; //$NON-NLS-1$
	
	/** debug option */
	private static final String DEBUG_OPTION_UCM = ClearCasePlugin.PLUGIN_ID
			+ "/debug/ucm"; //$NON-NLS-1$

	/** indicates if debugging is enabled */
	public static boolean DEBUG = false;
	
	// the list of all repositories currently handled by this provider
	private ClearCaseRepositories repositories;

	/**
	 * Configures debug settings.
	 */
	static void configureDebugOptions() {
		if (ClearCasePlugin.getDefault().isDebugging()) {

			if (getDebugOption(DEBUG_OPTION_PROVIDER)) {
				trace("debugging " + DEBUG_OPTION_PROVIDER); //$NON-NLS-1$
				ClearCasePlugin.DEBUG_PROVIDER = true;
			}

			if (getDebugOption(DEBUG_OPTION_PROVIDER_IGNORED_RESOURCES)) {
				trace("debugging " + DEBUG_OPTION_PROVIDER_IGNORED_RESOURCES); //$NON-NLS-1$
				ClearCasePlugin.DEBUG_PROVIDER_IGNORED_RESOURCES = true;
			}

			if (getDebugOption(DEBUG_OPTION_PLUGIN)) {
				trace("debugging " + DEBUG_OPTION_PLUGIN); //$NON-NLS-1$
				ClearCasePlugin.DEBUG = true;
			}

			if (getDebugOption(DEBUG_OPTION_STATE_CACHE)) {
				trace("debugging " + DEBUG_OPTION_STATE_CACHE); //$NON-NLS-1$
				ClearCasePlugin.DEBUG_STATE_CACHE = true;
			}

			if (getDebugOption(DEBUG_OPTION_UPDATE_QUEUE)) {
				trace("debugging " + DEBUG_OPTION_UPDATE_QUEUE); //$NON-NLS-1$
				ClearCasePlugin.DEBUG_UPDATE_QUEUE = true;
			}
			
			if (getDebugOption(DEBUG_OPTION_UCM)) {
				trace("debugging " + DEBUG_OPTION_UCM); //$NON-NLS-1$
				ClearCasePlugin.DEBUG_UCM = true;
			}

			if (getDebugOption(DEBUG_OPTION_SUBPROCESS)) {
				trace("debugging " + DEBUG_OPTION_SUBPROCESS); //$NON-NLS-1$
				ClearCasePlugin.getEngine().setDebugLevel(100);
			}

			String[] args = Platform.getCommandLineArgs();
			for (int i = 0; i < args.length; i++) {
				if ("-debugClearCase".equalsIgnoreCase(args[i].trim())) { //$NON-NLS-1$
					debug = Platform.getLocation()
							.append("clearcase.debug.log"); //$NON-NLS-1$
					break;
				}
			}
		}
	}

	/**
	 * Returns the value of the specified debug option.
	 * 
	 * @param optionId
	 * @return <code>true</code> if the option is enabled
	 */
	static boolean getDebugOption(String optionId) {
		String option = Platform.getDebugOption(optionId);
		return option != null ? Boolean.valueOf(option).booleanValue() : false;
	}

	/**
	 * Prints out a trace message.
	 * 
	 * @param message
	 */
	public static void trace(String message) {
		System.out.println("**ClearCase** " + message); //$NON-NLS-1$
	}

	/**
	 * Prints out a trace message.
	 * 
	 * @param traceId
	 * @param message
	 */
	public static void trace(String traceId, String message) {
		trace("[" + traceId + "] " + message); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** the file modification validator */
	private ClearCaseModificationHandler clearCaseModificationHandler = new ClearCaseModificationHandler();

	/**
	 * Prints out a debug string.
	 * 
	 * @param id
	 * @param message
	 */
	public static void debug(String id, String message) {
		if (!isDebug())
			return;

		BufferedWriter debugWriter = null;
		FileWriter debugFileWriter = null;
		try {

			File debugFile = debug.toFile();
			debugFile.createNewFile();
			debugFileWriter = new FileWriter(debugFile, true);
			debugWriter = new BufferedWriter(debugFileWriter);
		} catch (Exception e) {
			if (null != debugFileWriter) {
				try {
					debugFileWriter.close();
				} catch (IOException e1) {
					// ignore
				}
			}
			log(IStatus.ERROR, Messages
					.getString("ClearCasePlugin.error.debug") + debug, e); //$NON-NLS-1$
			debug = null;
			return;
		}

		try {
			debugWriter.write(id + "\t" + message + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			debugWriter.flush();
			debugWriter.close();
		} catch (Exception e) {
			if (null != debugWriter) {
				try {
					debugWriter.close();
				} catch (IOException e1) {
					// ignore
				}
			}
			log(IStatus.ERROR, Messages
					.getString("ClearCasePlugin.error.debug") + debug, e); //$NON-NLS-1$
			debug = null;
		}
	}

	/**
	 * Returns the ClearCase engine for performing ClearCase operations.
	 * <p>
	 * If no engine is available <code>null</code> is returned.
	 * </p>
	 * 
	 * @return the ClearCase engine (maybe <code>null</code>)
	 */
	public static ClearCaseInterface getEngine() {
		ClearCaseInterface impl = null;
		try {
			impl = ClearCasePlugin.getDefault().getClearCase();
		} catch (CoreException e) {
			log(IStatus.ERROR, Messages
					.getString("ClearCasePlugin.error.noClearCase"), e); //$NON-NLS-1$
		}
		return impl;
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static ClearCasePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace.
	 * 
	 * @return the workspace
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Indicates if debug output is enabled.
	 * 
	 * @return <code>true</code> if debug mode is enabled
	 */
	public static boolean isDebug() {
		return null != debug;
	}

	/**
	 * Logs an exception with the specified severity an message.
	 * 
	 * @param severity
	 * @param message
	 * @param ex
	 *            (maybe <code>null</code>)
	 */
	public static void log(int severity, String message, Throwable ex) {
		ILog log = ClearCasePlugin.getDefault().getLog();
		log.log(new Status(severity, ClearCasePlugin.PLUGIN_ID, severity,
				message, ex));
	}

	/**
	 * Logs an error message with the specified exception.
	 * 
	 * @param message
	 * @param ex
	 *            (maybe <code>null</code>)
	 */
	public static void log(String message, Throwable ex) {
		log(IStatus.ERROR, message, ex);
	}

	private ClearCaseInterface clearcaseImpl;

	/** debug flag */
	public static boolean DEBUG_PROVIDER = false;

	/** debug flag */
	public static boolean DEBUG_PROVIDER_IGNORED_RESOURCES = false;

	/** debug flag */
	public static boolean DEBUG_STATE_CACHE = false;

	/** debug flag */
	public static boolean DEBUG_UPDATE_QUEUE = false;
	
	/** debug flag */
	public static boolean DEBUG_UCM = false;

	/**
	 * The constructor.
	 */
	public ClearCasePlugin() {
		super();
		plugin = this;
	}

	/**
	 * Method addComment.
	 * 
	 * @param string
	 */
	public void addComment(String comment) {
		synchronized (previousComments) {
			// ensure the comment is UTF-8 encoded
			try {
				comment = new String(comment.getBytes(UTF_8));
			} catch (UnsupportedEncodingException ex) {
				return;
			}

			// remove existing comment (avoid duplicates)
			if (previousComments.contains(comment)) {
				previousComments.remove(comment);
			}

			// insert the comment as the first element
			previousComments.addFirst(comment);

			// check length
			while (previousComments.size() > MAX_COMMENTS) {
				previousComments.removeLast();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sourceforge.clearcase.simple.IClearCaseDebugger#debugClearCase(java
	 * .lang.String, java.lang.String)
	 */
	public void debugClearCase(String id, String message) {
		debug(id, message);
	}

	/**
	 * Returns the current ClearCase engine that can be used for performing
	 * ClearCase operations.
	 * 
	 * <p>
	 * The engine type depends on the current platform and on preference
	 * settings. It is cached internally. After changing the preferences you
	 * have to do a reset (see {@link #resetClearCase()}.
	 * </p>
	 * 
	 * @return the ClearCase engine
	 * @throws CoreException
	 *             if no engine is available
	 */
	public ClearCaseInterface getClearCase() throws CoreException {
		try {
			if (clearcaseImpl == null) {
				if (DEBUG) {
					trace("initializing clearcase engine"); //$NON-NLS-1$
				}

				if (ClearCasePreferences.isUseSingleProcess()) {
					if (DEBUG) {
						trace("using default engine"); //$NON-NLS-1$
					}
					clearcaseImpl = ClearCase
							.createInterface(ClearCase.INTERFACE_CLI_SP);
				} else {
					if (DEBUG) {
						trace("using old cleartool process"); //$NON-NLS-1$
					}
					clearcaseImpl = ClearCase
							.createInterface(ClearCase.INTERFACE_CLI);
				}

			}
			return clearcaseImpl;
		} catch (ClearCaseException e) {
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							ClearCasePlugin.PLUGIN_ID,
							TeamException.UNABLE,
							Messages
									.getString("ClearCasePlugin.error.noValidClearCase"), e)); //$NON-NLS-1$
		}
	}

	/**
	 * Answer the list of comments that were previously used when committing.
	 * 
	 * @return String[]
	 */
	public String[] getPreviousComments() {
		String[] comments = previousComments
				.toArray(new String[previousComments.size()]);

		// encode all strings to the platform default encoding
		for (int i = 0; i < comments.length; i++) {
			comments[i] = new String(comments[i].getBytes());
		}

		return comments;
	}

	/**
	 * Indicates if this plugin runs on a Microsoft Windows operating system.
	 * 
	 * @return <code>true</code> if this is a Windows operating system,
	 *         <code>false</code> otherwise
	 */
	public static boolean isWindows() {
		return Constants.OS_WIN32.equals(Platform.getOS());
	}

	/**
	 * Loads the comment history.
	 */
	private void loadCommentHistory() {
		IPath pluginStateLocation = getStateLocation()
				.append(COMMENT_HIST_FILE);
		File file = pluginStateLocation.toFile();
		if (!file.exists())
			return;
		try {
			BufferedInputStream is = new BufferedInputStream(
					new FileInputStream(file));
			try {
				readCommentHistory(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			getLog()
					.log(
							new Status(
									IStatus.ERROR,
									PLUGIN_ID,
									TeamException.UNABLE,
									Messages
											.getString("ClearCasePlugin.error.readingConfig.1") //$NON-NLS-1$
											+ e.getLocalizedMessage(), e));
		} catch (CoreException e) {
			getLog().log(e.getStatus());
		}
	}

	/**
	 * Builds (reads) the comment history from the specified input stream.
	 * 
	 * @param stream
	 * @throws CoreException
	 */
	private void readCommentHistory(InputStream stream) throws CoreException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			InputSource source = new InputSource(stream);
			Document document = parser.parse(source);
			NodeList list = document.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node instanceof Element) {
					if (ELEMENT_COMMENT_HISTORY.equals(((Element) node)
							.getTagName())) {
						synchronized (previousComments) {
							previousComments.clear();
							NodeList commentNodes = ((Element) node)
									.getElementsByTagName(ELEMENT_COMMENT);
							for (int j = 0; j < commentNodes.getLength()
									&& j < MAX_COMMENTS; j++) {
								Node commentNode = commentNodes.item(j);
								if (commentNode instanceof Element
										&& commentNode.hasChildNodes()) {
									// the first child is expected to be a text
									// node with our comment
									String comment = commentNode
											.getFirstChild().getNodeValue();
									if (null != comment) {
										comment = new String(BASE64_DECODER
												.decodeBuffer(comment), UTF_8);
										if (!previousComments.contains(comment)) {
											previousComments.addLast(comment);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							PLUGIN_ID,
							TeamException.UNABLE,
							Messages
									.getString("ClearCasePlugin.error.readingConfig.2"), e)); //$NON-NLS-1$
		}
	}

	/**
	 * Resets this plugin so that a new ClearCase engine will be created next
	 * time it is requested.
	 */
	public void resetClearCase() {
		// cancel pending refresh jobs
		StateCacheFactory.getInstance().getJobQueue().cancel(true);

		ClearCasePreferences.setGraphicalToolTimeout();

		// destroy clearcase engine
		if (clearcaseImpl != null) {
			clearcaseImpl.dispose();
			clearcaseImpl = null;
		}
	}

	/**
	 * Indicates if there are state refreshes pending.
	 * 
	 * @return <code>true</code> if there are state refreshes pending
	 */
	public boolean hasPendingRefreshes() {
		return !StateCacheFactory.getInstance().getJobQueue().isEmpty();
	}

	/**
	 * Cancels all pending state refreshes.
	 */
	public void cancelPendingRefreshes() {
		StateCacheFactory.getInstance().getJobQueue().cancel(true);
	}

	/**
	 * Saves the comment history.
	 * 
	 * @throws CoreException
	 */
	private void saveCommentHistory() throws CoreException {
		IPath pluginStateLocation = getStateLocation();
		File tempFile = pluginStateLocation.append(COMMENT_HIST_FILE + ".tmp") //$NON-NLS-1$
				.toFile();
		File histFile = pluginStateLocation.append(COMMENT_HIST_FILE).toFile();
		try {
			XMLWriter writer = new XMLWriter(new BufferedOutputStream(
					new FileOutputStream(tempFile)));
			try {
				writeCommentHistory(writer);
			} finally {
				writer.close();
			}
			if (histFile.exists()) {
				histFile.delete();
			}
			boolean renamed = tempFile.renameTo(histFile);
			if (!renamed)
				throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
						TeamException.UNABLE, MessageFormat.format(Messages
								.getString("ClearCasePlugin.error.renameFile"), //$NON-NLS-1$
								new Object[] { tempFile.getAbsolutePath() }),
						null));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
					TeamException.UNABLE, MessageFormat.format(Messages
							.getString("ClearCasePlugin.error.saveFile"), //$NON-NLS-1$
							new Object[] { histFile.getAbsolutePath() }), e));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		configureDebugOptions();

		// Disables plugin if clearcase is not available (throws CoreEx)
		getClearCase();

		// process deltas since last activated in another thread
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=67449
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=60566
		Job processSavedState = new Job(Messages
				.getString("savedState.jobName")) { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final IWorkspace workspace = ResourcesPlugin.getWorkspace();

					// add save participant and process delta atomically
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=67449
					workspace.run(new IWorkspaceRunnable() {

						public void run(IProgressMonitor progress)
								throws CoreException {
							StateCacheFactory cacheFactory = StateCacheFactory
									.getInstance();
							ISavedState savedState = workspace
									.addSaveParticipant(ClearCasePlugin.this,
											cacheFactory);
							if (savedState != null) {
								if (DEBUG) {
									trace("loading saved state"); //$NON-NLS-1$
								}
								cacheFactory.load(savedState);
								// the event type coming from the saved state is
								// always POST_AUTO_BUILD
								// force it to be POST_CHANGE so that the delta
								// processor can handle it
								savedState
										.processResourceChangeEvents(cacheFactory);
							}
							cacheFactory.setIsInitialized(true);
							workspace.addResourceChangeListener(cacheFactory,
									IResourceChangeEvent.POST_CHANGE);
						}
					}, monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		processSavedState.setSystem(!DEBUG);
		processSavedState.setPriority(Job.LONG);
		processSavedState.schedule(500);

		loadCommentHistory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);

		getWorkspace().removeResourceChangeListener(
				StateCacheFactory.getInstance());
		
		// save the state which includes the known repositories
		if (repositories != null) {
			repositories.shutdown();
		}

		StateCacheFactory.getInstance().getJobQueue().cancel();

		resetClearCase();

		saveCommentHistory();

		super.stop(context);

	}

	/**
	 * Writes the comment history to the specified writer.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	private void writeCommentHistory(XMLWriter writer) throws IOException {
		synchronized (previousComments) {
			writer.startTag(ELEMENT_COMMENT_HISTORY, null, false);
			for (int i = 0; i < previousComments.size() && i < MAX_COMMENTS; i++) {
				writer.printSimpleTag(ELEMENT_COMMENT, BASE64_ENCODER
						.encode(previousComments.get(i).getBytes(UTF_8)));
			}
			writer.endTag(ELEMENT_COMMENT_HISTORY);
		}
	}

	/**
	 * Returns the ClearCase modification handler.
	 * <p>
	 * Allthough this method is exposed in API it is not inteded to be called by
	 * clients.
	 * </p>
	 * 
	 * @return returns the ClearCase modification handler
	 */
	ClearCaseModificationHandler getClearCaseModificationHandler() {
		return clearCaseModificationHandler;
	}

	/**
	 * Sets the ClearCase modification handler.
	 * <p>
	 * Allthough this method is exposed in API it is not inteded to be called by
	 * clients.
	 * </p>
	 * 
	 * @param clearCaseModificationHandler
	 *            the ClearCase modification handler to set
	 */
	public void setClearCaseModificationHandler(
			ClearCaseModificationHandler clearCaseModificationHandler) {
		this.clearCaseModificationHandler = clearCaseModificationHandler;
	}
	
	/**
	 * get all the known repositories
	 */
	public ClearCaseRepositories getRepositories() {
	    if (repositories == null) {
	        // load the state which includes the known repositories
	        repositories = new ClearCaseRepositories();
	        repositories.startup();
	    }
		return repositories;
	}

}