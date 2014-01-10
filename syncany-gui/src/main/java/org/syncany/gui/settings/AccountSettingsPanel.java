package org.syncany.gui.settings;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.config.ApplicationConfiguration;
import org.syncany.util.I18n;


/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class AccountSettingsPanel extends Composite {
	
	private String[] proxyAuthValues = new String[] {"HTTP", "Socket 4", "Socket 5"};
	private Label lblNewLabel;
	private Label lblNewLabel_1;
	private Table table;
	private Composite composite;
	private Button btnNewButton;
	
	/**
	 * @param parent
	 * @param style
	 */
	public AccountSettingsPanel(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	private void initComposite() {
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayout(new GridLayout(1, false));
		
		lblNewLabel_1 = new Label(this, SWT.NONE);
		lblNewLabel_1.setText(I18n.getString("dialog.settings.proxy.title", true));
		lblNewLabel_1.setFont(fontBold);
		
		lblNewLabel = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("New Label");
		
		table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		composite.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		btnNewButton = new Button(composite, SWT.NONE);
		btnNewButton.setText("Delete Profile");
	}

	
	public void setApplicationParameters(ApplicationConfiguration configuration){

	}
}
