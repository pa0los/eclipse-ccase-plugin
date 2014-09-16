/*******************************************************************************
 * Copyright (c) 2012 eclipse-ccase.sourceforge.net.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     eraonel - inital API and implementation
 *     IBM Corporation - concepts and ideas from Eclipse
 *******************************************************************************/
package net.sourceforge.eclipseccase.ui.preferences;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.IPropertyChangeListener;
import net.sourceforge.eclipseccase.ClearCasePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import net.sourceforge.eclipseccase.diff.PreferenceHelper;
import java.text.Collator;
import java.util.*;
import net.sourceforge.eclipseccase.ClearCasePlugin;
import net.sourceforge.eclipseccase.IClearCasePreferenceConstants;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author eraonel
 *
 */
/**
 * The main preference page for the Eclipse ClearCase integration.
 */
public class DiffMergePreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IClearCasePreferenceConstants {
	private String selectedDiffTool = "";// Initial value.

	private String selectedMergeTool = "";

	private static Map<String, String> toolPathMap = new LinkedHashMap<String, String>();

	private static Map<String, String> mergeToolPathMap = new LinkedHashMap<String, String>();

	private BooleanFieldEditor useExternal;

	private BooleanFieldEditor useExternalMerge;

	private ComboViewer comboViewer;

	private ComboViewer mergeComboViewer;

	private Text execPath;

	private Text mergeExecPath;

	private static final int SPAN = 1;

	public static final String TOOL_IBM = "ibm";

	public static final String TOOL_KDIFF = "kdiff3";
	
	public static final String TOOL_BEYONDCOMPARE = "beyond compare";

	/**
	 * Creates a new instance.
	 */
	public DiffMergePreferencePage() {
		setDescription(PreferenceMessages.getString("DiffMergePreferencePage.Description")); //$NON-NLS-1$
		// Set the preference store for the preference page.
		setPreferenceStore(new ClearCasePreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		useExternal = new BooleanFieldEditor(COMPARE_EXTERNAL, PreferenceMessages.getString("Preferences.General.CompareWithExternalTool"), //$NON-NLS-1$
				composite);

		// Handle change diff ext&int
		useExternal.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if ((Boolean) event.getNewValue() == true && selectedDiffTool.equals(TOOL_IBM)) {
					execPath.setEnabled(false);
				} else if ((Boolean) event.getNewValue() == false) {
					execPath.setEnabled(false);
				} else {
					execPath.setEnabled(true);
				}
			}
		});
		addFieldEditor(useExternal);
		
		// Diff Group
		Group groupDiff = new Group(composite, SWT.NULL);
		GridLayout layoutDiff = new GridLayout();
		layoutDiff.numColumns = 1;
		groupDiff.setLayout(layoutDiff);
		GridData dataDiff = new GridData();
		dataDiff.horizontalAlignment = GridData.FILL;
		groupDiff.setLayoutData(dataDiff);
		groupDiff.setText("External Diff tool settings:");
		comboViewer = new ComboViewer(groupDiff);
		Combo combo = comboViewer.getCombo();
		combo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		comboViewer.setContentProvider(new IStructuredContentProvider() {
			String[] vals;

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				vals = (String[]) newInput;
			}

			public Object[] getElements(Object inputElement) {
				return vals;
			}
		});
		// This is called when I select the page.
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent evt) {
				ISelection selection = evt.getSelection();
				if (selection instanceof StructuredSelection) {
					StructuredSelection sel = (StructuredSelection) selection;
					if (!selection.isEmpty()) {
						selectedDiffTool = sel.getFirstElement().toString();
						if (selectedDiffTool.equals(TOOL_IBM)) {
							// Sine we already have cleartool path no need to
							// input.
							execPath.setEnabled(false);
						} else if (useExternal.getBooleanValue() == false) {
							// When we use no
							execPath.setEnabled(false);
						} else {
							execPath.setEnabled(true);
						}

					}

					// set matching execPath
					if (selectedDiffTool != null & execPath != null) {
						toolPathMap = PreferenceHelper.strToMap(getPreferenceStore().getString(IClearCasePreferenceConstants.EXTERNAL_DIFF_TOOL_EXEC_PATH));
						execPath.setText(PreferenceHelper.getExecPath(selectedDiffTool, toolPathMap));
					}
				}
			}
		});
		createLabel(groupDiff, PreferenceMessages.getString("DiffMergePreferencePage.External.Diff.Tool.ExecPath"), SPAN); //$NON-NLS-1$
		execPath = new Text(groupDiff, SWT.BORDER);
		execPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		useExternalMerge = new BooleanFieldEditor(MERGE_EXTERNAL, PreferenceMessages.getString("Preferences.General.MergeWithExternalTool"), //$NON-NLS-1$
				composite);
		// Handle change diff ext&int
		useExternalMerge.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if ((Boolean) event.getNewValue() == true && selectedMergeTool.equals(TOOL_IBM)) {
					mergeExecPath.setEnabled(false);
				} else if ((Boolean) event.getNewValue() == false) {
					mergeExecPath.setEnabled(false);
				} else {
					mergeExecPath.setEnabled(true);
				}
			}
		});
		addFieldEditor(useExternalMerge);

		// Merge Group
		Group mergeGroup = new Group(composite, SWT.NULL);
		GridLayout diffLayout = new GridLayout();
		diffLayout.numColumns = 1;
		mergeGroup.setLayout(diffLayout);
		GridData mergeData = new GridData();
		dataDiff.horizontalAlignment = GridData.FILL;
		mergeGroup.setLayoutData(mergeData);
		mergeGroup.setText("External Merge Tool Settings:");
		mergeComboViewer = new ComboViewer(mergeGroup);
		Combo mergeCombo = mergeComboViewer.getCombo();
		mergeCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		mergeComboViewer.setContentProvider(new IStructuredContentProvider() {
			String[] vals;

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				vals = (String[]) newInput;
			}

			public Object[] getElements(Object inputElement) {
				return vals;
			}
		});

		mergeComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent evt) {

				ISelection selection = evt.getSelection();
				if (selection instanceof StructuredSelection) {
					StructuredSelection sel = (StructuredSelection) selection;
					if (useExternalMerge.getBooleanValue() == false)
						return;// this overrides
					if (!selection.isEmpty()) {

						selectedMergeTool = sel.getFirstElement().toString();

						if (selectedMergeTool.equals(TOOL_IBM)) {
							// Sine we already have cleartool path no need to
							// input.
							mergeExecPath.setEnabled(false);
						} else if (useExternalMerge.getBooleanValue() == false) {
							// When we use no
							mergeExecPath.setEnabled(false);
						} else {
							mergeExecPath.setEnabled(true);
						}
					}

					// set matching execPath
					if (selectedMergeTool != null & mergeExecPath != null) {
						mergeToolPathMap = PreferenceHelper.strToMap(getPreferenceStore().getString(IClearCasePreferenceConstants.EXTERNAL_MERGE_TOOL_EXEC_PATH));
						mergeExecPath.setText(PreferenceHelper.getExecPath(selectedMergeTool, mergeToolPathMap));
					}

				}
			}
		});

		createLabel(mergeGroup, PreferenceMessages.getString("DiffMergePreferencePage.External.Merge.Tool.ExecPath"), SPAN); //$NON-NLS-1$
		mergeExecPath = new Text(mergeGroup, SWT.BORDER);
		mergeExecPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		initializeValues();
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// ignore
	}

	/**
	 * Initializes values for non-FieldEditors.
	 */
	private void initializeValues() {
		final IPreferenceStore store = getPreferenceStore();
		selectedDiffTool = store.getString(IClearCasePreferenceConstants.EXTERNAL_DIFF_TOOL);
		selectedMergeTool = store.getString(IClearCasePreferenceConstants.EXTERNAL_MERGE_TOOL);
		ArrayList<String> tools = new ArrayList<String>();
		tools.add(TOOL_IBM);
		tools.add(TOOL_KDIFF);
		tools.add(TOOL_BEYONDCOMPARE);
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
		Collections.sort(tools, collator);
		// Diff
		comboViewer.setInput(tools.toArray(new String[tools.size()]));
		comboViewer.reveal(selectedDiffTool);
		comboViewer.setSelection(new StructuredSelection(selectedDiffTool), true);

		if (useExternal.getBooleanValue() == false) {
			execPath.setEnabled(false);
		}

		// Merge
		mergeComboViewer.setInput(tools.toArray(new String[tools.size()]));
		mergeComboViewer.reveal(selectedMergeTool);
		mergeComboViewer.setSelection(new StructuredSelection(selectedMergeTool), true);

		if (useExternalMerge.getBooleanValue() == false) {
			mergeExecPath.setEnabled(false);
		}

	}

	// Needs to be done for each fieldeditor.
	private void addFieldEditor(FieldEditor fieldEditor) {
		fieldEditor.setPreferencePage(this);
		fieldEditor.setPreferenceStore(getPreferenceStore());
		fieldEditor.load();
	}

	/**
	 * creates a label
	 */
	private Label createLabel(Composite parent, String text, int span) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = span;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		/** Diff */
		useExternal.store();
		if (!selectedDiffTool.equals("")) {
			getPreferenceStore().setValue(IClearCasePreferenceConstants.EXTERNAL_DIFF_TOOL, selectedDiffTool);
			// check if
			if (isExecPathEmpty(execPath, selectedDiffTool)) {
				return false;
			}
			cache(execPath, selectedDiffTool, toolPathMap);
			// now store it.
			storePref(IClearCasePreferenceConstants.EXTERNAL_DIFF_TOOL_EXEC_PATH, PreferenceHelper.mapToStr(toolPathMap));

		}

		/** Merge */
		useExternalMerge.store();

		if (!selectedMergeTool.equals("")) {
			getPreferenceStore().setValue(IClearCasePreferenceConstants.EXTERNAL_MERGE_TOOL, selectedMergeTool);
			// check if
			if (isExecPathEmpty(mergeExecPath, selectedMergeTool)) {
				return false;
			}
		}

		cache(mergeExecPath, selectedMergeTool, mergeToolPathMap);
		// now store it.
		storePref(IClearCasePreferenceConstants.EXTERNAL_MERGE_TOOL_EXEC_PATH, PreferenceHelper.mapToStr(mergeToolPathMap));

		if (super.performOk()) {
			ClearCasePlugin.getDefault().resetClearCase();
			return true;
		}
		return false;
	}

	private void storePref(String name, String value) {
		getPreferenceStore().setValue(name, value);
	}

	/**
	 * Check if exec path is empty when a external tool is selected.
	 * 
	 * @param path
	 * @param selected
	 * @return true if empty
	 */
	private boolean isExecPathEmpty(Text path, String selected) {
		if (path.getText().equals("") && !selected.equals(TOOL_IBM) && ClearCasePreferences.isCompareExternal()) {
			MessageDialog.openError(getShell(), PreferenceMessages.getString("DiffMergePreferencePage.error.title"), PreferenceMessages.getString(("DiffMergePreferencePage.error.noPath"))); //$NON-NLS-1$ //$NON-NLS-2$
			path.setFocus();
			return true;
		}

		return false;

	}

	/**
	 * check if same values are already in cache map.
	 * 
	 * @param path
	 * @param selected
	 * @param map
	 */
	private void cache(Text path, String selected, Map<String, String> map) {
		// check if same values are already in map.
		if (map.containsKey(selected)) {
			String storedPath = map.get(selected);
			// we have the key check value.
			if (!storedPath.equals(path.getText())) {
				// value is not the same. Update it.
				map.put(selected, path.getText());
			}
		} else {
			// No key value at all. Add it!
			map.put(selected, path.getText());
		}
	}

}
