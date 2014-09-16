package net.sourceforge.eclipseccase.views;

import org.eclipse.jface.commands.ActionHandler;

import org.eclipse.ui.handlers.IHandlerService;

import net.sourceforge.eclipseccase.ClearCasePreferences;

import net.sourceforge.clearcase.ClearCase;
import net.sourceforge.clearcase.ClearCaseInterface;
import net.sourceforge.eclipseccase.ui.actions.SetConfigSpecAction;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

public class ConfigSpecView extends ViewPart {

	private String configSpecTxt = "";

	private Action refreshAction;

	private Action saveAction;

	private Action clearAction;

	private Text configSpec;

	private IResource resource = null;

	private boolean bConfigSpecModified;

	private boolean bConfigSpecRefreshing;

	private Group configSpecGroup;

	private Label configSpecLabel;

	// Use same id for action as for command definition in plugin.xml
	private static final String REFRESH_ACTION_ID = "net.sourceforge.eclipseccase.views.configspecview.refreshAction";

	private static final String SAVE_ACTION_ID = "net.sourceforge.eclipseccase.views.configspecview.saveAction";

	private static final String CLEAR_ACTION_ID = "net.sourceforge.eclipseccase.views.configspecview.clearAction";

	@Override
	public void createPartControl(Composite parent) {

		bConfigSpecModified = false;
		bConfigSpecRefreshing = false;

		configSpecGroup = new Group(parent, SWT.BORDER);
		configSpecGroup.setLayout(new GridLayout(1, false));

		configSpecLabel = new Label(configSpecGroup, SWT.LEFT);
		configSpecLabel.setText("Base: no");

		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		configSpecLabel.setLayoutData(data);

		configSpec = new Text(configSpecGroup, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

		configSpec.setEditable(false);

		data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		configSpec.setLayoutData(data);

		configSpec.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!bConfigSpecRefreshing) {
					if (!bConfigSpecModified) {
						bConfigSpecModified = true;
						focusOnConfigSpec();
					}
				}
			}
		});

		// Refresh Button
		refreshAction = new Action() {
			@Override
			public void run() {
				refresh();
			}
		};

		saveAction = new Action() {

			@Override
			public void run() {
				save();
			}
		};
		clearAction = new Action() {
			@Override
			public void run() {
				clear();
			}
		};

		// Register action with keys.
		IHandlerService service = (IHandlerService) getViewSite().getService(IHandlerService.class);

		refreshAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("net.sourceforge.eclipseccase.ui", "icons/full/refresh.gif"));
		refreshAction.setToolTipText("Refresh");
		refreshAction.setActionDefinitionId(REFRESH_ACTION_ID);
		service.activateHandler(refreshAction.getActionDefinitionId(), new ActionHandler(refreshAction));

		saveAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("net.sourceforge.eclipseccase.ui", "icons/full/save.gif"));
		saveAction.setToolTipText("Save");
		saveAction.setActionDefinitionId(SAVE_ACTION_ID);
		service.activateHandler(saveAction.getActionDefinitionId(), new ActionHandler(saveAction));

		clearAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("net.sourceforge.eclipseccase.ui", "icons/full/clear.gif"));
		clearAction.setToolTipText("Clear");
		clearAction.setActionDefinitionId(CLEAR_ACTION_ID);
		service.activateHandler(clearAction.getActionDefinitionId(), new ActionHandler(clearAction));

		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme currentTheme = themeManager.getCurrentTheme();

		FontRegistry fontRegistry = currentTheme.getFontRegistry();
		Font font = fontRegistry.get(JFaceResources.TEXT_FONT);

		configSpec.setFont(font);

		focusOnConfigSpec();
	}

	@Override
	public void setFocus() {
	}

	public void loadConfigSpec(IResource resource) {
		if (resource != null) {
			this.resource = null;
			focusOnConfigSpec();
			this.resource = resource;
			refresh();
		}
	}

	public void focusOnConfigSpec() {
		configSpec.getDisplay().asyncExec(new Runnable() {
			public void run() {
				getViewSite().getActionBars().getToolBarManager().removeAll();
				getViewSite().getActionBars().getToolBarManager().add(saveAction);
				getViewSite().getActionBars().getToolBarManager().add(clearAction);
				getViewSite().getActionBars().getToolBarManager().add(refreshAction);
				getViewSite().getActionBars().getToolBarManager().update(true);

				if (resource == null) {
					configSpecLabel.setText("Base: no");

					configSpec.setEditable(false);
					saveAction.setEnabled(false);
					clearAction.setEnabled(false);
					refreshAction.setEnabled(false);
					configSpec.setText("");
				} else {

					if (!(ClearCasePreferences.isConfigSpecModificationForbidden())) {
						configSpecLabel.setText("Base: " + resource.getLocation().toString());

						configSpec.setEditable(true);
						if (bConfigSpecModified) {
							saveAction.setEnabled(true);
						} else {
							saveAction.setEnabled(false);
						}
					} else {
						configSpecLabel.setText("Base: " + resource.getLocation().toString() + " - Config Spec cannot be modified");
						configSpec.setEditable(false);
						saveAction.setEnabled(false);
					}

					clearAction.setEnabled(true);
					refreshAction.setEnabled(true);
				}
			}
		});
	}

	public void refresh() {
		bConfigSpecRefreshing = true;
		if (resource != null) {
			ClearCaseInterface cci = ClearCase.createInterface(ClearCase.INTERFACE_CLI);
			String viewName = cci.getViewName(resource.getLocation().toOSString());
			if (viewName.length() > 0) {
				configSpecTxt = cci.getViewConfigSpec(viewName);
				configSpec.getDisplay().asyncExec(new Runnable() {
					public void run() {
						configSpec.setText(configSpecTxt);
						bConfigSpecRefreshing = false;
						bConfigSpecModified = false;
					}
				});
			}
		}

		focusOnConfigSpec();
	}

	public void save() {
		configSpec.setEditable(false);
		getViewSite().getActionBars().getToolBarManager().removeAll();
		getViewSite().getActionBars().getToolBarManager().update(true);
		configSpecTxt = configSpec.getText();

		SetConfigSpecAction saveAction = new SetConfigSpecAction();
		saveAction.setResource(this.resource);
		saveAction.setConfigSpecTxt(configSpecTxt);
		saveAction.setConfigSpecView(this);

		try {
			saveAction.execute((IAction) null);
			bConfigSpecModified = false;
		} catch (Exception e) {
			// Already handled in SaveAction.
		} finally {
		}
	}

	public void clear() {
		resource = null;
		configSpec.setEditable(false);
		configSpec.setText("");
		focusOnConfigSpec();
	}

}
