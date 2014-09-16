package net.sourceforge.eclipseccase.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class CommentDialog extends Dialog {
	private CommentDialogArea commentDialogArea;

	private String title;

	/**
	 * Creates a new CommentDialog instance.
	 * 
	 * @param parentShell
	 * @param dialogTitle
	 */
	public CommentDialog(Shell parentShell, String dialogTitle) {
		super(parentShell);
		commentDialogArea = new CommentDialogArea(this, null);
		this.title = dialogTitle;
	}

	/**
	 * Creates a new CommentDialog instance.
	 * 
	 * @param parentShell
	 * @param dialogTitle
	 * @param commentStart
	 */
	public CommentDialog(Shell parentShell, String dialogTitle, String commentStart) {
		super(parentShell);
		commentDialogArea = new CommentDialogArea(this, null);
		commentDialogArea.setComment(commentStart);
		this.title = dialogTitle;
	}

	Button recursiveButton;

	boolean recursive = false;

	/** is the recusrive button enabled (default is true) */
	boolean recursiveEnabled = true;

	/**
	 * Gets the recursive.
	 * 
	 * @return Returns a boolean
	 */
	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * Sets the recursive.
	 * 
	 * @param recursive
	 *            The recursive to set
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * @see Dialog#createDialogArea(Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(title);
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout(1, true));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		commentDialogArea.createArea(composite);
		commentDialogArea.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == CommentDialogArea.OK_REQUESTED) {
					okPressed();
				}
			}
		});

		recursiveButton = new Button(composite, SWT.CHECK);
		recursiveButton.setText("Recurse");
		recursiveButton.setEnabled(recursiveEnabled);
		recursiveButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				recursive = recursiveButton.getSelection();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		// set F1 help
		// WorkbenchHelp.setHelp(composite,
		// IHelpContextIds.RELEASE_COMMENT_DIALOG);

		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		super.okPressed();
	}

	/**
	 * Returns the comment.
	 * 
	 * @return String
	 */
	public String getComment() {
		return commentDialogArea.getComment();
	}

	/**
	 * Returns the recursiveEnabled.
	 * 
	 * @return returns the recursiveEnabled
	 */
	public boolean isRecursiveEnabled() {
		return recursiveEnabled;
	}

	/**
	 * Sets the value of recursiveEnabled.
	 * 
	 * @param recursiveEnabled
	 *            the recursiveEnabled to set
	 */
	public void setRecursiveEnabled(boolean recursiveEnabled) {
		this.recursiveEnabled = recursiveEnabled;
	}

}