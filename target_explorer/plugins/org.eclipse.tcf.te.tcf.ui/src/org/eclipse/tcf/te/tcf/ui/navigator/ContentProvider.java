/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerRedirector;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.internal.preferences.IPreferenceKeys;
import org.eclipse.tcf.te.tcf.ui.navigator.nodes.PeerRedirectorGroupNode;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.views.Managers;
import org.eclipse.tcf.te.ui.views.extensions.CategoriesExtensionPointManager;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IRoot;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.internal.navigator.NavigatorFilterService;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.ICommonFilterDescriptor;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.INavigatorFilterService;


/**
 * Content provider implementation.
 */
@SuppressWarnings("restriction")
public class ContentProvider implements ICommonContentProvider, ITreePathContentProvider {
	private final static Object[] NO_ELEMENTS = new Object[0];

	// The "Redirected Peers" filter id
	private final static String REDIRECT_PEERS_FILTER_ID = "org.eclipse.tcf.te.tcf.ui.navigator.RedirectPeersFilter"; //$NON-NLS-1$
	// The current user filter id
	private final static String CURRENT_USER_FILTER_ID = "org.eclipse.tcf.te.tcf.ui.navigator.PeersByCurrentUserFilter"; //$NON-NLS-1$


	// The peer model listener instance
	/* default */ IPeerModelListener peerModelListener = null;
	// The locator model listener instance
	/* default */ ILocatorModelListener locatorModelListener = null;

	// Internal map of PeerRedirectorGroupNodes per peer id
	private final Map<String, PeerRedirectorGroupNode> roots = new HashMap<String, PeerRedirectorGroupNode>();

	// Flag to remember if invisible nodes are to be included in the list of
	// returned children.
	private final boolean showInvisible;

	INavigatorFilterService navFilterService = null;

	/**
	 * Constructor.
	 */
	public ContentProvider() {
		this(false);
	}

	/**
	 * Constructor.
	 *
	 * @param showInvisible If <code>true</code>, {@link #getChildren(Object)} will include invisible nodes too.
	 */
	public ContentProvider(boolean showInvisible) {
		super();
		this.showInvisible = showInvisible;
	}

	/**
	 * Determines if the given peer model node is a value-add.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @return <code>True</code> if the peer model node is a value-add, <code>false</code> otherwise.
	 */
	/* default */ final boolean isValueAdd(IPeer peer) {
		Assert.isNotNull(peer);

		String value = peer.getAttributes().get("ValueAdd"); //$NON-NLS-1$
		boolean isValueAdd = value != null && ("1".equals(value.trim()) || Boolean.parseBoolean(value.trim())); //$NON-NLS-1$

		return isValueAdd;
	}

	/**
	 * Determines if the given peer model node is filtered from the view completely.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @return <code>True</code> if filtered, <code>false</code> otherwise.
	 */
	/* default */ final boolean isFiltered(IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		boolean filtered = false;

		if (!showInvisible) {
			filtered |= !peerNode.isVisible();
		}

		return filtered;
	}

	/**
	 * Determines if the given peer node is filtered from the view completely.
	 *
	 * @param peerNode The peer node. Must not be <code>null</code>.
	 * @return <code>True</code> if filtered, <code>false</code> otherwise.
	 */
	/* default */ final boolean isFiltered(IPeer peer) {
		Assert.isNotNull(peer);

		boolean filtered = false;
		boolean hideValueAdds = CoreBundleActivator.getScopedPreferences().getBoolean(org.eclipse.tcf.te.tcf.locator.interfaces.preferences.IPreferenceKeys.PREF_HIDE_VALUEADDS);

		filtered |= isValueAdd(peer) && hideValueAdds;

		filtered |= peer.getName() != null
						&& peer.getName().endsWith("Command Server"); //$NON-NLS-1$

		return filtered;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		Object[] children = NO_ELEMENTS;

		// The category element if the parent element is a category node
		final ICategory category = parentElement instanceof ICategory ? (ICategory)parentElement : null;
		// The category id if the parent element is a category node
		final String catID = category != null ? category.getId() : null;

		// Determine if both the "My Targets" and "Neighborhood" categories are hidden
		boolean allHidden = false;

		ICategory myTargetsCat = CategoriesExtensionPointManager.getInstance().getCategory(IUIConstants.ID_CAT_MY_TARGETS, false);
		ICategory neighborhoodCat = CategoriesExtensionPointManager.getInstance().getCategory(IUIConstants.ID_CAT_NEIGHBORHOOD, false);

		allHidden = neighborhoodCat != null && !neighborhoodCat.isEnabled() && myTargetsCat != null && !myTargetsCat.isEnabled();

		// If the parent element is a category, than we assume
		// the locator model as parent element.
		if (parentElement instanceof ICategory) {
			parentElement = ModelManager.getPeerModel();
		}
		// If the parent element is the root element and "all"
		// categories are hidden, assume the locator model as parent element
		if (parentElement instanceof IRoot && allHidden) {
			parentElement = ModelManager.getPeerModel();
		}

		// If it is the locator model, get the peers
		if (parentElement instanceof IPeerModel) {
			final IPeerModel model = (IPeerModel)parentElement;
			final IPeerNode[] peerNodes = model.getPeerNodes();
			final List<Object> candidates = new ArrayList<Object>();

			if (IUIConstants.ID_CAT_FAVORITES.equals(catID)) {
				for (IPeerNode peerNode : peerNodes) {
					ICategorizable categorizable = (ICategorizable)peerNode.getAdapter(ICategorizable.class);
					if (categorizable == null) {
						categorizable = (ICategorizable)Platform.getAdapterManager().getAdapter(peerNode, ICategorizable.class);
					}
					Assert.isNotNull(categorizable);

					boolean isFavorite = Managers.getCategoryManager().belongsTo(catID, categorizable.getId());
					if (isFavorite && !candidates.contains(peerNode)) {
						candidates.add(peerNode);
					}
				}
			}
			else if (IUIConstants.ID_CAT_MY_TARGETS.equals(catID)) {
				for (IPeerNode peerNode : peerNodes) {
					// Check for filtered nodes (Value-add's and Proxies)
					if (isFiltered(peerNode)) {
						continue;
					}

					ICategorizable categorizable = (ICategorizable)peerNode.getAdapter(ICategorizable.class);
					if (categorizable == null) {
						categorizable = (ICategorizable)Platform.getAdapterManager().getAdapter(peerNode, ICategorizable.class);
					}
					Assert.isNotNull(categorizable);

					boolean isMyTargets = Managers.getCategoryManager().belongsTo(catID, categorizable.getId());
					if (!isMyTargets) {
						Managers.getCategoryManager().addTransient(catID, categorizable.getId());
						isMyTargets = true;
					}

					if (isMyTargets && !candidates.contains(peerNode)) {
						candidates.add(peerNode);
					}
				}
			}
			else if (IUIConstants.ID_CAT_NEIGHBORHOOD.equals(catID)) {
				for (IPeer peer : ModelManager.getLocatorModel().getPeers()) {
					// Check for filtered nodes (Value-add's and Proxies)
					if (isFiltered(peer)) {
						continue;
					}

					if (!candidates.contains(peer)) {
						candidates.add(peer);
					}
				}
			}
			else if (catID != null) {
				for (IPeerNode peerNode : peerNodes) {
					ICategorizable categorizable = (ICategorizable)peerNode.getAdapter(ICategorizable.class);
					if (categorizable == null) {
						categorizable = (ICategorizable)Platform.getAdapterManager().getAdapter(peerNode, ICategorizable.class);
					}
					Assert.isNotNull(categorizable);

					boolean belongsTo = category.belongsTo(peerNode);
					if (belongsTo && !candidates.contains(peerNode)) {
                        Managers.getCategoryManager().addTransient(catID, categorizable.getId());
						candidates.add(peerNode);
					}
				}
			}
			else {
				for (IPeerNode peerNode : peerNodes) {
					// Check for filtered nodes (Value-add's and Proxies)
					if (isFiltered(peerNode)) {
						continue;
					}
					if (!candidates.contains(peerNode)) {
						candidates.add(peerNode);
					}
				}
			}

			children = candidates.toArray(new Object[candidates.size()]);
		}

		return children;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreePathContentProvider#getChildren(org.eclipse.jface.viewers.TreePath)
	 */
	@Override
	public Object[] getChildren(TreePath parentPath) {
		// getChildren is independent of the elements tree path
		return parentPath != null ? getChildren(parentPath.getLastSegment()) : NO_ELEMENTS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(final Object element) {
		// If it is a peer model node, return the parent locator model
		if (element instanceof IPeerNode) {
			// If it is a peer redirector, return the parent remote peer discover root node
			if (((IPeerNode)element).getPeer() instanceof IPeerRedirector) {
				IPeer parentPeer =  ((IPeerRedirector)((IPeerNode)element).getPeer()).getParent();
				String parentPeerId = parentPeer.getID();
				if (!roots.containsKey(parentPeerId)) {
					roots.put(parentPeer.getID(), new PeerRedirectorGroupNode(parentPeerId));
				}
				return roots.get(parentPeerId);
			}

			// Determine the parent category node
			ICategory category = null;
			String[] categoryIds = Managers.getCategoryManager().getCategoryIds(((IPeerNode)element).getPeerId());
			// If we have more than one, take the first one as parent category.
			// To get all parents, the getParents(Object) method must be called
			if (categoryIds != null && categoryIds.length > 0) {
				category = CategoriesExtensionPointManager.getInstance().getCategory(categoryIds[0], false);
			}

			return category != null ? category : ((IPeerNode)element).getModel();
		} else if (element instanceof PeerRedirectorGroupNode) {
			// Return the parent peer model node
			final AtomicReference<IPeerNode> parent = new AtomicReference<IPeerNode>();
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
					parent.set(ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(((PeerRedirectorGroupNode)element).peerId));
				}
			};

			Assert.isTrue(!Protocol.isDispatchThread());

			// The caller thread is very likely the display thread. We have to us a little
			// trick here to avoid blocking the display thread via a wait on a monitor as
			// this can (and has) lead to dead-locks with the TCF event dispatch thread if
			// something fatal (OutOfMemoryError in example) happens in-between.
			ExecutorsUtil.executeWait(new Runnable() {
				@Override
				public void run() {
					Protocol.invokeAndWait(runnable);
				}
			});

			return parent.get();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreePathContentProvider#getParents(java.lang.Object)
	 */
	@Override
	public TreePath[] getParents(Object element) {
		// Not sure if we ever have to calculate the _full_ tree path. The parent NavigatorContentServiceContentProvider
		// is consuming only the last segment.
		List<TreePath> pathes = new ArrayList<TreePath>();

		if (element instanceof IPeerNode) {
			if (Managers.getCategoryManager().belongsTo(IUIConstants.ID_CAT_FAVORITES, ((IPeerNode)element).getPeerId())) {
				// Get the "Favorites" category
				ICategory favCategory = CategoriesExtensionPointManager.getInstance().getCategory(IUIConstants.ID_CAT_FAVORITES, false);
				if (favCategory != null) {
					pathes.add(new TreePath(new Object[] { favCategory }));
				}
			}

			// Determine the default parent
			Object parent = getParent(element);
			if (parent != null) {
				pathes.add(new TreePath(new Object[] { parent }));
			}
		}

		return pathes.toArray(new TreePath[pathes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);

		if (children != null && children.length > 0 && navFilterService != null) {
			for (ViewerFilter filter : navFilterService.getVisibleFilters(true)) {
				children = filter.filter(null, element, children);
			}
		}

		return children != null && children.length > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreePathContentProvider#hasChildren(org.eclipse.jface.viewers.TreePath)
	 */
	@Override
	public boolean hasChildren(TreePath path) {
		// hasChildren is independent of the elements tree path
		return path != null ? hasChildren(path.getLastSegment()) : false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		if (peerModelListener != null) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					ModelManager.getPeerModel().removeListener(peerModelListener);
				}
			};
			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
			peerModelListener = null;
		}
		if (locatorModelListener != null) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					ModelManager.getLocatorModel().removeListener(locatorModelListener);
				}
			};
			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);
			locatorModelListener = null;
		}

		roots.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(final Viewer viewer, Object oldInput, Object newInput) {
		final IPeerModel peerModel = ModelManager.getPeerModel();
		final ILocatorModel locatorModel = ModelManager.getLocatorModel();

		// Create and attach the model listener if not yet done
		if (peerModelListener == null && peerModel != null && viewer instanceof CommonViewer) {
			peerModelListener = new PeerModelListener(peerModel, (CommonViewer)viewer);
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					peerModel.addListener(peerModelListener);
				}
			});
		}
		// Create and attach the model listener if not yet done
		if (locatorModelListener == null && locatorModel != null && viewer instanceof CommonViewer) {
			locatorModelListener = new LocatorModelListener(locatorModel, (CommonViewer)viewer);
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					locatorModel.addListener(locatorModelListener);
				}
			});
		}

		if (peerModel != null && newInput instanceof IRoot) {
			// Refresh the model asynchronously
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					peerModel.getService(IPeerModelRefreshService.class).refresh(null);
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.ICommonContentProvider#init(org.eclipse.ui.navigator.ICommonContentExtensionSite)
	 */
	@Override
	public void init(ICommonContentExtensionSite config) {
		Assert.isNotNull(config);

		// Make sure that the hidden "Redirected Peers" filter is active
		INavigatorContentService cs = config.getService();
		navFilterService = cs != null ? cs.getFilterService() : null;
		if (navFilterService instanceof NavigatorFilterService) {
			final NavigatorFilterService filterService = (NavigatorFilterService)navFilterService;
			boolean activeFiltersChanged = false;

			// Reconstruct the list of active filters based on the visible filter descriptors
			List<String> activeFilderIds = new ArrayList<String>();

			ICommonFilterDescriptor[] descriptors = filterService.getVisibleFilterDescriptors();
			for (ICommonFilterDescriptor descriptor : descriptors) {
				if (descriptor.getId() != null && !"".equals(descriptor.getId()) && filterService.isActive(descriptor.getId())) { //$NON-NLS-1$
					activeFilderIds.add(descriptor.getId());
				}
			}

			if (!activeFilderIds.contains(REDIRECT_PEERS_FILTER_ID)) {
				activeFilderIds.add(REDIRECT_PEERS_FILTER_ID);
				activeFiltersChanged = true;
			}

			if (UIPlugin.getDefault().getPreferenceStore().getBoolean(IPreferenceKeys.PREF_ACTIVATE_CURRENT_USER_FILTER)
					&& !navFilterService.isActive(CURRENT_USER_FILTER_ID)) {
				IDialogSettings settings = UIPlugin.getDefault().getDialogSettings();
				IDialogSettings section = settings.getSection(this.getClass().getSimpleName());
				if (section == null) section = settings.addNewSection(this.getClass().getSimpleName());
				if (!section.getBoolean(IPreferenceKeys.PREF_ACTIVATE_CURRENT_USER_FILTER + ".done")) { //$NON-NLS-1$
					activeFilderIds.add(CURRENT_USER_FILTER_ID);
					activeFiltersChanged = true;
					section.put(IPreferenceKeys.PREF_ACTIVATE_CURRENT_USER_FILTER + ".done", true); //$NON-NLS-1$
				}
			}

			if (activeFiltersChanged) {
				final String[] finActiveFilterIds = activeFilderIds.toArray(new String[activeFilderIds.size()]);
				// Do the update view asynchronous to avoid reentrant viewer calls
				DisplayUtil.safeAsyncExec(new Runnable() {
					@Override
					public void run() {
						filterService.activateFilterIdsAndUpdateViewer(finActiveFilterIds);
					}
				});
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#restoreState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void restoreState(IMemento aMemento) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento aMemento) {
	}
}
