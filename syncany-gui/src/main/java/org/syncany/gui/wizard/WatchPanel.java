package org.syncany.gui.wizard;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.SyncanyCommandParameters;
import org.syncany.gui.UserInput;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class WatchPanel extends WizardPanelComposite {
	private Text localDir;
	
	public WatchPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
	}
	
	public void initComposite(){
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		introductionTitleLabel.setText(I18n.getString("plugin.local.introduction.title"));
		introductionTitleLabel.setFont(fontBold);
		
		Label introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		introductionLabel.setText(I18n.getString("plugin.local.introduction"));
		introductionLabel.setFont(fontNormal);
		
		Label hostLabel = new Label(this, SWT.NONE);
		GridData gd_hostLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_hostLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		hostLabel.setLayoutData(gd_hostLabel);
		hostLabel.setText(I18n.getString("plugin.local.localFolder", true));
		hostLabel.setFont(fontNormal);
		
		localDir = new Text(this, SWT.BORDER);
		localDir.setFont(fontNormal);
		GridData gd_hostText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_hostText.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		gd_hostText.minimumWidth = 200;
		localDir.setLayoutData(gd_hostText);
		
		Button selectFolderButton = new Button(this, SWT.NONE);
		selectFolderButton.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1));
		selectFolderButton.setImage(SWTResourceManager.getResizedImage("/images/folder-icon.png", 16, 16));
		selectFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fd = new DirectoryDialog(getShell());
				String selectedFolder = fd.open();
				
				if (selectedFolder != null && selectedFolder.length() > 0)
					localDir.setText(selectedFolder);
			}
		});
		
		Composite buttonComposite = new Composite(this, SWT.NONE);
		GridLayout gl_buttonComposite = new GridLayout(2, false);
		gl_buttonComposite.horizontalSpacing = 0;
		gl_buttonComposite.verticalSpacing = 0;
		gl_buttonComposite.marginWidth = 0;
		gl_buttonComposite.marginHeight = 0;
		buttonComposite.setLayout(gl_buttonComposite);
		GridData gd_buttonComposite = new GridData(SWT.FILL, SWT.BOTTOM, true, false, 4, 1);
		gd_buttonComposite.minimumHeight = 30;
		buttonComposite.setLayoutData(gd_buttonComposite);
		
		final Label testResultLabel = new Label(buttonComposite, SWT.NONE);
		testResultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		testResultLabel.setAlignment(SWT.CENTER);
		
		final Button testLocalRepositoryButton = new Button(buttonComposite, SWT.NONE);

		GridData gd_testFtpButton = new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1);
		gd_testFtpButton.heightHint = ApplicationResourcesManager.DEFAULT_BUTTON_HEIGHT;
		gd_testFtpButton.widthHint = ApplicationResourcesManager.DEFAULT_BUTTON_WIDTH;
		testLocalRepositoryButton.setLayoutData(gd_testFtpButton);
		
		testLocalRepositoryButton.setFont(fontNormal);
		testLocalRepositoryButton.setText("Test");
		testLocalRepositoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String folder = localDir.getText();
				Display.getCurrent().syncExec(new Runnable() {
				    public void run() {
				    	if (folder == null || folder.length() == 0){
				    		testResultLabel.setText(I18n.getString("plugin.local.emptyDirectory"));
				    	}
				    	else if (!testLocalDirectory(folder)){
				    		testResultLabel.setText(I18n.getString("plugin.local.notValidDirectory"));
				    	}
				    	else{
				    		testResultLabel.setText(I18n.getString("plugin.local.validDirectory"));
				    	}
				    }
				});
			}
		});
	}	

	@Override
	public UserInput getUserSelection() {
		UserInput parameters = new UserInput();
		parameters.put(SyncanyCommandParameters.LOCAL_FOLDER, localDir.getText());
		return parameters;
	}
	
	@Override
	public boolean isValid() {
		return testLocalDirectory(localDir.getText());
	}
	
	private boolean testLocalDirectory(String folder){
		File localDirFile = new File(folder);		
		return localDirFile.exists() && localDirFile.isDirectory();
	}

	@Override
	public boolean hasNextButton() {
		return false;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return true;
	}

	@Override
	public void updateData() {
		
	}
}
