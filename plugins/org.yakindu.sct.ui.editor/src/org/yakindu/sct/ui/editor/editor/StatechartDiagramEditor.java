/**
 * Copyright (c) 2011 committers of YAKINDU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * 	committers of YAKINDU - initial API and implementation
 * 
 */
package org.yakindu.sct.ui.editor.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gmf.runtime.common.ui.services.marker.MarkerNavigationService;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.ui.internal.parts.DiagramGraphicalViewerKeyHandler;
import org.eclipse.gmf.runtime.gef.ui.internal.editparts.AnimatableZoomManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.yakindu.base.base.DomainElement;
import org.yakindu.base.xtext.utils.gmf.resource.DirtyStateListener;
import org.yakindu.sct.domain.extension.DomainRegistry;
import org.yakindu.sct.domain.extension.DomainStatus;
import org.yakindu.sct.domain.extension.DomainStatus.Severity;
import org.yakindu.sct.domain.extension.IDomain;
import org.yakindu.sct.ui.editor.DiagramActivator;
import org.yakindu.sct.ui.editor.partitioning.DiagramPartitioningEditor;
import org.yakindu.sct.ui.editor.partitioning.DiagramPartitioningUtil;
import org.yakindu.sct.ui.editor.proposals.ContentProposalViewerKeyHandler;
import org.yakindu.sct.ui.editor.providers.ISCTOutlineFactory;
import org.yakindu.sct.ui.editor.utils.HelpContextIds;
import org.yakindu.sct.ui.editor.validation.IValidationIssueStore;
import org.yakindu.sct.ui.editor.validation.LiveValidationListener;

import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * @author andreas muelder - Initial contribution and API
 * @author martin esser
 */
@SuppressWarnings("restriction")
public class StatechartDiagramEditor extends DiagramPartitioningEditor implements IGotoMarker {

	private static final int INITIAL_PALETTE_SIZE = 175;
	private static final Font INVALID_DOMAIN_FONT = new Font(null, new FontData("Verdana", 10, SWT.BOLD));
	public static final String ID = "org.yakindu.sct.ui.editor.editor.StatechartDiagramEditor";

	private KeyHandler keyHandler;

	private DirtyStateListener domainAdapter;
	private LiveValidationListener validationListener;
	private IValidationIssueStore issueStore;

	public StatechartDiagramEditor() {
		super(true);
	}

	public boolean isEditable() {
		DomainStatus domainStatus = getDomainStatus();
		if (domainStatus == null || domainStatus.getSeverity() == Severity.ERROR) {
			return false;
		}
		return super.isEditable();
	}

	protected DomainStatus getDomainStatus() {
		EObject element = getDiagram().getElement();
		DomainElement domainElement = EcoreUtil2.getContainerOfType(element, DomainElement.class);
		if (domainElement != null) {
			DomainStatus domainStatus = DomainRegistry.getDomainStatus(domainElement.getDomainID());
			return domainStatus;
		}
		return null;
	};

	@Override
	protected void createBreadcrumbViewer(Composite parent) {
		DomainStatus domainStatus = getDomainStatus();
		if (domainStatus != null && domainStatus.getSeverity() == Severity.ERROR) {
			createStatusLabel(parent, domainStatus);
			return;
		}
		super.createBreadcrumbViewer(parent);
	}

	private void createStatusLabel(Composite parent, DomainStatus domainStatus) {
		CLabel label = new CLabel(parent, SWT.SHADOW_OUT);
		label.setFont(INVALID_DOMAIN_FONT);
		label.setBackground(ColorConstants.white);
		label.setForeground(ColorConstants.red);
		label.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		label.setText(domainStatus.getMessage() + " - editor is in readonly mode.");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
		parent.pack(true);
	}

	protected Object createOutline(Class<?> type) {
		Injector editorInjector = getEditorInjector();
		boolean outlineBindingExists = null != editorInjector.getExistingBinding(Key.get(ISCTOutlineFactory.class));
		if (!outlineBindingExists) {
			// get the GMF default outline
			return super.getAdapter(type);
		}
		ISCTOutlineFactory outlineFactory = editorInjector.getInstance(ISCTOutlineFactory.class);
		return outlineFactory.createOutline(this);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		checkXtextNature();
		registerValidationListener();
	}

	protected void registerValidationListener() {
		issueStore = getEditorInjector().getInstance(IValidationIssueStore.class);
		issueStore.connect(getDiagram().eResource());
		validationListener = getEditorInjector().getInstance(LiveValidationListener.class);
		validationListener.setResource(getDiagram().eResource());
		validationListener.setValidationIssueProcessor(issueStore);
		getEditingDomain().addResourceSetListener(validationListener);
		validationListener.scheduleValidation();
	}

	protected Injector getEditorInjector() {
		IDomain domain = DomainRegistry.getDomain(getDiagram().getElement());
		Injector injector = domain.getInjector(IDomain.FEATURE_EDITOR);
		return injector;
	}

	protected void checkXtextNature() {
		IFileEditorInput editorInput = (IFileEditorInput) getEditorInput();
		IProject project = editorInput.getFile().getProject();
		if (project != null && !XtextProjectHelper.hasNature(project) && project.isAccessible()
				&& !project.isHidden()) {
			addNature(project);
		}
	}

	public void addNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = XtextProjectHelper.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected TransactionalEditingDomain createEditingDomain() {
		TransactionalEditingDomain domain = DiagramPartitioningUtil.getSharedDomain();
		domainAdapter = new DirtyStateListener();
		domain.addResourceSetListener(domainAdapter);
		return domain;
	}

	public void gotoMarker(IMarker marker) {
		MarkerNavigationService.getInstance().gotoMarker(this, marker);
	}

	@Override
	protected PreferencesHint getPreferencesHint() {
		return DiagramActivator.DIAGRAM_PREFERENCES_HINT;
	}

	@Override
	protected void createGraphicalViewer(Composite parent) {
		super.createGraphicalViewer(parent);
		IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
		helpSystem.setHelp(getGraphicalViewer().getControl(), HelpContextIds.SC_EDITOR_GRAPHICAL_VIEWER);

	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		disableAnimatedZoom();
		createContentProposalViewerKeyHandler();
	}

	// Disable the animated zoom, it is too slow for bigger models
	protected void disableAnimatedZoom() {
		AnimatableZoomManager zoomManager = (AnimatableZoomManager) getGraphicalViewer()
				.getProperty(ZoomManager.class.toString());
		zoomManager.setZoomAnimationStyle(ZoomManager.ANIMATE_NEVER);
	}

	protected void createContentProposalViewerKeyHandler() {
		ContentProposalViewerKeyHandler contentProposalHandler = new ContentProposalViewerKeyHandler(
				getGraphicalViewer());
		contentProposalHandler
				.setParent(new DiagramGraphicalViewerKeyHandler(getGraphicalViewer()).setParent(getKeyHandler()));
		getGraphicalViewer().setKeyHandler(contentProposalHandler);
	}

	/**
	 * Overrides the GMF key handler to fix key binding for zooming and to
	 * remove unused key bindings.
	 */
	@Override
	protected KeyHandler getKeyHandler() {
		if (keyHandler == null) {
			keyHandler = new KeyHandler();

			registerZoomActions();
			
			// Zoom in - Unix - Numpad plus
			getKeyHandler().put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, SWT.MOD1),
					getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));

			// Zoom in - Unix - Numpad minus
			getKeyHandler().put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, SWT.MOD1),
					getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));

			// Zoom out - all OS - German and English keyboard layout
			getKeyHandler().put(KeyStroke.getPressed('-', 0x2d, SWT.MOD1),
					getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));

			// Zoom in - all OS - English keyboard layout
			getKeyHandler().put(KeyStroke.getPressed('=', 0x3d, SWT.MOD1),
					getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));

			// Zoom in - Unix - German layout ([CTRL++] propagates char '+')
			getKeyHandler().put(KeyStroke.getPressed('+', 0x2b, SWT.MOD1),
					getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));

			// Zoom in - Windows - German layout ([CTRL++] propagates char 0x1d)
			getKeyHandler().put(KeyStroke.getPressed((char) 0x1d, 0x2b, SWT.MOD1),
					getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));

			// Test Error - for AERI testing only
			// DOWN: stateMask=0x50000 CTRL ALT, keyCode=0x6c 'l', character=0xc
			// ' '
			getKeyHandler().put(KeyStroke.getPressed((char) 0xc, 0x6c, 0x50000), new Action() {
				@Override
				public void run() {
					DiagramActivator.getDefault().getLog()
							.log(new Status(IStatus.ERROR, DiagramActivator.PLUGIN_ID, "AERI Testing error"));
				}
			});

		}
		return keyHandler;
	}

	@SuppressWarnings("unchecked")
	protected void registerZoomActions() {
		IAction action;
		action = new ZoomInAction(getZoomManager());
		action.setText(""); //$NON-NLS-1$ // no text necessary since this
							// is not a visible action
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZoomOutAction(getZoomManager());
		action.setText(""); //$NON-NLS-1$ // no text necessary since this
							// is not a visible action
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());
	}

	@Override
	public String getContributorId() {
		return ID;
	}

	@Override
	public void dispose() {
		if (validationListener != null) {
			validationListener.dispose();
		}
		if (issueStore != null)
			issueStore.disconnect(getDiagram().eResource());
		getEditingDomain().removeResourceSetListener(validationListener);
		getEditingDomain().removeResourceSetListener(domainAdapter);
		if (domainAdapter != null)
			domainAdapter.dispose();
		super.dispose();
	}

	@Override
	protected int getInitialPaletteSize() {
		return INITIAL_PALETTE_SIZE;
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
		if (IContentOutlinePage.class.equals(type)) {
			return createOutline(type);
		} else if (IValidationIssueStore.class.equals(type)) {
			return issueStore;
		}
		return super.getAdapter(type);
	}
}
