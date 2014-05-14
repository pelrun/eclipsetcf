/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.core.interfaces.IContextDataProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.utils.PeerDataHelper;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessContextItem;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IProcessesDataProperties;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelLookupService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelRefreshService;
import org.eclipse.tcf.te.tcf.processes.core.util.ProcessDataHelper;
import org.eclipse.tcf.te.tcf.processes.ui.editor.tree.ContentProvider;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.ui.interfaces.IDataExchangeDialog;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog;
import org.eclipse.tcf.te.ui.trees.TreeViewerSorterCaseInsensitive;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog for selecting contexts to attach to.
 */
public class AttachContextSelectionDialog extends CustomTitleAreaDialog implements IEventListener, IDataExchangeDialog {
	protected TreeViewer viewer;

	private boolean initDone = false;

	IPeerNode peerNode;
	IPropertiesContainer data = null;

	Text filter;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 */
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
	    return getDialogSettings();
	}

	/**
	 * Constructor
	 */
	public AttachContextSelectionDialog(Shell parent, String contextHelpId) {
		super(parent, contextHelpId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createDialogAreaContent(Composite parent) {
		super.createDialogAreaContent(parent);

		// Set dialog title and default message
		setDialogTitle(Messages.AttachContextSelectionDialog_dialogTitle);
		setTitle(Messages.AttachContextSelectionDialog_title);
		setDefaultMessage(Messages.AttachContextSelectionDialog_message, IMessageProvider.INFORMATION);

		// Create the inner panel
		Composite panel = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label filterLabel = new Label(panel, SWT.NONE);
		filterLabel.setText(Messages.AttachContextSelectionDialog_filter_label);

		filter = new Text(panel, SWT.BORDER);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		filter.setLayoutData(gd);
		filter.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				viewer.refresh();
				viewer.expandAll();
			}
		});

		Label contextLabel = new Label(panel, SWT.NONE);
		contextLabel.setText(Messages.AttachContextSelectionDialog_contexts_label);

		viewer = new TreeViewer(panel, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		Tree fModuleTree = viewer.getTree();
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.minimumHeight = 300;
		gd.minimumWidth = 300;
		gd.widthHint = 300;
		gd.heightHint = 300;
		fModuleTree.setLayoutData(gd);

		viewer.setContentProvider(new ContentProvider());
		DelegatingLabelProvider labelProvider = new DelegatingLabelProvider() {
			/* (non-Javadoc)
			 * @see org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider#decorateText(java.lang.String, java.lang.Object)
			 */
			@Override
			public String decorateText(String text, final Object element) {
				final AtomicBoolean isAttached = new AtomicBoolean();
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						isAttached.set(isAttached(element));
					}
				});
				if (isAttached.get()) {
					return NLS.bind(Messages.AttachContextSelectionDialog_allReadyAttached_decoration, text);
				}
			    return text;
			}
			/* (non-Javadoc)
			 * @see org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider#getForeground(java.lang.Object)
			 */
			@Override
			public Color getForeground(final Object element) {
				final AtomicBoolean canAttach = new AtomicBoolean();
				final AtomicBoolean isAttached = new AtomicBoolean();
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						canAttach.set(canAttach(element));
						isAttached.set(isAttached(element));
					}
				});
				if (!canAttach.get() || isAttached.get()) {
					return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		};
		viewer.setLabelProvider(new DecoratingLabelProvider(labelProvider, labelProvider) );
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (event.getSelection() instanceof IStructuredSelection &&
								((IStructuredSelection)event.getSelection()).size() > 0) {
					final Object selected = ((IStructuredSelection)event.getSelection()).getFirstElement();
					final AtomicBoolean valid = new AtomicBoolean();
					Protocol.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							valid.set(isValid(selected));						}
					});
					if (valid.get()) {
						okPressed();
					}
				}
			}
		});

		viewer.setSorter(new TreeViewerSorterCaseInsensitive());
		viewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, final Object element) {
				if (element instanceof IProcessContextNode) {
					final IProcessContextNode node = (IProcessContextNode)element;
					String filterStr = filter.getText().trim();
					filterStr = filterStr.replaceAll("\\*",	".*"); //$NON-NLS-1$ //$NON-NLS-2$
					filterStr = filterStr.replaceAll("\\?",	"."); //$NON-NLS-1$ //$NON-NLS-2$
					filterStr += ".*"; //$NON-NLS-1$
					if (!matchesFilter(filterStr, node)) {
						return false;
					}
				}
				return true;
			}

			private boolean matchesFilter(final String filterStr, final IProcessContextNode node) {
				boolean childMatches = false;
				if (node.hasChildren()) {
					for (IModelNode child : node.getChildren()) {
	                    if (child instanceof IProcessContextNode) {
	                    	if (matchesFilter(filterStr, (IProcessContextNode)child)) {
	                    		childMatches = true;
	                    		break;
	                    	}
	                    }
	                    else {
	                    	childMatches = true;
	                    	break;
	                    }
                    }
				}
				final AtomicBoolean filterMatches = new AtomicBoolean();
				final AtomicBoolean canAttach = new AtomicBoolean();
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						filterMatches.set(node.getName().toLowerCase().matches(filterStr.toLowerCase()));
						canAttach.set(canAttach(node));
					}
				});
				return (filterMatches.get() || childMatches) && canAttach.get();
			}
		});

		EventManager.getInstance().addEventListener(this, ChangeEvent.class);

		restoreWidgetValues();
		setupData(data);
		applyDialogFont(panel);

		initDone = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		validate();
		return control;
	}

	protected void validate() {
		if (!initDone) {
			return;
		}

		final AtomicBoolean valid = new AtomicBoolean(false);
		final ISelection selection = viewer.getSelection();

		if (selection instanceof IStructuredSelection &&
						((IStructuredSelection)selection).size() > 0) {
			valid.set(true);
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					Iterator<?> it = ((IStructuredSelection)selection).iterator();
					while (valid.get() && it.hasNext()) {
						Object sel = it.next();
						valid.set(isValid(sel));
					}
				}
			});
		}

		if (valid.get()) {
			setErrorMessage(null);
		}

		getButton(IDialogConstants.OK_ID).setEnabled(valid.get());
	}

	protected boolean canAttach(Object selection) {
		if (selection instanceof IProcessContextNode) {
			IProcessContextNode node = (IProcessContextNode)selection;
			if (node.getProcessContext().getProperties().containsKey("CanAttach")) { //$NON-NLS-1$
				Boolean value = (Boolean)node.getProcessContext().getProperties().get("CanAttach"); //$NON-NLS-1$
				return value != null && value.booleanValue();
			}
		}
		return false;
	}

	protected boolean isAttached(Object selection) {
		if (selection instanceof IProcessContextNode) {
			IProcessContextNode node = (IProcessContextNode)selection;
			return node.getProcessContext().isAttached();
		}
		return false;
	}

	protected boolean isValid(Object selection) {
		return canAttach(selection) && !isAttached(selection);
	}

	/*
	 * (non-Javadoc)
	 * @see com.windriver.ide.target.ui.dialogs.UnifiedDialog#saveWidgetValues()
	 */
	@Override
	public void saveWidgetValues() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#restoreWidgetValues()
	 */
	@Override
	protected void restoreWidgetValues() {
		super.restoreWidgetValues();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#dispose()
	 */
	@Override
	protected void dispose() {
		super.dispose();
		viewer = null;

		EventManager.getInstance().removeEventListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
    @Override
    public void eventFired(EventObject event) {
    	if (event instanceof ChangeEvent) {
    		final ChangeEvent e = (ChangeEvent)event;
    		if (IContainerModelNode.NOTIFY_CHANGED.equals(e.getEventId()) &&
    						(e.getSource() instanceof IRuntimeModel || e.getSource() instanceof IProcessContextNode)) {
    			IPeerNode node = (IPeerNode)Platform.getAdapterManager().getAdapter(e.getSource(), IPeerNode.class);
    			if (node == peerNode) {
    				getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
		    				viewer.refresh(e.getSource(), true);
						}
					});
    			}
    		}
    	}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
    @Override
    public void setupData(IPropertiesContainer data) {
		this.data = new PropertiesContainer();
		this.data.setProperties(data.getProperties());

		IPeerNode[] contexts = PeerDataHelper.decodeContextList(data.getStringProperty(IContextDataProperties.PROPERTY_CONTEXT_LIST));
		IPeerNode newPeerNode = contexts != null && contexts.length > 0  ? contexts[0] : null;

		if (peerNode == null || peerNode != newPeerNode) {
			peerNode = newPeerNode;
			if (viewer != null) {
				viewer.setInput(peerNode);
				viewer.expandAll();
				viewer.refresh(true);
			}
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					IRuntimeModelRefreshService refresh = ModelManager.getRuntimeModel(peerNode).getService(IRuntimeModelRefreshService.class);
					refresh.refresh(new Callback() {
						@Override
						protected void internalDone(Object caller, IStatus status) {
							if (viewer != null) {
							getShell().getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
								    internalSetupData();
								}
							});
							}
						}
					});
				}
			});
		}
		else {
			internalSetupData();
		}
    }

    protected void internalSetupData() {
		if (viewer != null) {
			viewer.setInput(peerNode);
			viewer.expandAll();
			viewer.refresh(true);
			final List<IProcessContextNode> ctxNodes = new ArrayList<IProcessContextNode>();
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					for (IProcessContextItem item : ProcessDataHelper.decodeProcessContextItems(AttachContextSelectionDialog.this.data.getStringProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST))) {
						IRuntimeModelLookupService lkup = ModelManager.getRuntimeModel(peerNode).getService(IRuntimeModelLookupService.class);
						IModelNode[] nodes = null;
						if (item.getId() != null) {
							nodes = lkup.lkupModelNodesById(item.getId());
						}
						if (nodes == null || nodes.length == 0) {
							nodes = lkup.lkupModelNodesByName(item.getName());
						}

						if (nodes != null) {
							for (IModelNode node : nodes) {
				                if (node instanceof IProcessContextNode) {
				                	ctxNodes.add((IProcessContextNode)node);
				                }
			                }
						}
					}
				}
			});
			viewer.setSelection(new StructuredSelection(ctxNodes));
		}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
    @Override
    public void extractData(IPropertiesContainer data) {
		Assert.isNotNull(data);
		data.setProperties(this.data.getProperties());
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		// Extract the properties
		if (data == null) {
			data = new PropertiesContainer();
		}
    	if (viewer != null) {
	    	ISelection sel = viewer.getSelection();
	    	final List<IProcessContextItem> items = new ArrayList<IProcessContextItem>();
	    	if (sel instanceof IStructuredSelection) {
	    		final Iterator<?> it = ((IStructuredSelection)sel).iterator();
	    		while (it.hasNext()) {
	    			Object obj = it.next();
	    			if (obj instanceof IProcessContextNode) {
	    				IProcessContextItem item = ProcessDataHelper.getProcessContextItem((IProcessContextNode)obj);
	    				if (item != null) {
	    					items.add(item);
	    				}
	    			}
	    		}
	    	}
	    	data.setProperty(IProcessesDataProperties.PROPERTY_CONTEXT_LIST, ProcessDataHelper.encodeProcessContextItems(items.toArray(new IProcessContextItem[items.size()])));
    	}

		super.okPressed();
	}
}
