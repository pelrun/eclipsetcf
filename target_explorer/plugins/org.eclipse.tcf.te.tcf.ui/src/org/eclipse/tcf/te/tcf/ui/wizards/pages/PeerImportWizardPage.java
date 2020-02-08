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
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IImportPersistenceService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.progress.UIJob;

/**
 * PeerImportWizardPage
 */
public class PeerImportWizardPage extends WizardPage {

	private static final int SIZING_TEXT_FIELD_WIDTH = 250;
	private static final String OVERWRITE = "overwrite"; //$NON-NLS-1$
	private static final String OLD_PATH = "oldpath"; //$NON-NLS-1$

	CheckboxTableViewer fViewer;
	Text fLocationField;
	private Button fLocationButton;
	private Button fOverwrite;

	/**
	 * Constructor
	 */
	public PeerImportWizardPage() {
		super(Messages.PeerImportWizard_title);
		setTitle(Messages.PeerImportWizard_title);
		setMessage(Messages.PeerImportWizard_message);
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
						| GridData.HORIZONTAL_ALIGN_FILL));
		composite.setFont(parent.getFont());

		createLocationGroup(composite);
		createPeersGroup(composite);

		if (getDialogSettings().get(OLD_PATH) != null) {
			fLocationField.setText(getDialogSettings().get(OLD_PATH));
		}
		else {
			fLocationField.setText(Platform.getLocation().toOSString());
		}

		setPageComplete(isComplete());
		setErrorMessage(null);

		setControl(composite);
	}

	/**
	 * Creates the checkbox tree and list for selecting peers.
	 *
	 * @param parent the parent control
	 */
	@SuppressWarnings("unused")
	private final void createPeersGroup(Composite parent) {
		Composite resourcesGroup = new Composite(parent, SWT.NONE);
		resourcesGroup.setLayout(new GridLayout());
		resourcesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		resourcesGroup.setFont(parent.getFont());

		new Label(resourcesGroup, SWT.NONE).setText(Messages.PeerImportWizardPage_peers_label);
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
				List<IPeer> elements = new ArrayList<IPeer>();
				File[] candidates = ((IPath)inputElement).toFile().listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						IPath path = new Path(pathname.getAbsolutePath());
						return path.getFileExtension() != null && path.getFileExtension().toLowerCase().equals("peer"); //$NON-NLS-1$
					}
				});
				// If there are "*.peer" files to read, process them
				if (candidates != null && candidates.length > 0) {
					for (File candidate : candidates) {
						try {
							IURIPersistenceService service = ServiceManager.getInstance().getService(IURIPersistenceService.class);
							if (service != null) {
								IPeer tempPeer = (IPeer)service.read(IPeer.class, candidate.getAbsoluteFile().toURI());
								elements.add(tempPeer);
							}
						}
						catch (Exception e) {
						}
					}
				}

				return elements.toArray();
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

		// top level group
		Composite buttonComposite = new Composite(resourcesGroup, SWT.NONE);
		buttonComposite.setFont(parent.getFont());

		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight= layout.marginWidth= 0;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL	| GridData.HORIZONTAL_ALIGN_FILL));

		Button selectButton = createButton(buttonComposite,
						IDialogConstants.SELECT_ALL_ID, Messages.PeerImportWizardPage_selectAll, false);

		selectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fViewer.setAllChecked(true);
				setPageComplete(isComplete());
			}
		});

		Button deselectButton = createButton(buttonComposite,
						IDialogConstants.DESELECT_ALL_ID, Messages.PeerImportWizardPage_deselectAll, false);

		deselectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fViewer.setAllChecked(false);
				setPageComplete(isComplete());
			}
		});

		new Label(resourcesGroup, SWT.NONE);

		fOverwrite = new Button(resourcesGroup, SWT.CHECK);
		fOverwrite.setText(Messages.PeerImportWizardPage_overwrite_button);
		fOverwrite.setSelection(getDialogSettings().getBoolean(OVERWRITE));

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

	private void createLocationGroup(Composite parent) {
		Font font = parent.getFont();
		// destination specification group
		Composite destinationSelectionGroup = new Composite(parent, SWT.NONE);
		destinationSelectionGroup.setLayout(new GridLayout(3, false));
		destinationSelectionGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		destinationSelectionGroup.setFont(font);

		Label destinationLabel = new Label(destinationSelectionGroup, SWT.NONE);
		destinationLabel.setText(Messages.PeerImportWizardPage_destination_label);
		destinationLabel.setFont(font);

		// destination name entry field
		fLocationField = new Text(destinationSelectionGroup, SWT.BORDER);
		fLocationField.setFont(font);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
		fLocationField.setLayoutData(gd);
		fLocationField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fViewer.setInput(new Path(fLocationField.getText()));
			}
		});

		fLocationButton = createButton(destinationSelectionGroup, IDialogConstants.SELECT_ALL_ID, Messages.PeerImportWizardPage_destination_button, false);
		fLocationButton.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getContainer().getShell());
				dd.setText(Messages.PeerImportWizardPage_destination_label);
				String file = dd.open();
				if(file != null) {
					IPath path = new Path(file);
					fLocationField.setText(path.toOSString());
					setPageComplete(isComplete());
				}
			}
		});
	}

	/**
	 * Returns if the page is complete
	 * @return true if the page is complete and can be 'finished', false otherwise
	 */
	protected boolean isComplete() {
		Object[] elements = fViewer.getCheckedElements();
		boolean selected = false;
		for (Object element : elements) {
			if(element instanceof IPeer) {
				selected = true;
				break;
			}
		}
		if(elements.length < 1 || !selected) {
			setErrorMessage(Messages.PeerImportWizardPage_peersMissing_error);
			return false;
		}
		String path = fLocationField.getText().trim();
		if(path.length() == 0) {
			setErrorMessage(Messages.PeerImportWizardPage_locationMissing_error);
			return false;
		}
		if ((new File(path)).isFile()) {
			setErrorMessage(Messages.PeerImportWizardPage_locationIsFile_error);
			return false;
		}
		setErrorMessage(null);
		setMessage(Messages.PeerImportWizard_message);
		return true;
	}

	public boolean finish() {
		final Object[] configs = fViewer.getCheckedElements();
		final boolean overwrite = fOverwrite.getSelection();
		final String path = fLocationField.getText().trim();
		IDialogSettings settings = getDialogSettings();
		settings.put(OVERWRITE, overwrite);
		settings.put(OLD_PATH, path);
		UIJob importjob = new UIJob(getContainer().getShell().getDisplay(), Messages.PeerImportWizard_title) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				final IPeerModel model = ModelManager.getPeerModel();
				final IProgressMonitor finalMonitor;
				if (monitor == null) {
					finalMonitor = new NullProgressMonitor();
				}
				else {
					finalMonitor = monitor;
				}
				finalMonitor.beginTask(Messages.PeerImportWizard_title, configs.length);
				boolean toggleState = false;
				int toggleResult = -1;
				for (final Object config : configs) {
					if (config instanceof IPeer) {
						final AtomicReference<IPeerNode> peerNode = new AtomicReference<IPeerNode>();
						Protocol.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								peerNode.set(model.getService(IPeerModelLookupService.class).lkupPeerModelById(((IPeer)config).getID()));
								if (peerNode.get() == null) {
									for (IPeerNode peer : model.getPeerNodes()) {
											String name = peer.getPeer().getName();
											if (name.equalsIgnoreCase(((IPeer)config).getName())) {
												peerNode.set(peer);
												break;
											}
									}
								}
							}
						});
						// And create a new one if we cannot find it
						IURIPersistenceService service = ServiceManager.getInstance().getService(IImportPersistenceService.class);
						if (service == null) {
							service = ServiceManager.getInstance().getService(IURIPersistenceService.class);
						}
						if (peerNode.get() != null) {
							if (!toggleState || toggleResult < 0) {
								MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(
												getShell(), null,
												NLS.bind(Messages.PeerImportWizardPage_overwriteDialog_message, peerNode.get().getName()),
												Messages.PeerImportWizardPage_overwriteDialogToggle_message, toggleState, null, null);
								toggleState = dialog.getToggleState();
								toggleResult = dialog.getReturnCode();
							}
							if (toggleResult != IDialogConstants.YES_ID) {
								continue;
							}
							try {
								service.delete(peerNode.get().getPeer(), null);
							}
							catch (IOException e) {
							}
						}
						try {
							URI uri = service.getURI(config);
							File file = new File(uri.normalize());
							service.write(config, file.toURI());
						}
						catch (IOException e) {
						}
					}
					finalMonitor.worked(1);
				}
				Protocol.invokeLater(new Runnable() {
					@Override
					public void run() {
						model.getService(IPeerModelRefreshService.class).refresh(null);
					}
				});
				finalMonitor.done();
				return Status.OK_STATUS;
			}
		};
		importjob.schedule();
		return true;
	}
}
