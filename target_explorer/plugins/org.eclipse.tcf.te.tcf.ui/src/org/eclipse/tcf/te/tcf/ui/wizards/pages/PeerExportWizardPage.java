/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.wizards.pages;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IExportPersistenceService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.progress.UIJob;

/**
 * PeerExportWizardPage
 */
public class PeerExportWizardPage extends WizardPage {

	private static final int SIZING_TEXT_FIELD_WIDTH = 250;
	private static final String OVERWRITE = "overwrite"; //$NON-NLS-1$
	private static final String OLD_PATH = "oldpath"; //$NON-NLS-1$

	CheckboxTableViewer fViewer;
	Text fDestinationField;
	private Button fDestinationButton;
	private Button fOverwrite;

	private IStructuredSelection fSelection;

	/**
	 * Constructor
	 */
	public PeerExportWizardPage(IStructuredSelection selection) {
		super(Messages.PeerExportWizard_title);
		setTitle(Messages.PeerExportWizard_title);
		setMessage(Messages.PeerExportWizard_message);
		fSelection = selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
						| GridData.HORIZONTAL_ALIGN_FILL));
		composite.setFont(parent.getFont());

		createPeersGroup(composite);
		createDestinationGroup(composite);

		List<IPeerNode> elements = new ArrayList<IPeerNode>();
		Iterator<Object> it = fSelection.iterator();
		while (it.hasNext()) {
			Object element = it.next();
			IPeerNode peerNode = (IPeerNode)Platform.getAdapterManager().getAdapter(element, IPeerNode.class);
			if (peerNode != null) {
				elements.add((IPeerNode)element);
			}
		}
		fViewer.setCheckedElements(elements.toArray());

		setPageComplete(isComplete());
		setErrorMessage(null);

		setControl(composite);
	}

	/**
	 * Creates the checkbox tree and list for selecting peers.
	 *
	 * @param parent the parent control
	 */
	private final void createPeersGroup(Composite parent) {
		Composite resourcesGroup = new Composite(parent, SWT.NONE);
		resourcesGroup.setLayout(new GridLayout());
		resourcesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		resourcesGroup.setFont(parent.getFont());

		new Label(resourcesGroup, SWT.NONE).setText(Messages.PeerExportWizardPage_peers_label);
		Table table= new Table(resourcesGroup, SWT.CHECK | SWT.BORDER);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		fViewer= new CheckboxTableViewer(table);
		fViewer.setContentProvider(new ITreePathContentProvider() {
			@Override
			public void dispose() {
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof IPeerModel) {
					IPeerNode[] nodes = ((IPeerModel)inputElement).getPeerNodes();
					List<IPeerNode> filteredNodes = new ArrayList<IPeerNode>();
					for (IPeerNode node : nodes) {
	                    if (node.isVisible()) {
	                    	filteredNodes.add(node);
	                    }
                    }
					return filteredNodes.toArray();
				}
				return new Object[0];
			}
			@Override
			public Object[] getChildren(TreePath parentPath) {
				return null;
			}
			@Override
			public boolean hasChildren(TreePath path) {
				return false;
			}
			@Override
			public TreePath[] getParents(Object element) {
				return null;
			}
		});
		fViewer.setLabelProvider(new DelegatingLabelProvider());
		ICheckStateListener checkListener = new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(isComplete());
			}
		};
		fViewer.addCheckStateListener(checkListener);
		fViewer.setInput(ModelManager.getPeerModel());

		// top level group
		Composite buttonComposite = new Composite(resourcesGroup, SWT.NONE);
		buttonComposite.setFont(parent.getFont());

		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight= layout.marginWidth= 0;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL	| GridData.HORIZONTAL_ALIGN_FILL));

		Button selectButton = createButton(buttonComposite,
						IDialogConstants.SELECT_ALL_ID, Messages.PeerExportWizardPage_selectAll, false);

		selectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fViewer.setAllChecked(true);
				setPageComplete(isComplete());
			}
		});

		Button deselectButton = createButton(buttonComposite,
						IDialogConstants.DESELECT_ALL_ID, Messages.PeerExportWizardPage_deselectAll, false);

		deselectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fViewer.setAllChecked(false);
				setPageComplete(isComplete());
			}
		});
	}

	private Button createButton(Composite parent, int id, String label,
					boolean defaultButton) {
		Button button = new Button(parent, SWT.PUSH);

		GridData buttonData = new GridData(GridData.FILL_HORIZONTAL);
		button.setLayoutData(buttonData);

		button.setData(Integer.valueOf(id));
		button.setText(label);
		button.setFont(parent.getFont());

		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
			button.setFocus();
		}
		button.setFont(parent.getFont());
		setButtonLayoutData(button);
		return button;
	}

	private void createDestinationGroup(Composite parent) {
		Font font = parent.getFont();
		// destination specification group
		Composite destinationSelectionGroup = new Composite(parent, SWT.NONE);
		destinationSelectionGroup.setLayout(new GridLayout(3, false));
		destinationSelectionGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		destinationSelectionGroup.setFont(font);

		Label destinationLabel = new Label(destinationSelectionGroup, SWT.NONE);
		destinationLabel.setText(Messages.PeerExportWizardPage_destination_label);
		destinationLabel.setFont(font);

		// destination name entry field
		fDestinationField = new Text(destinationSelectionGroup, SWT.BORDER);
		fDestinationField.setFont(font);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
		fDestinationField.setLayoutData(gd);
		if (getDialogSettings().get(OLD_PATH) != null) {
			fDestinationField.setText(getDialogSettings().get(OLD_PATH));
		}
		fDestinationField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(isComplete());
			}
		});

		fDestinationButton = createButton(destinationSelectionGroup, IDialogConstants.SELECT_ALL_ID, Messages.PeerExportWizardPage_destination_button, false);
		fDestinationButton.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getContainer().getShell());
				dd.setText(Messages.PeerExportWizardPage_destination_label);
				String file = dd.open();
				if(file != null) {
					IPath path = new Path(file);
					fDestinationField.setText(path.toOSString());
					setPageComplete(isComplete());
				}
			}
		});

		fOverwrite = new Button(destinationSelectionGroup, SWT.CHECK);
		fOverwrite.setText(Messages.PeerExportWizardPage_overwrite_button);
		gd = new GridData();
		gd.horizontalSpan = 3;
		fOverwrite.setLayoutData(gd);
		fOverwrite.setSelection(getDialogSettings().getBoolean(OVERWRITE));
	}

	/**
	 * Returns if the page is complete
	 * @return true if the page is complete and can be 'finished', false otherwise
	 */
	protected boolean isComplete() {
		Object[] elements = fViewer.getCheckedElements();
		boolean selected = false;
		for (Object element : elements) {
			if(element instanceof IPeerNode) {
				selected = true;
				break;
			}
		}
		if(elements.length < 1 || !selected) {
			setErrorMessage(Messages.PeerExportWizardPage_peersMissing_error);
			return false;
		}
		String path = fDestinationField.getText().trim();
		if(path.length() == 0) {
			setErrorMessage(Messages.PeerExportWizardPage_destinationMissing_error);
			return false;
		}
		File dir = new File(path);
		if (dir.isFile()) {
			setErrorMessage(Messages.PeerExportWizardPage_destinationIsFile_error);
			return false;
		}
		if (!dir.isAbsolute()) {
			setErrorMessage(Messages.PeerExportWizardPage_destinationMissing_error);
			return false;
		}
		setErrorMessage(null);
		setMessage(Messages.PeerExportWizard_message);
		return true;
	}

	public boolean finish() {
		final Object[] configs = fViewer.getCheckedElements();
		final boolean overwrite = fOverwrite.getSelection();
		final String path = fDestinationField.getText().trim();
		IDialogSettings settings = getDialogSettings();
		settings.put(OVERWRITE, overwrite);
		settings.put(OLD_PATH, path);
		UIJob exportjob = new UIJob(getContainer().getShell().getDisplay(), Messages.PeerExportWizard_title) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if(monitor == null) {
					monitor = new NullProgressMonitor();
				}
				IPath destpath = new Path(path);
				File destfolder = destpath.toFile();
				boolean exist = destfolder.exists();
				if(!exist) {
					exist = destfolder.mkdirs();
				}
				if (exist) {
					monitor.beginTask(Messages.PeerExportWizard_title, configs.length);
					boolean toggleState = false;
					int toggleResult = -1;
					for (Object config : configs) {
						IURIPersistenceService service = ServiceManager.getInstance().getService(config, IExportPersistenceService.class);
						if (service == null) {
							service = ServiceManager.getInstance().getService(config, IURIPersistenceService.class);
						}
						if (service != null) {
							try {
								URI uri = service.getURI(config);
								File defaultFile = new File(uri.normalize());
								defaultFile = new Path(defaultFile.toString()).removeFileExtension().toFile();
								File file = destpath.append(defaultFile.getName()).addFileExtension("peer").toFile(); //$NON-NLS-1$
								if (file.exists() && !overwrite) {
									if (!toggleState || toggleResult < 0) {
										MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(
														getShell(), null,
														NLS.bind(Messages.PeerExportWizardPage_overwriteDialog_message, file.toString()),
														Messages.PeerExportWizardPage_overwriteDialogToggle_message, toggleState, null, null);
										toggleState = dialog.getToggleState();
										toggleResult = dialog.getReturnCode();
									}
									if (toggleResult != IDialogConstants.YES_ID) {
										continue;
									}
								}
								service.write(config, file.toURI());
							}
							catch (Exception e) {
							}
						}
						monitor.worked(1);
					}
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		exportjob.schedule();
		return true;
	}
}
