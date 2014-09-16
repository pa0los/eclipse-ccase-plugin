package net.sourceforge.eclipseccase.ui.operation;

import java.beans.PropertyChangeEvent;

import java.util.Iterator;

import java.util.ArrayList;

import java.util.List;

import java.beans.PropertyChangeListener;

import java.util.Observable;

import org.eclipse.compare.BufferedContent;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.compare.IContentChangeNotifier;

import org.eclipse.compare.IContentChangeListener;

import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;

import java.io.OutputStream;

import java.io.InputStream;

import java.io.IOException;

import org.eclipse.compare.IStreamContentAccessor;

import org.eclipse.core.resources.IResource;

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;

import org.eclipse.compare.structuremergeviewer.IStructureComparator;

import org.eclipse.compare.structuremergeviewer.DiffNode;

import net.sourceforge.eclipseccase.ClearCasePlugin;

import org.eclipse.core.runtime.Status;

import org.eclipse.team.core.TeamException;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.compare.structuremergeviewer.Differencer;

import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.compare.ITypedElement;

import net.sourceforge.eclipseccase.ClearCaseProvider;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IFile;

import net.sourceforge.eclipseccase.ui.compare.ClearCaseResourceNode;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.compare.CompareEditorInput;

public class VersionMergeInput extends CompareEditorInput{

	private ITypedElement left;

	private ITypedElement right;

	private ITypedElement ancestor;
		
	private boolean neverINTERNAL_MERGE = true;

	private boolean isSaving = false;

	private IFile resource;

	private Object result;
	
	private static List<PropertyChangeListener> listener = new ArrayList<PropertyChangeListener>();
	
	public  static final String INTERNAL_MERGE = "intMerge";
	

	public VersionMergeInput(CompareConfiguration configuration, IFile resource, String selected, String comparableVersion, String base, ClearCaseProvider provider) {
		super(configuration);
			
		this.resource = resource;

		right = comparableVersion != null ? new ClearCaseResourceNode(resource, comparableVersion, provider) : new ResourceNode(resource);

		// TODO: Invokers of this method should ensure that trees and contents
		// are prefetched
		left = new ResourceNode(resource);
		

		ancestor = base != null ? new ClearCaseResourceNode(resource, base, provider) : new ResourceNode(resource);

		configuration.setLeftImage(CompareUI.getImage(resource));
		if (left != null) {
			configuration.setLeftLabel(selected);
		}

		configuration.setRightImage(CompareUI.getImage(resource));
		if (right != null) {
			configuration.setRightLabel(comparableVersion);
		}

		configuration.setAncestorImage(CompareUI.getImage(resource));
		if (ancestor != null) {
			configuration.setAncestorLabel(base);
		}

	}
	
	
	

	/*
	 * (non-Javadoc) Method declared on CompareEditorInput
	 */
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		final boolean threeWay = ancestor != null;
		if (right == null || left == null) {
			setMessage("different"); //$NON-NLS-1$
			return null;
		}

		try {
			// do the diff
			Object result = null;
			monitor.beginTask("Merging ...", 30); //$NON-NLS-1$
			IProgressMonitor sub = new SubProgressMonitor(monitor, 30);
			sub.beginTask("Merging...", 100); //$NON-NLS-1$
			try {
				// add after setting contents, otherwise we end up in a loop
			    // makes sure that the diff gets re-run if we right-click and select Save on the left pane.
			    // Requires that we have a isSaving flag to avoid recursion
				BufferedContent l = null;	
			if(left instanceof BufferedContent){
				l = (BufferedContent)left;
			}	
			l.addContentChangeListener( new IContentChangeListener() {            
			        public void contentChanged(IContentChangeNotifier source) {
			            if (!isSaving) {
			                try {                    
			                    saveChanges(new NullProgressMonitor());
			                } catch (CoreException e) {
			                    e.printStackTrace();
			                }
			            }
			        }            
			    });
				// instead of just DiffNode
				Differencer d = new Differencer() {
					protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
						return new MyDiffNode((IDiffContainer) data, result, (ITypedElement) ancestor, (ITypedElement) left, (ITypedElement) right);
					}
				};
				result = d.findDifferences(threeWay, sub, null, ancestor, left, right);
				
			} finally {
				sub.done();
			}
			return result;
		} catch (OperationCanceledException e) {
			throw new InterruptedException(e.getMessage());
		} catch (RuntimeException e) {
			handle(e);
			return null;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Handles a random exception and gives error message.
	 */
	private void handle(Exception e) {
		// create a status
		Throwable t = e;
		// unwrap the invocation target exception
		if (t instanceof InvocationTargetException) {
			t = ((InvocationTargetException) t).getTargetException();
		}
		IStatus error;
		if (t instanceof CoreException) {
			error = ((CoreException) t).getStatus();
		} else if (t instanceof TeamException) {
			error = ((TeamException) t).getStatus();
		} else {
			error = new Status(IStatus.ERROR, ClearCasePlugin.PLUGIN_ID, 1, "Runtime Exception occurred", t); //$NON-NLS-1$
		}
		setMessage(error.getMessage());
		if (!(t instanceof TeamException)) {
			ClearCasePlugin.log(error.getSeverity(), error.getMessage(), t);
		}
	}

	

	// save if any changes
	public void saveChanges(IProgressMonitor pm) throws CoreException {
		if (left instanceof IStreamContentAccessor) {
			try {
				isSaving = true;
				super.saveChanges(pm);
				//Since left is the only file editable.
				IStreamContentAccessor sca = (IStreamContentAccessor) left;
				InputStream contents = sca.getContents();
				File f = new File(resource.getLocation().toOSString());
				OutputStream out = new FileOutputStream(f);
				byte buf[] = new byte[1024];
				int len;
				while ((len = contents.read(buf)) > 0)
					out.write(buf, 0, len);
				out.close();
				neverINTERNAL_MERGE = false;
				flushLeftViewers(pm);
				notifyListeners();//Notifiy MergeView of save has taken place.
				
			} catch (Exception e) {

			} finally {
				isSaving = false;
			}
		}
	}

	public boolean isSaveNeeded() {
		if (neverINTERNAL_MERGE) {
			return true;
		} else {
			return super.isSaveNeeded();
		}
	}
	
	
	
	public static class MyDiffNode extends DiffNode {
		public MyDiffNode(IDiffContainer parent, int kind, ITypedElement ancestor, ITypedElement left, ITypedElement right) {
			super(parent, kind, ancestor, left, right);
		}
		
	}
	
	//To mark as merged in MergeView.
	private void notifyListeners() {
	    for (Iterator iterator = listener.iterator(); iterator.hasNext();) {
	      PropertyChangeListener name = (PropertyChangeListener) iterator
	          .next();
	      name.propertyChange(new PropertyChangeEvent(this, INTERNAL_MERGE , resource, null));

	    }
	  }

	  public static void addChangeListener(PropertyChangeListener newListener) {
	    listener.add(newListener);
	  }

}
