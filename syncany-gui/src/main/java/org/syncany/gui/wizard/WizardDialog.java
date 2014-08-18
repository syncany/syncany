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
import java.util.UUID;

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
import org.syncany.daemon.websocket.messages.DaemonResultInitMessage;
import org.syncany.gui.CommonParameters;
import org.syncany.gui.Launcher;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.config.Profile;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.util.DialogUtil;
import org.syncany.util.I18n;

import com.google.common.eventbus.Subscribe;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class WizardDialog extends Dialog {
	private enum Panel {
		START(null, "LOCAL_FOLDER_SELECTION"), 
		REPOSITORY_SELECTION("LOCAL_FOLDER_SELECTION", "REPOSITORY_ENCRYPTION"), 
		REPOSITORY_ENCRYPTION("REPOSITORY_SELECTION", "SUMMARY"),
		LOCAL_FOLDER_SELECTION("START", "REPOSITORY_SELECTION"),
		SUMMARY("REPOSITORY_ENCRYPTION", null); 
		
		Panel(String previous, String next){
			this.previous = previous;
			this.next = next;
		}
		
		private String previous;
		private String next;
		
		public Panel getPrevious() {
			return Panel.valueOf(previous);
		}
		public Panel getNext() {
			return Panel.valueOf(next);
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
		shell.setBackground(WidgetDecorator.COLOR_WIDGET);
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
		cancelButton.setLayoutData(new RowData(WidgetDecorator.DEFAULT_BUTTON_WIDTH, WidgetDecorator.DEFAULT_BUTTON_HEIGHT));
		cancelButton.setText(I18n.getString("dialog.default.cancel"));
		cancelButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleCancel();
			}
		});

		previousButton = new Button(buttonComposite, SWT.NONE);
		previousButton.setLayoutData(new RowData(WidgetDecorator.DEFAULT_BUTTON_WIDTH, WidgetDecorator.DEFAULT_BUTTON_HEIGHT));
		previousButton.setText(I18n.getString("dialog.default.previous"));
		previousButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handlePrevious();
			}
		});

		nextButton = new Button(buttonComposite, SWT.NONE);
		nextButton.setLayoutData(new RowData(WidgetDecorator.DEFAULT_BUTTON_WIDTH, WidgetDecorator.DEFAULT_BUTTON_HEIGHT));
		nextButton.setText(I18n.getString("dialog.default.next"));
		nextButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleNext();
			}
		});
		
		finishButton = new Button(buttonComposite, SWT.NONE);
		finishButton.setLayoutData(new RowData(WidgetDecorator.DEFAULT_BUTTON_WIDTH, WidgetDecorator.DEFAULT_BUTTON_HEIGHT));
		finishButton.setText(I18n.getString("dialog.default.finish"));
		finishButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				handleFinish();
			}
		});
		
		WidgetDecorator.normal(nextButton, previousButton, cancelButton, finishButton);
	}
	
	private String commandId;
	
	@Subscribe
	public void update(DaemonResultInitMessage event){
		String id = event.getCommandId();
		if (commandId.equals(id)){
			final SummaryPanel summaryPanel = (SummaryPanel)panels.get(Panel.SUMMARY);
			summaryPanel.stopIndeterminateProgressBar();
			
			boolean result = event.isSuccess();
			final String shareLink = event.getShareLink();
			final String folder = event.getLocalFolder();
			final boolean shareLinkEncrypted = event.isShareLinkEncrypted();
			
			if(result) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						toggleButtons(false);
						summaryPanel.showSuccessMessage(shareLink, shareLinkEncrypted);

						ClientCommandFactory.handleWatch(folder, 120000, true);
						Launcher.updateProfiles(Profile.getDefault(folder));
					}
				});
			}
			else {
				
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						finishButton.setText("Retry ...");
						summaryPanel.showErrorMessage();
					}
				});
			}					
		}
	}
	
	private void handleFinish(){
		switch (selectedPanel) {
			case LOCAL_FOLDER_SELECTION:
				SelectLocalFolder wp = (SelectLocalFolder)panels.get(selectedPanel);
				String f = wp.getUserSelection().getCommonParameter(CommonParameters.LOCAL_FOLDER);
				
				ClientCommandFactory.handleWatch(f, 120000, true);
				Launcher.updateProfiles(Profile.getDefault(f));
				shell.dispose();
				break;
				
			case SUMMARY:
				updateFinishButton(false);
				this.commandId = UUID.randomUUID().toString();
				userInput.putCommonParameter(CommonParameters.COMMAND_ID, this.commandId);
				ClientCommandFactory.handleCommand(userInput);
				SummaryPanel sp = (SummaryPanel)panels.get(selectedPanel);
				sp.startIndeterminateProgressBar();
				break;
				
			default:	
				throw new RuntimeException("Invalid user selection: " + selectedPanel);
		}
	}
	
	private void handleNext() {
		WizardPanelComposite panel = panels.get(selectedPanel);
		if (panel.isValid()) {
			userInput.merge(panel.getUserSelection());
			showPanel(selectedPanel.getNext());
		}
	}

	private void handlePrevious() {
		showPanel(selectedPanel.getPrevious());
	}

	private void handleCancel() {
		safeDispose();
	}

	public UserInput getUserInput() {
		return userInput;
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
	
	public void updateFinishButton(final boolean state){
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				finishButton.setEnabled(state);		
			}
		});
	}
	public void updateNextButton(final boolean state){
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				nextButton.setEnabled(state);
			}
		});
	}
	
	public void safeDispose() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				shell.dispose();
			}
		});
	}
	
	private void buildPanels() {
		panels.put(Panel.START, new StartPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.REPOSITORY_SELECTION, new RepositorySelectionPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.REPOSITORY_ENCRYPTION, new RepositoryEncryptionPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.SUMMARY, new SummaryPanel(this, stackComposite, SWT.NONE));
		panels.put(Panel.LOCAL_FOLDER_SELECTION, new SelectLocalFolder(this, stackComposite, SWT.NONE));
	}
}
