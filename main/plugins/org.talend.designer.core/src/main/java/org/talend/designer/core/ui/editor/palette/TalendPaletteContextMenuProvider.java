// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.editor.palette;

import java.util.List;
import java.util.Set;

import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.palette.PaletteContextMenuProvider;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.components.ComponentCategory;
import org.talend.core.model.components.ComponentPaletteUtilities;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.general.Project;
import org.talend.core.model.process.IProcess2;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.ui.action.ComponentSearcher;
import org.talend.designer.core.ui.editor.AbstractTalendEditor;
import org.talend.designer.core.ui.editor.PaletteComponentFactory;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.repository.model.ComponentsFactoryProvider;
import org.talend.repository.ui.actions.ShowFavoriteAction;

/**
 * DOC hcw class global comment. Detailled comment
 */
public class TalendPaletteContextMenuProvider extends PaletteContextMenuProvider {

    /**
     * DOC hcw TalendPaletteContextMenuProvider constructor comment.
     * 
     * @param palette
     */

    public TalendPaletteContextMenuProvider(PaletteViewer palette) {
        super(palette);
    }

    // public TalendPaletteContextMenuProvider() {
    //
    // }

    protected boolean isComponentsTypePalette(ComponentCategory componentCategory) {
        if (componentCategory == null) {
            return false;
        }
        EditDomain editDomain = getPaletteViewer().getEditDomain();
        if (editDomain == null || !(editDomain instanceof DefaultEditDomain)) {
            return false;
        }
        IEditorPart editorPart = ((DefaultEditDomain) editDomain).getEditorPart();
        if (editorPart == null || !(editorPart instanceof AbstractTalendEditor)) {
            return false;
        }
        IProcess2 process = ((AbstractTalendEditor) editorPart).getProcess();
        if (process == null) {
            return false;
        }
        return componentCategory.getName().equals(process.getComponentsType());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void buildContextMenu(IMenuManager menu) {
        super.buildContextMenu(menu);
        boolean hasSearchComponentAction = true;

        List editParts = getPaletteViewer().getSelectedEditParts();
        if (!editParts.isEmpty()) {
            PaletteEntry element = (PaletteEntry) ((EditPart) editParts.get(0)).getModel();
            if (editParts.size() > 1) { // search component only process one .
                hasSearchComponentAction = false;
            } else { // check the entry is component or folder
                if (element instanceof CombinedTemplateCreationEntry) {
                    Object template = ((CombinedTemplateCreationEntry) element).getTemplate();
                    if (template == null || !Node.class.equals(template)) {
                        hasSearchComponentAction = false;
                    }
                } else { // not component entry
                    hasSearchComponentAction = false;
                }
            }
            // don't work for camel.
            if (hasSearchComponentAction && isComponentsTypePalette(ComponentCategory.CATEGORY_4_CAMEL)) {
                hasSearchComponentAction = false;
            }
            // note
            boolean note = element.getLabel().equals(Messages.getString("TalendEditorPaletteFactory.Note"));//$NON-NLS-1$
            if (!note) {
                if (hasSearchComponentAction) {
                    menu.appendToGroup(GEFActionConstants.MB_ADDITIONS, new SearchComponentAction(getPaletteViewer()));
                }
                if (ShowFavoriteAction.state == true) {
                    menu.appendToGroup(GEFActionConstants.GROUP_COPY, new FavoriteComponentAction(getPaletteViewer()));
                } else {
                    menu.appendToGroup(GEFActionConstants.GROUP_COPY, new RemoveFavoriteComponentAction(getPaletteViewer()));
                }
            }
        }
        menu.appendToGroup(GEFActionConstants.GROUP_COPY, new HiddenFloderAction(getPaletteViewer()));
        menu.appendToGroup(GEFActionConstants.GROUP_COPY, new DisplayFloderAction(getPaletteViewer()));
    }

    class SearchComponentAction extends Action {

        private Shell shell;

        private PaletteViewer paletteViewer;

        /**
         * DOC hcw SearchComponentAction constructor comment.
         * 
         * @param paletteViewer
         */
        public SearchComponentAction(PaletteViewer paletteViewer) {
            this.paletteViewer = paletteViewer;
            shell = paletteViewer.getControl().getShell();
            setText(Messages.getString("SearchComponentInJobs.Title")); //$NON-NLS-1$
            setToolTipText(Messages.getString("SearchComponentInJobs.Tooltip")); //$NON-NLS-1$
            setDescription(Messages.getString("SearchComponentInJobs.Tooltip")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            List list = paletteViewer.getSelectedEditParts();
            if (!list.isEmpty()) {
                PaletteEntry selection = (PaletteEntry) ((EditPart) list.get(0)).getModel();
                if (!(selection instanceof PaletteRoot)) {
                    ComponentSearcher searcher = new ComponentSearcher(selection.getLabel(), paletteViewer.getControl()
                            .getShell());
                    searcher.run();
                }
            }
        }

    }

    class FavoriteComponentAction extends Action {

        private Shell shell;

        private PaletteViewer paletteViewer;

        /**
         * DOC hcw SearchComponentAction constructor comment.
         * 
         * @param paletteViewer
         */
        public FavoriteComponentAction(PaletteViewer paletteViewer) {
            this.paletteViewer = paletteViewer;
            shell = paletteViewer.getControl().getShell();
            setText(Messages.getString("TalendPaletteContextMenuProvider.textAdd")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TalendPaletteContextMenuProvider.tipAdd")); //$NON-NLS-1$
            setDescription(Messages.getString("TalendPaletteContextMenuProvider.descriptionAdd")); //$NON-NLS-1$
        }

        @Override
        public void run() {

            List list = paletteViewer.getSelectedEditParts();
            PaletteEntry element = (PaletteEntry) ((EditPart) list.get(0)).getModel();
            Project project = null;
            if (element instanceof TalendPaletteDrawer) {

                List eleList = ((TalendPaletteDrawer) element).getChildren();
                addListNotes(eleList, project);
            } else if (element instanceof CombinedTemplateCreationEntry) {
                addNotes((CombinedTemplateCreationEntry) element, project);

            }

        }

    }

    class RemoveFavoriteComponentAction extends Action {

        private Shell shell;

        private PaletteViewer paletteViewer;

        /**
         * DOC hcw SearchComponentAction constructor comment.
         * 
         * @param paletteViewer
         */
        public RemoveFavoriteComponentAction(PaletteViewer paletteViewer) {
            this.paletteViewer = paletteViewer;
            shell = paletteViewer.getControl().getShell();
            setText(Messages.getString("TalendPaletteContextMenuProvider.textRemove")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TalendPaletteContextMenuProvider.tipRemove")); //$NON-NLS-1$
            setDescription(Messages.getString("TalendPaletteContextMenuProvider.descriptionRemove")); //$NON-NLS-1$
        }

        @Override
        public void run() {

            List list = paletteViewer.getSelectedEditParts();
            PaletteEntry element = (PaletteEntry) ((EditPart) list.get(0)).getModel();
            Project project = null;
            if (element instanceof TalendPaletteDrawer) {

                List eleList = ((TalendPaletteDrawer) element).getChildren();
                removeListNotes(eleList, project);
            } else if (element instanceof CombinedTemplateCreationEntry) {
                removeNotes((CombinedTemplateCreationEntry) element, project);

            }
            ComponentPaletteUtilities.updatePalette(true);
        }

    }

    class HiddenFloderAction extends Action {

        private Shell shell;

        private PaletteViewer paletteViewer;

        /**
         * DOC hcw SearchComponentAction constructor comment.
         * 
         * @param paletteViewer
         */
        public HiddenFloderAction(PaletteViewer paletteViewer) {
            this.paletteViewer = paletteViewer;
            shell = paletteViewer.getControl().getShell();
            setText(Messages.getString("TalendPaletteContextMenuProvider.textHidden")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TalendPaletteContextMenuProvider.tipHidden")); //$NON-NLS-1$
            setDescription(Messages.getString("TalendPaletteContextMenuProvider.descriptionHidden")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            ComponentPaletteUtilities.histate = 1;
            DesignerPlugin.getDefault().getPreferenceStore().setValue("HiddenState", 1); //$NON-NLS-1$
            ComponentPaletteUtilities.updatePalette(!ShowFavoriteAction.state);

        }
    }

    class DisplayFloderAction extends Action {

        private Shell shell;

        private PaletteViewer paletteViewer;

        /**
         * DOC hcw SearchComponentAction constructor comment.
         * 
         * @param paletteViewer
         */
        public DisplayFloderAction(PaletteViewer paletteViewer) {
            this.paletteViewer = paletteViewer;
            shell = paletteViewer.getControl().getShell();
            setText(Messages.getString("TalendPaletteContextMenuProvider.textDisplay")); //$NON-NLS-1$
            setToolTipText(Messages.getString("TalendPaletteContextMenuProvider.tipDisplay")); //$NON-NLS-1$
            setDescription(Messages.getString("TalendPaletteContextMenuProvider.descriptionDisplay")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            ComponentPaletteUtilities.histate = 0;
            DesignerPlugin.getDefault().getPreferenceStore().setValue("HiddenState", 0); //$NON-NLS-1$
            ComponentPaletteUtilities.updatePalette(!ShowFavoriteAction.state);

        }
    }

    public void addNotes(CombinedTemplateCreationEntry element, Project project) {
        String label = element.getLabel();
        String[] fam = null;
        String family = ComponentsFactoryProvider.getPaletteEntryFamily(element.getParent());
        if ("".equals(family) || family == null) {//$NON-NLS-1$
            PaletteComponentFactory paCom = (PaletteComponentFactory) element
                    .getToolProperty(CreationTool.PROPERTY_CREATION_FACTORY);
            fam = paCom.getCombinedFamilyName().split(ComponentsFactoryProvider.FAMILY_SEPARATOR_REGEX);
        }

        RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                Context.REPOSITORY_CONTEXT_KEY);
        project = repositoryContext.getProject();

        Set<IComponent> components = ComponentsFactoryProvider.getInstance().getComponents();
        for (IComponent component : components) {

            if (fam != null) {
                for (int i = 0; i < fam.length; i++) {
                    if (!component.isVisible(fam[i])) {
                        continue;
                    }
                    String famName = null;
                    String familyName[] = null;
                    famName = fam[i];

                    familyName = component.getOriginalFamilyName().split(ComponentsFactoryProvider.FAMILY_SEPARATOR_REGEX);
                    for (String element2 : familyName) {

                        if (component.getName().equals(label) && (element2).equals(famName)) {
                            String key = component.getName() + "#" + famName; //$NON-NLS-1$
                            DesignerPlugin.getDefault().getPreferenceStore().setValue(key, true);
                        }
                    }

                }
            } else {
                if (!component.isVisible(family)) {
                    continue;
                }
                String famName = null;
                String[] familyName = null;

                famName = family;
                familyName = component.getOriginalFamilyName().split(ComponentsFactoryProvider.FAMILY_SEPARATOR_REGEX);
                for (String element2 : familyName) {
                    if (component.getName().equals(label) && (element2).equals(famName)) {
                        String key = component.getName() + "#" + famName; //$NON-NLS-1$
                        DesignerPlugin.getDefault().getPreferenceStore().setValue(key, true);
                    }
                }

            }

        }
    }

    public void addListNotes(List eleList, Project project) {
        for (int i = 0; i < eleList.size(); i++) {
            PaletteEntry elementLi = (PaletteEntry) eleList.get(i);
            if (elementLi instanceof TalendPaletteDrawer) {
                List list = ((TalendPaletteDrawer) elementLi).getChildren();
                addListNotes(list, project);
            } else if (elementLi instanceof CombinedTemplateCreationEntry) {
                addNotes((CombinedTemplateCreationEntry) elementLi, project);
            }

        }
    }

    public void removeNotes(CombinedTemplateCreationEntry element, Project project) {
        String label = element.getLabel();
        String[] fam = null;
        String family = ComponentsFactoryProvider.getPaletteEntryFamily(element.getParent());

        if ("".equals(family) || family == null) {//$NON-NLS-1$
            PaletteComponentFactory paCom = (PaletteComponentFactory) element
                    .getToolProperty(CreationTool.PROPERTY_CREATION_FACTORY);
            fam = paCom.getCombinedFamilyName().split(ComponentsFactoryProvider.FAMILY_SEPARATOR_REGEX);
        }
        RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                Context.REPOSITORY_CONTEXT_KEY);
        project = repositoryContext.getProject();

        Set<IComponent> components = ComponentsFactoryProvider.getInstance().getComponents();

        for (IComponent component : components) {

            if (fam != null) {
                for (int i = 0; i < fam.length; i++) {
                    if (!component.isVisible(fam[i])) {
                        continue;
                    }
                    String famName = null;
                    String[] familyName = null;
                    famName = fam[i];
                    familyName = component.getOriginalFamilyName().split(ComponentsFactoryProvider.FAMILY_SEPARATOR_REGEX);
                    for (String element2 : familyName) {
                        if (component.getName().equals(label) && (element2).equals(famName)) {
                            String key = component.getName() + "#" + famName; //$NON-NLS-1$
                            DesignerPlugin.getDefault().getPreferenceStore().setValue(key, false);
                        }
                    }
                }
            } else {
                if (!component.isVisible(family)) {
                    continue;
                }
                String famName = null;
                String[] familyName = null;

                famName = family;
                familyName = component.getOriginalFamilyName().split(ComponentsFactoryProvider.FAMILY_SEPARATOR_REGEX);
                for (String element2 : familyName) {
                    if (component.getName().equals(label) && (element2).equals(famName)) {
                        String key = component.getName() + "#" + famName; //$NON-NLS-1$
                        DesignerPlugin.getDefault().getPreferenceStore().setValue(key, false);
                    }
                }

            }

        }
    }

    public void removeListNotes(List eleList, Project project) {
        for (int i = 0; i < eleList.size(); i++) {
            PaletteEntry elementLi = (PaletteEntry) eleList.get(i);
            if (elementLi instanceof TalendPaletteDrawer) {
                List list = ((TalendPaletteDrawer) elementLi).getChildren();
                removeListNotes(list, project);
            } else if (elementLi instanceof CombinedTemplateCreationEntry) {
                removeNotes((CombinedTemplateCreationEntry) elementLi, project);
            }

        }
    }
}
