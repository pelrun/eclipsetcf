/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryState;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryType;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.ITransportTypes;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.model.ModelLocationUtil;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.editor.sections.TcpTransportSection;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.navigator.ContentProvider;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.async.UICallbackInvocationDelegate;
import org.eclipse.tcf.te.ui.dialogs.AbstractSectionDialog;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.views.extensions.CategoriesExtensionPointManager;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Locator node selection dialog implementation.
 */
public class LocatorNodeSelectionDialog extends AbstractTreeSelectionDialog implements ILocatorModelListener {

	Button addButton;
	Button deleteButton;
	Button refreshButton;

	final boolean isProxyAllowed;

	/**
	 * Constructor.
	 *
	 * @param shell The shell used to view the dialog, or <code>null</code>.
	 */
	public LocatorNodeSelectionDialog(Shell shell, boolean isProxyAllowed) {
		super(shell, IContextHelpIds.LOCATOR_NODE_SELECTION_DIALOG);

		this.isProxyAllowed = isProxyAllowed;

		ModelManager.getLocatorModel().addListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#createButtonAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonAreaContent(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		comp.setLayout(gl);
	    GridData layoutData = new GridData(SWT.NONE, SWT.FILL, false, true);
	    comp.setLayoutData(layoutData);

	    addButton = new Button(comp, SWT.PUSH);
	    layoutData = new GridData(SWT.FILL, SWT.FILL, false, false);
	    addButton.setLayoutData(layoutData);
		addButton.setText(" " + Messages.LocatorNodeSelectionDialog_button_add + " "); //$NON-NLS-1$ //$NON-NLS-2$
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    onButtonAddSelected();
			}
		});

		deleteButton = new Button(comp, SWT.PUSH);
	    layoutData = new GridData(SWT.FILL, SWT.FILL, false, false);
	    deleteButton.setLayoutData(layoutData);
		deleteButton.setText(" " + Messages.LocatorNodeSelectionDialog_button_delete + " "); //$NON-NLS-1$ //$NON-NLS-2$
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    onButtonDeleteSelected();
			}
		});

		Composite spacer = new Composite(comp, SWT.NONE);
	    layoutData = new GridData(SWT.FILL, SWT.FILL, false, true);
	    spacer.setLayoutData(layoutData);

		refreshButton = new Button(comp, SWT.PUSH);
	    layoutData = new GridData(SWT.FILL, SWT.FILL, false, false);
	    refreshButton.setLayoutData(layoutData);
		refreshButton.setText(" " + Messages.LocatorNodeSelectionDialog_button_refresh + " "); //$NON-NLS-1$ //$NON-NLS-2$
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    onButtonRefreshSelected();
			}
		});
	}

	protected void onButtonAddSelected() {
		final ISelection sel = getViewer().getSelection();
		if (sel instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) sel).getFirstElement();

			String encProxies;
			if (element instanceof ICategory) {
				encProxies = null;
			}
			else if (element instanceof ILocatorNode) {
				ILocatorNode parent = (ILocatorNode) element;
				String parentProxies = parent.getPeer().getAttributes()
				                .get(IPeerProperties.PROP_PROXIES);
				IPeer[] proxies = PeerDataHelper.decodePeerList(parentProxies);
				List<IPeer> proxiesList = new ArrayList<IPeer>(Arrays.asList(proxies));
				proxiesList.add(parent.getPeer());
				proxies = proxiesList.toArray(new IPeer[proxiesList.size()]);
				encProxies = PeerDataHelper.encodePeerList(proxies);
			}
			else {
				return;
			}

			AbstractSectionDialog dialog = new AbstractSectionDialog(getShell(),
							Messages.LocatorNodeSelectionDialog_add_dialogTitle,
							Messages.LocatorNodeSelectionDialog_add_title,
							Messages.LocatorNodeSelectionDialog_add_message,
							false, null) {
				@Override
				protected AbstractSection[] createSections(IManagedForm form, Composite parent) {
					return new AbstractSection[] { new TcpTransportSection(form, parent, false) };
				}
			};
			IPropertiesContainer data = new PropertiesContainer();
			data.setProperty(IPeerProperties.PROP_PROXIES, encProxies);
			data.setProperty(IPeer.ATTR_TRANSPORT_NAME, ITransportTypes.TRANSPORT_TYPE_TCP);
			data.setProperty(IPeer.ATTR_IP_PORT, "1534"); //$NON-NLS-1$
			dialog.setupData(data);

			if (dialog.open() == Window.OK) {
				dialog.extractData(data);

				String proxy = data.getStringProperty(IPeerProperties.PROP_PROXIES);
				String host = data.getStringProperty(IPeer.ATTR_IP_HOST);
				String port = data.getStringProperty(IPeer.ATTR_IP_PORT);
				String transport = data.getStringProperty(IPeer.ATTR_TRANSPORT_NAME);
				String id = transport + ":" + host + ":" + port; //$NON-NLS-1$ //$NON-NLS-2$
				Map<String,String> attrs = new HashMap<String, String>();
				attrs.put(IPeer.ATTR_ID, id);
				attrs.put(IPeer.ATTR_IP_HOST, host);
				attrs.put(IPeer.ATTR_IP_PORT, port);
				attrs.put(IPeer.ATTR_TRANSPORT_NAME, transport);
				attrs.put(IPeerProperties.PROP_PROXIES, proxy);
				final IPeer peer = new TransientPeer(attrs);

				Protocol.invokeLater(new Runnable() {
					@Override
					public void run() {
						final ILocatorModelLookupService lkup = ModelManager.getLocatorModel().getService(ILocatorModelLookupService.class);
						ILocatorModelRefreshService refresh = ModelManager.getLocatorModel().getService(ILocatorModelRefreshService.class);
						ILocatorNode node = lkup.lkupLocatorNode(peer);
						if (node == null || !node.isStatic()) {
							try {
								IURIPersistenceService persistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
								String id = peer.getID();
								String name = id.replaceAll("\\W", "_").trim(); //$NON-NLS-1$ //$NON-NLS-2$
								IPath basePath = ModelLocationUtil.getStaticLocatorsRootLocation();
								IPath path = basePath.append(name).addFileExtension("locator"); //$NON-NLS-1$
								int i = 0;
								while (path.toFile().exists()) {
									path = basePath.append(name+"_"+i).addFileExtension("locator"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								persistenceService.write(peer, path.toFile().toURI());
							}
							catch (Exception e) {
							}
							refresh.refresh(new Callback() {
								@Override
                                protected void internalDone(Object caller, org.eclipse.core.runtime.IStatus status) {
									final ILocatorNode node = lkup.lkupLocatorNode(peer);
									getShell().getDisplay().asyncExec(new Runnable() {
										@Override
										public void run() {
											if (node != null) {
												setSelection(new StructuredSelection(node));
												updateSelection(getSelection());
											}
										}
									});
								}
							});
						}
						else {
							refresh.refresh(node, new Callback() {
								@Override
                                protected void internalDone(Object caller, org.eclipse.core.runtime.IStatus status) {
									final ILocatorNode node = lkup.lkupLocatorNode(peer);
									getShell().getDisplay().asyncExec(new Runnable() {
										@Override
										public void run() {
											if (node != null) {
												setSelection(new StructuredSelection(node));
												updateSelection(getSelection());
											}
										}
									});
								}
							});
						}
					}
				});
			}
		}
	}

	protected void onButtonDeleteSelected() {
		final ISelection sel = getViewer().getSelection();
		if (sel instanceof IStructuredSelection) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					Object element = ((IStructuredSelection)sel).getFirstElement();
					if (element instanceof ILocatorNode) {
						doDelete((ILocatorNode)element);
						ILocatorModelRefreshService refresh = ModelManager.getLocatorModel().getService(ILocatorModelRefreshService.class);
						refresh.refresh(null);
					}
				}
			});
		}
	}

	protected void doDelete(ILocatorNode locatorNode) {
		for (ILocatorNode child : locatorNode.getChildren(ILocatorNode.class)) {
	        doDelete(child);
        }

		if (locatorNode.isStatic()) {
			IURIPersistenceService persistenceService = ServiceManager.getInstance()
			                .getService(IURIPersistenceService.class);
			try {
				persistenceService.delete(locatorNode.isDiscovered() ? locatorNode
				                .getProperty(ILocatorNode.PROPERTY_STATIC_INSTANCE) : locatorNode
				                .getPeer(), null);
			}
			catch (Exception e) {
			}
			ILocatorModelUpdateService update = ModelManager.getLocatorModel()
			                .getService(ILocatorModelUpdateService.class);
			update.remove(locatorNode.getPeer());
		}
	}

	protected void onButtonRefreshSelected() {
		final ISelection sel = getViewer().getSelection();
		if (sel instanceof IStructuredSelection) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					ILocatorModelRefreshService service = ModelManager.getLocatorModel().getService(ILocatorModelRefreshService.class);
					Object element = ((IStructuredSelection)sel).getFirstElement();
					if (element instanceof ICategory) {
						service.refresh(null);
					}
					if (element instanceof ILocatorNode) {
						service.refresh((ILocatorNode)element, null);
					}
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#dispose()
	 */
	@Override
	protected void dispose() {
	    super.dispose();
		ModelManager.getLocatorModel().removeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#getInput()
	 */
	@Override
    protected Object getInput() {
		return ModelManager.getLocatorModel();
	}

	/**
	 * A styled label provider for the target selection list.
	 */
	static class TargetStyledLabelProvider extends DelegatingLabelProvider implements IStyledLabelProvider {
		@Override
        public StyledString getStyledText(Object element) {
	        return new StyledString(getText(element));
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#getLabelProvider()
	 */
	@Override
	protected IBaseLabelProvider getLabelProvider() {
		TargetStyledLabelProvider labelProvider = new TargetStyledLabelProvider();
		return new DecoratingStyledCellLabelProvider(labelProvider, labelProvider, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#getContentProvider()
	 */
	@Override
    protected IContentProvider getContentProvider() {
		return new ContentProvider() {
			@Override
			public Object[] getChildren(Object parentElement) {
				if (isProxyAllowed || parentElement instanceof ILocatorModel || parentElement instanceof ICategory) {
					return super.getChildren(parentElement);
				}
				return new Object[0];
			}
			@Override
			public boolean hasChildren(Object element) {
				if (isProxyAllowed || element instanceof ILocatorModel || element instanceof ICategory) {
					return super.hasChildren(element);
				}
				return false;
			}
		};
	}

    protected Object getViewerSelection() {
		ISelection sel = getViewer().getSelection();
		if (sel instanceof IStructuredSelection) {
			return ((IStructuredSelection)sel).getFirstElement();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#isValidSelection()
	 */
	@Override
	protected boolean isValidSelection() {
		Object element = getViewerSelection();
		return element instanceof ICategory || element instanceof ILocatorNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#getDialogTitle()
	 */
	@Override
    protected String getDialogTitle() {
		return Messages.LocatorNodeSelectionDialog_dialogTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#getTitle()
	 */
	@Override
    protected String getTitle() {
		return Messages.LocatorNodeSelectionDialog_title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#getDefaultMessage()
	 */
	@Override
    protected String getDefaultMessage() {
		return Messages.LocatorNodeSelectionDialog_message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode, boolean)
	 */
    @Override
    public void modelChanged(ILocatorModel model, ILocatorNode locatorNode, boolean added) {
    	refresh();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel)
	 */
    @Override
    public void modelDisposed(ILocatorModel model) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#updateEnablement(org.eclipse.jface.viewers.TreeViewer)
     */
    @Override
    protected void updateEnablement(TreeViewer viewer) {
    	boolean valid = isValidSelection();

    	if (addButton != null && !addButton.isDisposed()) {
    		if (isProxyAllowed) {
    			addButton.setEnabled(valid);
    		}
    		else {
    			addButton.setEnabled(getViewerSelection() instanceof ICategory);
    		}
    	}
    	if (refreshButton != null && !refreshButton.isDisposed()) {
    		refreshButton.setEnabled(valid);
    	}

    	if (deleteButton != null && !deleteButton.isDisposed()) {
    		valid = false;
    		ISelection sel = getViewer().getSelection();
    		if (sel instanceof IStructuredSelection) {
    			Object element = ((IStructuredSelection)sel).getFirstElement();
    			if (element instanceof ILocatorNode) {
    				valid = ((ILocatorNode)element).isStatic();
				}
			}
			deleteButton.setEnabled(valid);
    	}

		super.updateEnablement(viewer);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractTreeSelectionDialog#updateSelection(org.eclipse.jface.viewers.ISelection)
     */
	@Override
    protected void updateSelection(ISelection selection) {
		ICategory category = CategoriesExtensionPointManager.getInstance().getCategory(IUIConstants.ID_CAT_NEIGHBORHOOD, false);
	    if (selection instanceof IStructuredSelection) {
	    	final Object element = ((IStructuredSelection)selection).getFirstElement();
	    	if (element instanceof ILocatorNode) {
	    		final List<Object> treePath = new ArrayList<Object>();
	    		treePath.add(category);
	    		treePath.add(element);
	    		Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
			    		IContainerModelNode container = ((ILocatorNode)element).getParent();
			    		while (container != null) {
			    			treePath.add(1, container);
			    			container = container.getParent();
			    		}
					}
				});

	    		final TreePath path = new TreePath(treePath.toArray());

				final AsyncCallbackCollector collector = new AsyncCallbackCollector(new Callback() {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						if (viewer != null && !viewer.getTree().isDisposed()) {
							viewer.refresh();
							viewer.setSelection(new TreeSelection(path), true);
						}
					}
				}, new UICallbackInvocationDelegate());
	    		for (Object pathNode : treePath) {
	                if (pathNode instanceof ILocatorNode) {
	                	final ILocatorNode locatorNode = (ILocatorNode)pathNode;
	                	IAsyncRefreshableCtx refreshCtx = (IAsyncRefreshableCtx)locatorNode.getAdapter(IAsyncRefreshableCtx.class);
	                	if (refreshCtx.getQueryState(QueryType.CONTEXT) != QueryState.DONE || refreshCtx.getQueryState(QueryType.CHILD_LIST) != QueryState.DONE) {
	                		final ICallback cb = locatorNode.isStatic() ? null : new AsyncCallbackCollector.SimpleCollectorCallback(collector);
	                		Protocol.invokeLater(new Runnable() {
								@Override
								public void run() {
									ModelManager.getLocatorModel().getService(ILocatorModelRefreshService.class).refresh(locatorNode, cb);
								}
							});
	                	}
	                }
                }
                collector.initDone();
	    	}
	    }
	    else {
	    	viewer.expandToLevel(2);
	    	viewer.setSelection(new StructuredSelection(category));
	    }
	}
}
