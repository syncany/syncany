package org.syncany.gui.wizard;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.CommonParameters;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.util.FileUtil;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class SelectLocalFolder extends WizardPanelComposite {
	private Text localDir;
	private Label introductionLabel;
	private Label messageLabel;
	
	public SelectLocalFolder(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
	}
	
	public void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		introductionTitleLabel.setText(I18n.getString("dialog.selectLocalFolder.introduction.title"));
		
		introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		introductionLabel.setText(I18n.getString("dialog.selectLocalFolder.introduction"));
		
		Label hostLabel = new Label(this, SWT.WRAP);
		GridData gd_hostLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_hostLabel.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		hostLabel.setLayoutData(gd_hostLabel);
		hostLabel.setText(I18n.getString("dialog.selectLocalFolder.selectLocalFolder", true));
		
		localDir = new Text(this, SWT.BORDER);
		GridData gd_hostText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_hostText.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		gd_hostText.minimumWidth = 200;
		localDir.setLayoutData(gd_hostText);
		
		Button selectFolderButton = new Button(this, SWT.FLAT);
		selectFolderButton.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1));
		selectFolderButton.setImage(SWTResourceManager.getResizedImage("/images/folder-icon.png", 16, 16));
		selectFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fd = new DirectoryDialog(getShell());
				String selectedFolder = fd.open();
				
				if (selectedFolder != null && selectedFolder.length() > 0){
					localDir.setText(selectedFolder);
				}
			}
		});
		
		messageLabel = new Label(this, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		
		WidgetDecorator.bold(introductionTitleLabel);
		WidgetDecorator.normal(introductionLabel, localDir, hostLabel, messageLabel);
	}	

	@Override
	public UserInput getUserSelection() {
		UserInput parameters = new UserInput();
		parameters.putCommonParameter(CommonParameters.LOCAL_FOLDER, localDir.getText());
		return parameters;
	}
	
	@Override
	public boolean isValid() {
		String action = getParentWizardDialog().getUserInput().getCommonParameter(CommonParameters.COMMAND_ACTION);
		String folder = localDir.getText();
		
		switch (action){
			case "watch":
				//test if .syncany folder exists
				if (FileUtil.isRepositoryFolder(folder)) {
					return true;
				}
				else {
					WidgetDecorator.markAs(false, localDir);
					return false;
				}
			
			case "create":
				//test if folder exists && is empty
				
				if (FileUtil.isExistingFolder(folder)) {
					if (FileUtil.isEmptyFolder(folder)) {
						WidgetDecorator.markAs(true, localDir);
						return true;
					}
					else {
						MessageBox dialog = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.CANCEL);
						dialog.setText("Warning");
						dialog.setMessage("Folder is not empty, please choose an empty folder");
						dialog.open();
						WidgetDecorator.markAs(false, localDir);
						return false;
					}
				}
				else {
					boolean create = askCreateFolder(getShell(), folder);
					WidgetDecorator.markAs(create, localDir);
					return create;
				}
				
			case "connect":
				// test if folder exists only : folder might be full of files
				if (FileUtil.isExistingFolder(folder)) {
					WidgetDecorator.markAs(false, localDir);
					return false;
				}
				else {
					boolean create = askCreateFolder(getShell(), folder);
					WidgetDecorator.markAs(create, localDir);
					return create;
				}
			default:
				return false;
		}
	}
	
	private static boolean askCreateFolder(Shell shell, String f) {
		File localDirFile = new File(f);
		MessageBox dialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
		dialog.setText("Create Folder");
		dialog.setMessage(String.format("Would you like to create the folder [%s]?", localDirFile.getAbsolutePath()));

		int ret = dialog.open();

		if (ret == SWT.OK) {
			return true;
		}
		return false;
	}

	@Override
	public boolean hasNextButton() {
		return true;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return false;
	}

	@Override
	public void updateData() {
		String action = getParentWizardDialog().getUserInput().getCommonParameter(CommonParameters.COMMAND_ACTION);
		if (action.equals("watch")){
			getParentWizardDialog().updateFinishButton(true);
			getParentWizardDialog().updateNextButton(false);
		}
		else{
			getParentWizardDialog().updateFinishButton(false);
			getParentWizardDialog().updateNextButton(true);
		}

		introductionLabel.setText(I18n.getString(String.format("dialog.selectLocalFolder.%sIntroduction", action)));
	}
}
