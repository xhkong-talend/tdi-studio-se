// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.model.actions;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.BinRepositoryNode;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.JobletReferenceBean;
import org.talend.repository.model.MetadataTableRepositoryObject;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.IRepositoryNode.EProperties;
import org.talend.repository.ui.actions.DeleteAction;
import org.talend.repository.ui.actions.DeleteActionCache;
import org.talend.repository.ui.actions.metadata.CopyToGenericSchemaHelper;
import org.talend.repository.ui.dialog.JobletReferenceDialog;

/**
 * DOC smallet class global comment. Detailed comment <br/>
 * 
 * $Id$
 * 
 */
public class MoveObjectAction {

    private static Logger log = Logger.getLogger(MoveObjectAction.class);

    private static MoveObjectAction singleton = new MoveObjectAction();

    private boolean isGenericSchema;

    private IPath sourcePath;

    private IPath targetPath;

    public static MoveObjectAction getInstance() {
        return singleton;
    }

    /**
     * DOC Administrator Comment method "validateAction".
     * 
     * @param sourceNode
     * @param targetNode
     * @return
     */
    public boolean validateAction(RepositoryNode sourceNode, RepositoryNode targetNode) {
        if (sourceNode == null) {
            return false;
        }

        IRepositoryViewObject objectToCopy = sourceNode.getObject();

        // Cannot move system routines:
        // if (objectToCopy != null && objectToCopy.getType() == ERepositoryObjectType.ROUTINES) {
        // Property property = objectToCopy.getProperty();
        // RoutineItem item = (RoutineItem) property.getItem();
        // return !item.isBuiltIn();
        // }

        // Cannot move system sql pattern:
        // if (objectToCopy != null && objectToCopy.getType() == ERepositoryObjectType.SQLPATTERNS) {
        // Property property = objectToCopy.getProperty();
        // SQLPatternItem item = (SQLPatternItem) property.getItem();
        // return !item.isSystem();
        // }

        // cannot move job html documentation node:
        // if (objectToCopy != null && objectToCopy.getType() == ERepositoryObjectType.JOB_DOC) {
        // return false;
        // }

        // Cannot move folder in job documentation node:
        if (sourceNode.getType() == ENodeType.SIMPLE_FOLDER && sourceNode.getContentType() == ERepositoryObjectType.JOB_DOC) {
            return false;
        }

        // cannot move html in joblet documentation node:
        // if (objectToCopy != null && objectToCopy.getType() == ERepositoryObjectType.JOBLET_DOC) {
        // return false;
        // }

        // Cannot move folder in joblet documentation node:
        if (sourceNode.getType() == ENodeType.SIMPLE_FOLDER && sourceNode.getContentType() == ERepositoryObjectType.JOBLET_DOC) {
            return false;
        }

        // can't drag mdm connection
        if (sourceNode.getObjectType() == ERepositoryObjectType.METADATA_MDMCONNECTION) {
            return false;
        }

        if (targetNode == null) {
            switch (sourceNode.getType()) {
            case REPOSITORY_ELEMENT:
            case SIMPLE_FOLDER:
                return true;
            default:
                return false;
            }
        }
        if (sourceNode.equals(targetNode)) {
            return false;
        }

        sourcePath = RepositoryNodeUtilities.getPath(sourceNode);
        // IPath targetPath = RepositoryNodeUtilities.getTargetPath(targetNode);
        targetPath = RepositoryNodeUtilities.getPath(targetNode);
        if (sourceNode.getType() == ENodeType.REPOSITORY_ELEMENT) {
            isGenericSchema = targetNode.getContentType() == ERepositoryObjectType.METADATA_GENERIC_SCHEMA
                    && sourceNode.getProperties(EProperties.CONTENT_TYPE) != ERepositoryObjectType.METADATA_GENERIC_SCHEMA
                    && (sourceNode.getObject() instanceof MetadataTableRepositoryObject);

            if (!isGenericSchema && !ResourceUtils.isCorrectDestination(sourcePath, targetPath, false)) {
                return false;
            }

            switch (targetNode.getType()) {
            case SYSTEM_FOLDER:
            case SIMPLE_FOLDER:
                boolean booleanValue = ((ERepositoryObjectType) targetNode.getProperties(EProperties.CONTENT_TYPE)) == objectToCopy
                        .getRepositoryObjectType();
                if (isGenericSchema) {
                    return true;
                } else {
                    return booleanValue;
                }
            case STABLE_SYSTEM_FOLDER:
                // see bug remove a joblet haven't the same behavior when move it into the recycle with the mouse or use
                // delete
                boolean isJoblet = sourceNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.JOBLET;
                boolean isBin = targetNode instanceof BinRepositoryNode;
                if (isBin && isJoblet) {
                    IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
                    List<JobletReferenceBean> checkRepository = DeleteAction.checkRepositoryNodeFromProcess(factory, null,
                            sourceNode);
                    DeleteActionCache.getInstance().clearRecords();
                    if (checkRepository.size() > 0) {
                        JobletReferenceDialog dialog = new JobletReferenceDialog(PlatformUI.getWorkbench()
                                .getActiveWorkbenchWindow().getShell(), sourceNode.getObject(), checkRepository);
                        dialog.open();
                        return false;
                    }
                }
                return isBin;// || isGenericSchema;
            default:
                return false;
            }

        } else if (sourceNode.getType() == ENodeType.SIMPLE_FOLDER) {
            if (targetNode.getType() != ENodeType.SIMPLE_FOLDER && targetNode.getType() != ENodeType.SYSTEM_FOLDER) {
                return false;
            }

            ERepositoryObjectType sourceType = (ERepositoryObjectType) sourceNode.getProperties(EProperties.CONTENT_TYPE);
            if (((ERepositoryObjectType) targetNode.getProperties(EProperties.CONTENT_TYPE)) != sourceType) {
                return false;
            }

            if (!ResourceUtils.isCorrectDestination(sourcePath, targetPath, true)) {
                return false;
            }

            IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
            try {
                if (!factory.isPathValid(sourceType, targetPath, sourcePath.lastSegment())) {
                    return false;
                }
            } catch (PersistenceException e) {
                ExceptionHandler.process(e);
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    public void execute(RepositoryNode sourceNode, RepositoryNode targetNode) throws Exception {
        execute(sourceNode, targetNode, null);
    }

    // folderPath is used for restore item to original folder. see bug 0005465: Restore from Recycle Bin lose the
    // directories.
    public void execute(RepositoryNode sourceNode, RepositoryNode targetNode, IPath folderPath) throws Exception {
        if (!validateAction(sourceNode, targetNode)) {
            // i18n
            // log.debug("Cannot move [" + sourceNode + "] to " + targetNode);
            String str[] = new String[] { sourceNode.toString(), targetNode.toString() };
            log.debug(Messages.getString("MoveObjectAction.0", str)); //$NON-NLS-1$
            return;
        }

        if (folderPath != null) {
            targetPath = folderPath;
        } else {
            targetPath = (targetNode == null ? new Path("") : RepositoryNodeUtilities.getPath(targetNode)); //$NON-NLS-1$
        }
        sourcePath = RepositoryNodeUtilities.getPath(sourceNode);

        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();

        if (sourceNode.getType().equals(ENodeType.REPOSITORY_ELEMENT)) {
            // Source is an repository element :
            IRepositoryViewObject objectToMove = sourceNode.getObject();

            if (targetNode instanceof BinRepositoryNode) {
                // Move in the recycle bin :
                factory.deleteObjectLogical(objectToMove);
            } else {
                if (factory.getStatus(objectToMove) == ERepositoryStatus.DELETED) {
                    // Restore :
                    factory.restoreObject(objectToMove, targetPath);
                    // if object is opened and editable, will re-lock it.
                    if (RepositoryManager.isEditableItemInEditor(objectToMove)) {
                        factory.lock(objectToMove);
                    }
                } else {
                    // Move :
                    if (isGenericSchema) {
                        CopyToGenericSchemaHelper.copyToGenericSchema(factory, objectToMove, targetPath);
                    } else {
                        factory.moveObject(objectToMove, targetPath, sourcePath);
                    }

                }
            }
        } else if (sourceNode.getType().equals(ENodeType.SIMPLE_FOLDER)) {
            // Source is a folder :
            ERepositoryObjectType sourceType = (ERepositoryObjectType) sourceNode.getProperties(EProperties.CONTENT_TYPE);
            factory.moveFolder(sourceType, sourcePath, targetPath);
        }
    }

}
