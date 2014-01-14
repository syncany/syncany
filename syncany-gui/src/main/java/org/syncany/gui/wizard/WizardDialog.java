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

import static org.syncany.gui.ApplicationResourcesManager.DEFAULT_BUTTON_HEIGHT;
import static org.syncany.gui.ApplicationResourcesManager.DEFAULT_BUTTON_WIDTH;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.Launcher;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.SyncanyCommandParameters;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.WidgetDecorator.FontDecorator;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.messaging.InterfaceUpdate;
import org.syncany.gui.messaging.InterfaceUpdate.InterfaceUpdateAction;
import org.syncany.gui.util.DialogUtil;
import org.syncany.util.I18n;

import com.google.common.eventbus.Subscribe;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class WizardDialog extends Dialog {
	private enum Panel {
		START(null), 
		REPOSITORY_SELECTION(START), 
		REPOSITORY_ENCRYPTION(REPOSITORY_SELECTION),
		LOCAL_FOLDER_SELECTION(REPOSITORY_ENCRYPTION),
		WATCH(START),
		SUMMARY(LOCAL_FOLDER_SELECTION); 
		
		Panel(Panel previous){
			this.previous = previous;
		}
		
		private Panel previous;
		
		public Panel getPrevious() {
			return previous;
		}
	};

	//Widgets
	private Button cancelButton;
	private Button nextButton;
	private Button previousButton;
	private Button finishButton;
	
	private Shell shell;
	private Composite stackComposite;
	private StackLayout stackLayout;

	private Panel selectedPanel = Panel.START;
	private Map<Panel, WizardPanelComposite> panels = new HashMap<>();
	private UserInput userInput = new UserInput();
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public WizardDialog(Shell parent, int style) {
		super(parent, style);
		setText(I18n.getString("dialog.wizard.title"));
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		buildPanels();
		showPanel(Panel.START);

		DialogUtil.centerOnScreen(shell);
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return null;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.DIALOG_TRIM);
		shell.setToolTipText("");
		shell.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		shell.setSize(700, 500);
		shell.setText(getText());
		GridLayout gl_shell = new GridLayout(2, false);
		gl_shell.marginLeft = -2;
		gl_shell.marginHeight = 0;
		gl_shell.marginWidth = 0;
		gl_shell.horizontalSpacing = 0;
		gl_shell.verticalSpacing = 0;
		shell.setLayout(gl_shell);

		Label imageLabel = new Label(shell, SWT.NONE);
		imageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 2));
		imageLabel.setImage(SWTResourceManager.getImage("/images/wizard-left.png"));

		stackComposite = new Composite(shell, SWT.NONE);
		stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);
		stackComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true, 1, 1));

		Composite buttonComposite = new Composite(shell, SWT.NONE);
		RowLayout rl_buttonComposite = new RowLayout(SWT.HORIZONTAL);
		rl_buttonComposite.marginBottom = 15;
		rl_buttonComposite.marginRight = 20;
		buttonComposite.setLayout(rl_buttonComposite);
		buttonComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 1));

		cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setLayoutData(new RowData(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
		cancelButton.setText(I18n.getString("dialog.default.cancel"));
		cancelButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleCancel();
			}
		});

		previousButton = new Button(buttonComposite, SWT.NONE);
		previousButton.setLayoutData(new RowData(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
		previousButton.setText(I18n.getString("dialog.default.previous"));
		previousButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handlePrevious();
			}
		});

		nextButton = new Button(buttonComposite, SWT.NONE);
		nextButton.setLayoutData(new RowData(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
		nextButton.setText(I18n.getString("dialog.default.next"));
		nextButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleNext();
			}
		});
		
		finishButton = new Button(buttonComposite, SWT.NONE);
		finishButton.setLayoutData(new RowData(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
		finishButton.setText(I18n.getString("dialog.default.finish"));
		finishButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleFinish();
			}
		});
		
		WidgetDecorator.setFont(cancelButton, FontDecorator.NORMAL);
		WidgetDecorator.setFont(previousButton, FontDecorator.NORMAL);
		WidgetDecorator.setFont(nextButton, FontDecorator.NORMAL);
		WidgetDecorator.setFont(finishButton, FontDecorator.NORMAL);
	}
	
	private void handleFinish(){
		switch (selectedPanel) {
			case WATCH:
				WatchPanel wp = (WatchPanel)panels.get(selectedPanel);
				String f = wp.getUserSelection().get(SyncanyCommandParameters.LOCAL_FOLDER);
				Launcher.applicationConfiguration.addWatchedFolder(f);
				Launcher.saveConfiguration();
				ClientCommandFactory.handleWatch(f);
				shell.dispose();
				break;
			case SUMMARY:
				ClientCommandFactory.handleCommand(userInput);
				SummaryPanel sp = (SummaryPanel)panels.get(selectedPanel);
				sp.startIndeterminateProgressBar();
				break;
			default:	
				throw new RuntimeException("Invalid user selection: " + selectedPanel);
		}
	}
	
	@Subscribe
	public void update(InterfaceUpdate event){
		if (event.getAction() == InterfaceUpdateAction.WIZARD_COMMAND_DONE){
			final SummaryPanel summaryPanel = (SummaryPanel)panels.get(Panel.SUMMARY);
			summaryPanel.stopIndeterminateProgressBar();
			
			String reply = (String) event.getData().get("result");
			final String shareLink = (String) event.getData().get("share_link");
			final boolean shareLinkEncrypted = Boolean.parseBoolean((String) event.getData().get("share_link_encrypted"));
			
			switch (reply){
				case "failed":
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							finishButton.setText("Retry ...");
							summaryPanel.showErrorMessage();
						}
					});
					
					break;
				case "succeed":
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							toggleButtons(false);
							summaryPanel.showSuccessMessage(shareLink, shareLinkEncrypted);
						}
					});
					
					break;
			}
		}
	}
	
	private void safeDispose() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				shell.dispose();
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
					
					if (userInput.get(SyncanyCommandParameters.COMMAND_ACTION).equals("watch")){
						showPanel(Panel.WATCH);
					}
					else{
						showPanel(Panel.REPOSITORY_SELECTION);
					}
				}
				break;
		
			case REPOSITORY_SELECTION:
				panel = panels.get(Panel.REPOSITORY_SELECTION);
				if (panel.isValid()) {
					userInput.putAll(panel.getUserSelection());
					showPanel(Panel.REPOSITORY_ENCRYPTION);
				}
				break;
			case REPOSITORY_ENCRYPTION:
				panel = panels.get(Panel.REPOSITORY_ENCRYPTION);
				if (panel.isValid()){
					userInput.putAll(panel.getUserSelection());
					showPanel(Panel.LOCAL_FOLDER_SELECTION);
				}
				break;
			case LOCAL_FOLDER_SELECTION:
				panel = panels.get(Panel.LOCAL_FOLDER_SELECTION);
				if (panel.isValid()){
					userInput.putAll(panel.getUserSelection());
					showPanel(Panel.SUMMARY);
				}
				break;
			default:
				throw new RuntimeException("Invalid user selection: "+selectedPanel);
		}
	}

	private void handlePrevious() {
		showPanel(selectedPanel.getPrevious());
	}

	public UserInput getUserInput() {
		return userInput;
	}
	
	private void handleCancel() {
		safeDispose();
	}

	private void showPanel(Panel panel) {
		selectedPanel = panel;
		WizardPanelComposite wizardPanel = panels.get(panel);
		stackLayout.topControl = wizardPanel;
		wizardPanel.updateData();
		toggleButtons(wizardPanel);
		stackComposite.layout();
	}

	private void toggleButtons(boolean state){
		nextButton.setEnabled(state);
		previousButton.setEnabled(state);
		cancelButton.setEnabled(state);
		finishButton.setEnabled(state);
	}
	
	private void toggleButtons(WizardPanelComposite panel) {
		nextButton.setEnabled(panel.hasNextButton());
		previousButton.setEnabled(panel.hasPreviousButton());
		cancelButton.setEnabled(panel.hasCancelButton());
		finishButton.setEnabled(panel.hasFinishButton());
	}

	private void buildPanels() {
		panels.put(Panel.START, new StartPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.REPOSITORY_SELECTION, new RepositorySelectionPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.REPOSITORY_ENCRYPTION, new RepositoryEncryptionPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.SUMMARY, new SummaryPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.WATCH, new WatchPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.LOCAL_FOLDER_SELECTION, new SelectLocalFolder(this, stackComposite, SWT.NONE));
	}
}
