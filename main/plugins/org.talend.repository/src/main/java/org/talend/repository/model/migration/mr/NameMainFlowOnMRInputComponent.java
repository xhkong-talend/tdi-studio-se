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

package org.talend.repository.model.migration.mr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.components.ModifyComponentsAction;
import org.talend.core.model.components.conversions.IComponentConversion;
import org.talend.core.model.components.filters.NameComponentFilter;
import org.talend.core.model.migration.AbstractJobMigrationTask;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.designer.core.model.utils.emf.talendfile.impl.MetadataTypeImpl;

/**
 * This migration task add the die on error option on the MapReduce tInput components and will rename their main flow to
 * "MAIN". This will allow them to have a "ERROR" flow.
 */

public class NameMainFlowOnMRInputComponent extends AbstractJobMigrationTask {

    /**
     * Override the getTypes fonction in order to process the MapReduce components. (The default one will exclude them.)
     */
    @Override
    public List<ERepositoryObjectType> getTypes() {
        List<ERepositoryObjectType> toReturn = new ArrayList<ERepositoryObjectType>();
        ERepositoryObjectType type = ERepositoryObjectType.getType("PROCESS_MR"); //$NON-NLS-1$
        if (type != null) {
            toReturn.add(type);
        }
        return toReturn;
    }

    @Override
    public ExecutionResult execute(Item item) {
        ProcessType processType = getProcessType(item);
        if (getProject().getLanguage() != ECodeLanguage.JAVA || processType == null) {
            return ExecutionResult.NOTHING_TO_DO;
        }
        try {
            IComponentConversion addFailOnErrorOption = new AddFailOnErrorOptionConversion();
            NameMainFlow nameMainFlow = new NameMainFlow();
            ModifyComponentsAction.searchAndModify(item, processType, new NameComponentFilter("tHDFSInput"), //$NON-NLS-1$ 
                    Arrays.asList(addFailOnErrorOption, nameMainFlow));

            ModifyComponentsAction.searchAndModify(item, processType, new NameComponentFilter("tJDBCInput"), //$NON-NLS-1$ 
                    Arrays.asList(addFailOnErrorOption, nameMainFlow));

            ModifyComponentsAction.searchAndModify(item, processType, new NameComponentFilter("tAvroInput"), //$NON-NLS-1$ 
                    Arrays.asList(addFailOnErrorOption, nameMainFlow));

            ModifyComponentsAction.searchAndModify(item, processType, new NameComponentFilter("tHBaseInput"), //$NON-NLS-1$ 
                    Arrays.asList(addFailOnErrorOption, nameMainFlow));

            ModifyComponentsAction.searchAndModify(item, processType, new NameComponentFilter("tFileInputJSON"), //$NON-NLS-1$ 
                    Arrays.asList(addFailOnErrorOption, nameMainFlow));

            return ExecutionResult.SUCCESS_NO_ALERT;
        } catch (Exception e) {
            ExceptionHandler.process(e);
            return ExecutionResult.FAILURE;
        }
    }

    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2014, 8, 28, 15, 00, 00);
        return gc.getTime();
    }

    /**
     * DOC pbailly AddFailOnErrorOptionConversion
     * 
     * Check fail on error option by default on the job
     */
    private class AddFailOnErrorOptionConversion implements IComponentConversion {

        private String field = "CHECK"; //$NON-NLS-1$

        private String name = "DIE_ON_ERROR"; //$NON-NLS-1$

        public AddFailOnErrorOptionConversion() {
            super();
        }

        @Override
        public void transform(NodeType node) {
            if (ComponentUtilities.getNodeProperty(node, name) == null) {
                ComponentUtilities.addNodeProperty(node, name, field);
                ComponentUtilities.setNodeValue(node, name, "true"); //$NON-NLS-1$
            }
        }
    }

    /**
     * DOC pbailly AddFailOnErrorOptionConversion
     * 
     * Change the name of the only connector to Main
     */
    private class NameMainFlow implements IComponentConversion {

        public NameMainFlow() {
            super();
        }

        @Override
        public void transform(NodeType node) {
            EObjectContainmentEList<MetadataTypeImpl> metadata = (EObjectContainmentEList<MetadataTypeImpl>) node.getMetadata();
            if (metadata.get(0) != null) {
                metadata.get(0).setConnector("MAIN"); //$NON-NLS-1$
            }
        }
    }

}
