/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.gui.wizard;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.panel.ApplicationResources;
import org.syncany.gui.panel.SWTResourceManager;
import org.syncany.util.I18n;

/**
 * @author vwiencek
 *
 */
public class WizardDialog extends Dialog {
	private enum Panel {
		START, 
		CREATE_REPOSITORY, 
		CREATE_REPOSITORY_EMAIL, 
		CREATE_REPOSITORY_PLUGIN, 
		CREATE_SUMMARY, 
		CONNECT_REPOSITORY, 
		REPOSITORY_ENCRYPTION,
		CONNECT_REPOSITORY_EMAIL, 
		CONNECT_REPOSITORY_PLUGIN;

		String className;

		Panel() {
			className = null;
		}

		Panel(String className) {
			this.className = className;
		}

		/**
		 * @return the className
		 */
		public String getClassName() {
			return className;
		}
	};

	private Panel selectedPanel = Panel.START;
	private Map<Panel, WizardPanelComposite> panels = new HashMap<>();
	private Map<String, String> userInput = new HashMap<>();
	private Button cancelButton;
	private Button nextButton;
	private Button previousButton;
	private Object result;
	private Shell shell;
	private Composite stackComposite;
	private StackLayout stackLayout;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public WizardDialog(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		buildPanels();
		showPanel(Panel.START);

		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		Font fontNormal = ApplicationResources.FONT_NORMAL;
		Font fontBold = ApplicationResources.FONT_BOLD;
		
		shell = new Shell(getParent(), SWT.DIALOG_TRIM);
		shell.setToolTipText("");
		shell.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		shell.setSize(560, 350);
		shell.setText(getText());
		GridLayout gl_shell = new GridLayout(2, false);
		gl_shell.marginLeft = -2;
		gl_shell.marginHeight = 0;
		gl_shell.marginWidth = 0;
		gl_shell.horizontalSpacing = 0;
		gl_shell.verticalSpacing = 0;
		shell.setLayout(gl_shell);

		Label imageLabel = new Label(shell, SWT.NONE);
		imageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 2));
		imageLabel.setImage(SWTResourceManager.getImage("/images/wizard-left.png"));

		stackComposite = new Composite(shell, SWT.NONE);
		stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));

		Composite buttonComposite = new Composite(shell, SWT.NONE);
		RowLayout rl_buttonComposite = new RowLayout(SWT.HORIZONTAL);
		buttonComposite.setLayout(rl_buttonComposite);
		buttonComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 1));

		cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setLayoutData(new RowData(100, 30));
		cancelButton.setFont(fontNormal);
		cancelButton.setText(I18n.getString("DefaultWizardPanel.dialog.cancel"));
		cancelButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleCancel();
			}
		});

		previousButton = new Button(buttonComposite, SWT.NONE);
		previousButton.setLayoutData(new RowData(100, 30));
		previousButton.setFont(fontNormal);
		previousButton.setText(I18n.getString("DefaultWizardPanel.dialog.previous"));
		previousButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handlePrevious();
			}
		});

		nextButton = new Button(buttonComposite, SWT.NONE);
		nextButton.setLayoutData(new RowData(100, 30));
		nextButton.setFont(fontNormal);
		nextButton.setText(I18n.getString("DefaultWizardPanel.dialog.next"));
		nextButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleNext();
			}
		});
	}

	private void handleNext() {
		WizardPanelComposite panel;
		switch (selectedPanel) {
			case START:
				panel = panels.get(Panel.START);
				if (panel.isValid()) {
					userInput.putAll(panel.getUserSelection());
					showPanel(Panel.CREATE_REPOSITORY);
				}
				break;
			case CREATE_REPOSITORY:
				panel = panels.get(Panel.CREATE_REPOSITORY);
				if (panel.isValid()) {
					userInput.putAll(panel.getUserSelection());
					String repositoryType = panel.getUserSelection().get("repositoryType").toLowerCase();
	
					if (repositoryType.equals("email")){
						showPanel(Panel.CREATE_REPOSITORY_EMAIL);
					}
					else {
						RepositoryPluginPanel pluginPanel = (RepositoryPluginPanel) panels.get(Panel.CREATE_REPOSITORY_PLUGIN);
						switch (repositoryType){
							case "ftp":
								pluginPanel.setClassName("org.syncany.gui.plugin.FTPPluginPanel");
								break;
							case "amazon s3":
								pluginPanel.setClassName("org.syncany.gui.plugin.AmazonPluginPanel");
								break;
							case "webdav":
								pluginPanel.setClassName("org.syncany.gui.plugin.WebdavPluginPanel");
								break;
						}
						showPanel(Panel.CREATE_REPOSITORY_PLUGIN);
					}
				}
				break;
			case CREATE_REPOSITORY_EMAIL:
				panel = panels.get(Panel.CREATE_REPOSITORY_EMAIL);
				if (panel.isValid()) {
					userInput.putAll(panel.getUserSelection());
					showPanel(Panel.CREATE_SUMMARY);
				}
				break;
			case CREATE_REPOSITORY_PLUGIN:
				panel = panels.get(Panel.CREATE_REPOSITORY_PLUGIN);
				if (panel.isValid()) {
					userInput.putAll(panel.getUserSelection());
					showPanel(Panel.REPOSITORY_ENCRYPTION);
				}
		}
	}

	private void handlePrevious() {
		switch (selectedPanel) {
			case CREATE_REPOSITORY:
				showPanel(Panel.START);
				break;
			case CREATE_REPOSITORY_EMAIL:
			case CREATE_REPOSITORY_PLUGIN:
				showPanel(Panel.CREATE_REPOSITORY);
				break;
			}
	}

	private void handleCancel() {
		shell.dispose();
	}

	private void showPanel(Panel panel) {
		selectedPanel = panel;
		stackLayout.topControl = panels.get(panel);
		toggleButtons(panel);
		stackComposite.layout();
	}

	private void toggleButtons(Panel panel) {
		if (panel.equals(Panel.START)){
			previousButton.setVisible(false);
			cancelButton.setVisible(true);
		}
		else{
			previousButton.setVisible(true);
			cancelButton.setVisible(false);
		}
	}

	private void buildPanels() {
		panels.put(Panel.START, new StartPanel(stackComposite, SWT.NONE));
		panels.put(Panel.CREATE_REPOSITORY, new CreateRepositorySelectionPanel(stackComposite, SWT.NONE));
		panels.put(Panel.CREATE_REPOSITORY_EMAIL, new RepositoryEmailPanel(stackComposite, SWT.NONE));
		panels.put(Panel.CREATE_REPOSITORY_PLUGIN, new RepositoryPluginPanel(stackComposite, SWT.NONE));
		panels.put(Panel.CREATE_SUMMARY, new CreateRepositorySummaryPanel(stackComposite, SWT.NONE));
		panels.put(Panel.CONNECT_REPOSITORY, new ConnectDialog(stackComposite, SWT.NONE));
		panels.put(Panel.REPOSITORY_ENCRYPTION, new RepositoryEncryptionPanel(stackComposite, SWT.NONE));
	}

	/**
	 * This method centers the dialog on the screen using
	 * <code>Display.getCurrent().getPrimaryManitor()</code>
	 */
	protected void centerOnScreen() {
		Monitor primary = Display.getCurrent().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);
	}
}
