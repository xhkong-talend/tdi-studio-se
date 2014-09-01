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
package org.talend.designer.core.ui.editor.cmd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.components.IODataComponent;
import org.talend.core.model.components.IODataComponentContainer;
import org.talend.core.model.metadata.ColumnNameChanged;
import org.talend.core.model.metadata.Dbms;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataTalendType;
import org.talend.core.model.metadata.MetadataToolHelper;
import org.talend.core.model.metadata.builder.connection.MDMConnection;
import org.talend.core.model.metadata.designerproperties.RepositoryToComponentProperty;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.core.service.IDbMapService;
import org.talend.core.service.IXmlMapService;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.editor.properties.controllers.ColumnListController;

/**
 * Command that will change a metadata in a node.
 * 
 * $Id$
 * 
 */
/**
 * DOC Administrator class global comment. Detailled comment <br/>
 * 
 */
public class ChangeMetadataCommand extends Command {

    private INode node, inputNode;

    protected IMetadataTable currentOutputMetadata, newOutputMetadata, oldOutputMetadata;

    private boolean outputWasRepository = false, inputWasRepository = false;

    private IMetadataTable currentInputMetadata, newInputMetadata, oldInputMetadata;

    private IODataComponentContainer inputdataContainer;

    private IODataComponentContainer outputdataContainer;

    private IODataComponent dataComponent;

    private Boolean propagate;

    private final List<ChangeMetadataCommand> propagatedChange = new ArrayList<ChangeMetadataCommand>();

    private boolean internal = false;

    private boolean repositoryMode = false;

    private IElementParameter schemaParam;

    private IElementParameter inputSchemaParam;

    private String currentConnector;

    protected boolean isNotSim = false;

    // Default constructor.
    public ChangeMetadataCommand() {
    }

    public ChangeMetadataCommand(INode node, IElementParameter schemaParam, INode inputNode, IMetadataTable currentInputMetadata,
            IMetadataTable newInputMetadata, IMetadataTable currentOutputMetadata, IMetadataTable newOutputMetadata) {
        this.node = node;
        this.inputNode = inputNode;
        this.schemaParam = schemaParam;
        if (schemaParam == null) {
            currentConnector = EConnectionType.FLOW_MAIN.getName();
            for (IElementParameter param : node.getElementParameters()) {
                if ((param.getFieldType().equals(EParameterFieldType.SCHEMA_TYPE) || param.getFieldType().equals(
                        EParameterFieldType.DCSCHEMA))
                        && param.getContext().equals(currentConnector)) {
                    this.schemaParam = param;
                }
            }
        } else {
            currentConnector = this.schemaParam.getContext();
        }
        this.currentInputMetadata = currentInputMetadata;
        if (currentInputMetadata != null) {
            oldInputMetadata = currentInputMetadata.clone();
        } else {
            oldInputMetadata = null;
        }
        this.newInputMetadata = newInputMetadata;
        this.currentOutputMetadata = currentOutputMetadata;
        if (this.currentOutputMetadata == null) {
            this.currentOutputMetadata = node.getMetadataFromConnector(currentConnector);
        }
        oldOutputMetadata = this.currentOutputMetadata.clone();
        this.newOutputMetadata = newOutputMetadata;
        initializeContainer();
        setLabel(Messages.getString("ChangeMetadataCommand.changeMetadataValues")); //$NON-NLS-1$
    }

    public ChangeMetadataCommand(INode node, IElementParameter schemaParam, IMetadataTable currentOutputMetadata,
            IMetadataTable newOutputMetadata, IElementParameter inputSchemaParam) {
        this.inputSchemaParam = inputSchemaParam;
        init(node, schemaParam, currentOutputMetadata, newOutputMetadata);
    }

    public ChangeMetadataCommand(INode node, IElementParameter schemaParam, IMetadataTable currentOutputMetadata,
            IMetadataTable newOutputMetadata) {
        init(node, schemaParam, currentOutputMetadata, newOutputMetadata);
    }

    public void init(INode node, IElementParameter schemaParam, IMetadataTable currentOutputMetadata,
            IMetadataTable newOutputMetadata) {
        this.node = node;
        this.schemaParam = schemaParam;
        if (schemaParam == null) {
            if (newOutputMetadata.getAttachedConnector() != null) {
                currentConnector = newOutputMetadata.getAttachedConnector();
            } else {
                if (node.isELTComponent()) {
                    currentConnector = EConnectionType.TABLE.getName();
                } else {
                    currentConnector = EConnectionType.FLOW_MAIN.getName();
                }
            }
            for (IElementParameter param : node.getElementParameters()) {
                if (param.getFieldType().equals(EParameterFieldType.SCHEMA_TYPE) && param.getContext().equals(currentConnector)) {
                    this.schemaParam = param;
                }
            }
        } else {
            currentConnector = this.schemaParam.getContext();
        }

        this.inputNode = null;
        this.currentInputMetadata = null;
        this.newInputMetadata = null;
        oldInputMetadata = null;
        this.currentOutputMetadata = currentOutputMetadata;
        if (this.currentOutputMetadata == null) {
            this.currentOutputMetadata = node.getMetadataFromConnector(currentConnector);
        }
        if (currentOutputMetadata == null && newOutputMetadata != null) {
            currentOutputMetadata = newOutputMetadata.clone(true);
        }
        oldOutputMetadata = this.currentOutputMetadata.clone(true);
        this.newOutputMetadata = newOutputMetadata.clone(true);
        this.newOutputMetadata.setReadOnly(this.currentOutputMetadata.isReadOnly());
        initializeContainer();
        setLabel(Messages.getString("ChangeMetadataCommand.changeMetadataValues")); //$NON-NLS-1$
    }

    public void setRepositoryMode(boolean repositoryMode) {
        this.repositoryMode = repositoryMode;
    }

    private void initializeContainer() {
        outputdataContainer = new IODataComponentContainer();
        for (Connection connec : (List<Connection>) node.getIncomingConnections()) {
            if (connec.getLineStyle().equals(EConnectionType.FLOW_MAIN)) {
                IODataComponent input = null;
                if (newInputMetadata == null) {
                    input = new IODataComponent(connec);
                } else {
                    if (connec.getMetaName().equals(newInputMetadata.getTableName())) {
                        input = new IODataComponent(connec, newInputMetadata);
                    }
                }
                if (input != null) {
                    outputdataContainer.getInputs().add(input);
                }

            }
        }
        for (Connection connec : (List<Connection>) node.getOutgoingConnections()) {
            if (connec.getLineStyle().equals(EConnectionType.FLOW_MAIN) || isinputContainerOutput(connec)
                    || ((connec.getLineStyle().equals(EConnectionType.FLOW_MERGE) && (connec.getInputId() == 1)))) {
                if ((!connec.getSource().getConnectorFromType(connec.getLineStyle()).isMultiSchema())
                        || (connec.getMetaName().equals(newOutputMetadata.getTableName()))) {
                    IODataComponent output = new IODataComponent(connec, newOutputMetadata);
                    outputdataContainer.getOuputs().add(output);
                }
            }
        }

        if (inputNode != null) {
            inputdataContainer = new IODataComponentContainer();
            for (Connection connec : (List<Connection>) inputNode.getOutgoingConnections()) {
                if (connec.getTarget().equals(node)) {
                    if ((!connec.getSource().getConnectorFromType(connec.getLineStyle()).isMultiSchema())
                            || (connec.getMetaName().equals(newInputMetadata.getTableName()))) {
                        IODataComponent output = new IODataComponent(connec, newInputMetadata);
                        inputdataContainer.getOuputs().add(output);
                    }
                }
            }
        }
    }

    private boolean isinputContainerOutput(Connection connec) {
        boolean isREF = connec.getLineStyle().equals(EConnectionType.FLOW_REF);
        if (isREF && (connec.getTarget().getOutgoingConnections().size() == 1)) {
            if (connec.getTarget().getOutgoingConnections().get(0).getLineStyle().equals(EConnectionType.FLOW_REF)) {
                return true;
            }
        }
        return false;
    }

    private void setInternal(boolean internal) {
        this.internal = internal;
    }

    private boolean getPropagate(Boolean returnIfNull) {
        if (propagate == null) {
            if (returnIfNull != null) {
                return returnIfNull;
            }
            propagate = MessageDialog.openQuestion(new Shell(),
                    Messages.getString("ChangeMetadataCommand.messageDialog.propagate"), //$NON-NLS-1$
                    Messages.getString("ChangeMetadataCommand.messageDialog.questionMessage")); //$NON-NLS-1$ 
        }
        return propagate;
    }

    private boolean getPropagate() {
        return getPropagate(null);
    }

    @SuppressWarnings("unchecked")
    protected void updateColumnList(IMetadataTable oldTable, IMetadataTable newTable) {
        final List<ColumnNameChanged> columnNameChanged = MetadataToolHelper.getColumnNameChanged(oldTable, newTable);

        if (inputNode != null) {
            List<IElementParameter> eps = (List<IElementParameter>) inputNode.getElementParameters();
            if (eps != null) {
                boolean end = false;
                for (int i = 0; i < eps.size() && !end; i++) {
                    IElementParameter parameter = eps.get(i);
                    if (parameter.getFieldType() == EParameterFieldType.TABLE) {
                        end = true;
                        if (parameter != null) {
                            List<Map<String, Object>> map2 = (List<Map<String, Object>>) parameter.getValue();
                            if (map2 != null && inputNode.getMetadataList().get(0).getListColumns().size() != map2.size()) {
                                ColumnListController.updateColumnList(inputNode, columnNameChanged);
                            }
                        }
                    }
                }
            }
            inputNode.setPropertyValue(EParameterName.UPDATE_COMPONENTS.getName(), Boolean.TRUE);
        }
        node.setPropertyValue(EParameterName.UPDATE_COMPONENTS.getName(), Boolean.TRUE);
    }

    public void execute(Boolean propagateP) {
        this.propagate = propagateP;
        if (currentOutputMetadata == null) {
            currentOutputMetadata = node.getMetadataFromConnector(currentConnector);
        }
        setInternal(true);
        execute();
    }

    private void propagateDatas(boolean isExecute) {
        String baseConnectorForCurrentNode = node.getConnectorFromName(currentConnector).getBaseSchema();

        // Propagate :
        if (outputdataContainer != null
                && (!outputdataContainer.getInputs().isEmpty() || !outputdataContainer.getOuputs().isEmpty())) {
            for (IODataComponent currentIO : outputdataContainer.getInputs()) {
                INode sourceNode = currentIO.getSource();
                if (currentIO.hasChanged()
                        && (sourceNode.getConnectorFromName(currentIO.getConnection().getConnectorName()).getBaseSchema()
                                .equals(baseConnectorForCurrentNode))) {
                    sourceNode.metadataOutputChanged(currentIO, currentIO.getName());
                    if (isExecute) {
                        currentIO.setTable(oldInputMetadata);
                        currentIO.setColumnNameChanged(null);
                    } else {
                        currentIO.setTable(newInputMetadata);
                        currentIO.setColumnNameChanged(null);
                    }
                }
            }
            for (IODataComponent currentIO : outputdataContainer.getOuputs()) {
                INodeConnector nodeConnector = null;
                String baseConnector = null;

                Node sourceNode = (Node) currentIO.getSource();
                nodeConnector = sourceNode.getConnectorFromName(currentIO.getConnection().getConnectorName());
                baseConnector = nodeConnector.getBaseSchema();

                INode targetNode = currentIO.getTarget();

                boolean sourceIsBuiltIn = ((Node) currentIO.getSource()).getConnectorFromType(
                        currentIO.getConnection().getLineStyle()).isMultiSchema();

                boolean targetIsBuiltIn = ((Node) targetNode).getConnectorFromType(currentIO.getConnection().getLineStyle())
                        .isMultiSchema();
                boolean isJoblet = ((Node) targetNode).isJoblet();
                if (!isJoblet
                        && baseConnector.equals(baseConnectorForCurrentNode)
                        && (targetIsBuiltIn || (targetNode.getMetadataFromConnector(baseConnector) != null && !targetNode
                                .getMetadataFromConnector(baseConnector).sameMetadataAs(newOutputMetadata)))) {

                    targetNode.metadataInputChanged(currentIO, currentIO.getUniqueName());
                    if (isExecute) {
                        if (targetNode instanceof Node) {
                            if (((Node) targetNode).getComponent().isSchemaAutoPropagated() && getPropagate()
                                    && targetNode.getMetadataList().size() > 0) {
                                IMetadataTable tmpClone;
                                if (sourceIsBuiltIn) {
                                    IMetadataTable tab = node.getMetadataTable(currentIO.getConnection().getMetadataTable()
                                            .getTableName());
                                    if (tab == null && node.getJobletNode() != null) {
                                        tab = node.getJobletNode().getMetadataTable(
                                                currentIO.getConnection().getMetadataTable().getTableName());
                                    }
                                    tmpClone = tab.clone(true);
                                } else {
                                    IMetadataTable tab = node.getMetadataFromConnector(currentIO.getConnection()
                                            .getConnectorName());
                                    if (tab == null && node.getJobletNode() != null) {
                                        tab = node.getJobletNode().getMetadataFromConnector(
                                                currentIO.getConnection().getConnectorName());
                                    }
                                    tmpClone = tab.clone(true);
                                }
                                IMetadataTable toCopy = newOutputMetadata.clone();

                                // wzhang modify to add feature 7611

                                String dbmsId = null;
                                IMetadataTable copy;
                                if (((Node) targetNode).getMetadataFromConnector(baseConnector) != null) {
                                    dbmsId = targetNode.getMetadataFromConnector(baseConnector).getDbms();
                                    MetadataToolHelper.copyTable(dbmsId, toCopy, tmpClone);
                                    toCopy = tmpClone;

                                    // only if the target node have exactly the same connector
                                    copy = ((Node) targetNode).getMetadataFromConnector(baseConnector).clone(true);
                                } else {
                                    final String mainConnector = "FLOW"; // can only be FLOW right now for this case. //$NON-NLS-1$

                                    dbmsId = targetNode.getMetadataFromConnector(mainConnector).getDbms();
                                    MetadataToolHelper.copyTable(dbmsId, toCopy, tmpClone);
                                    toCopy = tmpClone;
                                    // if don't have the same connector, take the main connector of the component.

                                    copy = ((Node) targetNode).getMetadataFromConnector(mainConnector).clone(true);
                                }
                                // MetadataTool.copyTable(toCopy, copy);
                                // wzhang modify to add feature 7611
                                MetadataToolHelper.copyTable(dbmsId, toCopy, copy);
                                ChangeMetadataCommand cmd = new ChangeMetadataCommand(targetNode, null, null, copy,
                                        inputSchemaParam);
                                if (outputdataContainer.getOuputs().size() > 0) {
                                    List<ColumnNameChanged> columnNameChanged = outputdataContainer.getOuputs().get(0)
                                            .getColumnNameChanged();
                                    for (IODataComponent dataComp : cmd.outputdataContainer.getOuputs()) {
                                        dataComp.setColumnNameChanged(columnNameChanged);
                                    }
                                }
                                cmd.execute(true);
                                propagatedChange.add(cmd);
                            }
                        }
                        currentIO.setTable(oldOutputMetadata);
                        currentIO.setColumnNameChanged(null);
                    } else {
                        if (targetNode instanceof Node) {
                            if (!targetIsBuiltIn && getPropagate()) {
                                if (((Node) targetNode).getComponent().isSchemaAutoPropagated()) {
                                    if (outputdataContainer.getOuputs().size() > 0) {
                                        List<ColumnNameChanged> columnNameChanged = outputdataContainer.getOuputs().get(0)
                                                .getColumnNameChanged();
                                        for (ChangeMetadataCommand cmd : propagatedChange) {
                                            for (IODataComponent dataComp : cmd.outputdataContainer.getOuputs()) {
                                                dataComp.setColumnNameChanged(columnNameChanged);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        currentIO.setTable(newOutputMetadata);
                        currentIO.setColumnNameChanged(null);
                    }
                }
            }
        } else if (dataComponent != null) {
            for (IConnection outgoingConnection : node.getOutgoingConnections()) {
                if (outgoingConnection.getConnectorName().equals(currentConnector)) {
                    outgoingConnection.getTarget().metadataInputChanged(dataComponent, outgoingConnection.getName());
                }
            }
        } else {
            if (!node.getOutgoingConnections().isEmpty()) {
                for (IConnection outgoingConnection : node.getOutgoingConnections()) {
                    final Node target = (Node) outgoingConnection.getTarget();
                    if (target != null && target.getExternalNode() != null) {
                        List<IMetadataColumn> oldListColumns = oldOutputMetadata.getListColumns();
                        List<IMetadataColumn> newListColumns = newOutputMetadata.getListColumns();
                        List<ColumnNameChanged> columnNameChanges = new ArrayList<ColumnNameChanged>();
                        int size = oldListColumns.size();
                        int newSize = newListColumns.size();
                        if (newSize < size) {
                            size = newSize;
                        }

                        for (int i = 0; i < size; i++) {
                            IMetadataColumn metadataColumn = oldListColumns.get(i);
                            IMetadataColumn newMetadataColumn = newListColumns.get(i);
                            if (metadataColumn != null && newMetadataColumn != null) {
                                String oldLabel = metadataColumn.getLabel();
                                String newLabel = newMetadataColumn.getLabel();
                                if (oldLabel != null && !oldLabel.equals(newLabel)) {
                                    columnNameChanges.add(new ColumnNameChanged(oldLabel, newLabel));
                                }
                            }
                        }
                        if (GlobalServiceRegister.getDefault().isServiceRegistered(IXmlMapService.class)) {
                            final IXmlMapService service = (IXmlMapService) GlobalServiceRegister.getDefault().getService(
                                    IXmlMapService.class);
                            if (service.isXmlMapComponent(target.getExternalNode())) {
                                IODataComponent output = new IODataComponent(outgoingConnection, newOutputMetadata);
                                output.setColumnNameChanged(columnNameChanges);
                                target.metadataInputChanged(output, outgoingConnection.getName());
                            }
                        }

                        if (GlobalServiceRegister.getDefault().isServiceRegistered(IDbMapService.class)) {
                            final IDbMapService service = (IDbMapService) GlobalServiceRegister.getDefault().getService(
                                    IDbMapService.class);
                            if (service.isDbMapComponent(target.getExternalNode())) {
                                IODataComponent output = new IODataComponent(outgoingConnection, newOutputMetadata);
                                // TDI-25307:should setColumNameChanged here for ELtDbMap in case the propagate schema
                                // does not affect it.
                                output.setColumnNameChanged(columnNameChanges);
                                target.metadataInputChanged(output, outgoingConnection.getName());
                            }
                        }
                    }
                }
            }
        }

        if (inputdataContainer != null) {
            for (IODataComponent currentIO : inputdataContainer.getOuputs()) {
                if (currentIO.hasChanged()
                        && (currentIO.getSource().getConnectorFromName(currentIO.getConnection().getConnectorName())
                                .getBaseSchema().equals(currentConnector))) {
                    INode targetNode = currentIO.getTarget();
                    targetNode.metadataInputChanged(currentIO, currentIO.getUniqueName());
                    if (isExecute) {
                        currentIO.setTable(oldInputMetadata);
                        currentIO.setColumnNameChanged(null);
                    } else {
                        currentIO.setTable(newInputMetadata);
                        currentIO.setColumnNameChanged(null);
                    }
                }
            }
        }
        // End propagate
    }

    @Override
    public void execute() {
        propagatedChange.clear();

        propagateDatas(true);
        if (currentInputMetadata != null) {
            if (!currentInputMetadata.sameMetadataAs(newInputMetadata, IMetadataColumn.OPTIONS_NONE)) {
                if (!currentInputMetadata.sameMetadataAs(newInputMetadata, IMetadataColumn.OPTIONS_IGNORE_USED)) {
                    String type = (String) inputNode.getPropertyValue(EParameterName.SCHEMA_TYPE.getName());
                    if (type != null) {
                        if (type.equals(EmfComponent.REPOSITORY)) {
                            inputWasRepository = true;
                            inputNode.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.BUILTIN);
                        }
                    }
                }
                MetadataToolHelper.copyTable(newInputMetadata, currentInputMetadata);
            }
        }

        if (!currentOutputMetadata.sameMetadataAs(newOutputMetadata, IMetadataColumn.OPTIONS_NONE)) {
            if (!currentOutputMetadata.sameMetadataAs(newOutputMetadata, IMetadataColumn.OPTIONS_IGNORE_USED)) {
                String type = (String) node.getPropertyValue(EParameterName.SCHEMA_TYPE.getName());
                if (type != null && type.equals(EmfComponent.REPOSITORY) && !repositoryMode) {
                    // for one node has several schema_type,set mode for the current one
                    if ((node.getElementParameter("SCHEMA_TYPE").getContext()
                            .equals(currentOutputMetadata.getAttachedConnector()))) {
                        outputWasRepository = true;
                        node.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.BUILTIN);
                    }
                }
            }
            MetadataToolHelper.copyTable(newOutputMetadata, currentOutputMetadata);
        }
        if (inputSchemaParam != null
                && inputSchemaParam.getChildParameters().get(EParameterName.SCHEMA_TYPE.getName()) != null
                && EmfComponent.REPOSITORY.equals(inputSchemaParam.getChildParameters().get(EParameterName.SCHEMA_TYPE.getName())
                        .getValue())) {
            // add by wzhang to fix bug 7898.
            IElementParameter elementParameter = node.getElementParameter(EParameterName.MAPPING.getName());
            if (elementParameter != null) {
                if (elementParameter.getValue() instanceof String) {
                    String value = (String) elementParameter.getValue();
                    if (!isDBComponent(value)) {
                        schemaParam.getChildParameters().get(EParameterName.SCHEMA_TYPE.getName())
                                .setValue(EmfComponent.REPOSITORY);
                        schemaParam
                                .getChildParameters()
                                .get(EParameterName.REPOSITORY_SCHEMA_TYPE.getName())
                                .setValue(
                                        inputSchemaParam.getChildParameters()
                                                .get(EParameterName.REPOSITORY_SCHEMA_TYPE.getName()).getValue());
                    }
                }
            }
        }

        for (INodeConnector connector : node.getListConnector()) {
            if ((!connector.getName().equals(currentConnector)) && connector.getBaseSchema().equals(currentConnector)) {
                if (node.getComponent() != null && "tSalesforceOutput".equals(node.getComponent().getName())
                        && "REJECT".equals(connector.getName())) {
                    IMetadataTable clone = newOutputMetadata.clone(true);
                    Iterator<IMetadataColumn> iterator = clone.getListColumns().iterator();
                    while (iterator.hasNext()) {
                        IMetadataColumn column = iterator.next();
                        if (column.isCustom()) {
                            iterator.remove();
                        }
                    }
                    MetadataToolHelper.copyTable(clone, node.getMetadataFromConnector(connector.getName()));
                } else {
                    // if there is some other schema dependant of this one, modify them
                    MetadataToolHelper.copyTable(newOutputMetadata, node.getMetadataFromConnector(connector.getName()));
                }
            }
        }

        List<ColumnNameChanged> columnNameChanged = MetadataToolHelper.getColumnNameChanged(oldOutputMetadata, newOutputMetadata);
        ColumnListController.updateColumnList(node, columnNameChanged, true);

        if (inputNode != null) {
            List<ColumnNameChanged> inputColumnNameChangedExt = MetadataToolHelper.getColumnNameChangedExt(inputNode,
                    oldInputMetadata, newInputMetadata);
            ColumnListController.updateColumnList(node, inputColumnNameChangedExt);
        }
        //
        List<ColumnNameChanged> outputColumnNameChangedExt = MetadataToolHelper.getColumnNameChangedExt(node, oldOutputMetadata,
                newOutputMetadata);
        syncOutputNodeColumnsList(outputColumnNameChangedExt);

        setXMLMAPPING();

        if (!internal) {
            updateColumnList(oldOutputMetadata, newOutputMetadata);
            ((Process) node.getProcess()).checkProcess();
        }
        refreshTHMAPObjectType();
    }

    private org.talend.core.model.metadata.builder.connection.Connection connection;

    /**
     * wzhang Comment method "isDBComponent".
     */
    private boolean isDBComponent(String dbmsId) {
        if (dbmsId != null) {
            try {
                Dbms dbms = MetadataTalendType.getDbms(dbmsId);
                if (dbms != null) {
                    return true;
                }
            } catch (Exception e) {
                // nothing to do
            }
        }
        return false;
    }

    /*
     * use to synchronize column list for output connections.
     */
    private void syncOutputNodeColumnsList(List<ColumnNameChanged> columnNameChanged) {
        if (outputdataContainer == null) {
            return;
        }
        for (IConnection conn : node.getOutgoingConnections()) {
            INode targetNode = conn.getTarget();
            EConnectionType connStyle = conn.getLineStyle();
            if (EConnectionType.FLOW_MAIN.equals(connStyle) || EConnectionType.FLOW_MERGE.equals(connStyle)
                    || EConnectionType.FLOW_REF.equals(connStyle)) {
                ColumnListController.updateColumnList(targetNode, columnNameChanged);
                // fix for TDI-23202
                // reset value in order to call "firePropertyChange(RETURNS_CHANGED, null, null)"
                IElementParameter defaultMap = targetNode.getElementParameter(EParameterName.DEFAULT_MAP.getName());
                if (defaultMap != null) {
                    if ("tFlowToIterate".equals(targetNode.getComponent().getName())) {
                        // update target properties incase any old columns are removed
                        IElementParameter elementParameter = targetNode.getElementParameter("MAP");
                        if (elementParameter != null) {
                            Object value = elementParameter.getValue();
                            if (value instanceof List) {
                                for (Object obj : (List) value) {
                                    if (obj instanceof Map) {
                                        Object object = ((Map) obj).get("VALUE");
                                        if (newOutputMetadata != null && !newOutputMetadata.getListColumns().isEmpty()) {
                                            boolean found = false;
                                            for (IMetadataColumn column : newOutputMetadata.getListColumns()) {
                                                if (column.getLabel().equals(object)) {
                                                    found = true;
                                                }
                                            }
                                            if (!found) {
                                                ((Map) obj).put("VALUE", newOutputMetadata.getListColumns().get(0).getLabel());
                                            }
                                        } else {
                                            ((Map) obj).put("VALUE", "");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    targetNode.setPropertyValue(EParameterName.DEFAULT_MAP.getName(), defaultMap.getValue());
                }
            }
        }

    }

    /**
     * qzhang Comment method "setXMLMAPPING".
     */
    protected void setXMLMAPPING() {
        if (getConnection() != null) {
            for (IElementParameter parameter : node.getElementParameters()) {
                if (parameter.getFieldType() == EParameterFieldType.TABLE && parameter.getRepositoryValue() != null
                        && parameter.getRepositoryValue().equals("XML_MAPPING")) { //$NON-NLS-1$
                    List<Map<String, Object>> value2 = (List<Map<String, Object>>) parameter.getValue();
                    RepositoryToComponentProperty.getTableXMLMappingValue(getConnection(), value2, newOutputMetadata);
                    IElementParameter elementParameter = node.getElementParameter(EParameterName.PROPERTY_TYPE.getName());
                    parameter.setRepositoryValueUsed(true);
                    if (getConnection() instanceof MDMConnection) {
                        if (elementParameter != null && EmfComponent.BUILTIN.equals(elementParameter.getValue())) {
                            parameter.setRepositoryValueUsed(false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void undo() {
        propagateDatas(false);

        if (currentInputMetadata != null) {
            if (!currentInputMetadata.sameMetadataAs(oldInputMetadata, IMetadataColumn.OPTIONS_NONE)) {
                currentInputMetadata.setListColumns(oldInputMetadata.getListColumns());
                if (inputWasRepository) {
                    inputNode.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.REPOSITORY);
                }
            }
        }
        if (!currentOutputMetadata.sameMetadataAs(oldOutputMetadata, IMetadataColumn.OPTIONS_NONE)) {
            List<IMetadataColumn> currentColumns = new ArrayList<IMetadataColumn>(oldOutputMetadata.getListColumns());
            currentOutputMetadata.setListColumns(currentColumns);
            MetadataToolHelper.copyTable(oldOutputMetadata, currentOutputMetadata);
        }

        for (INodeConnector connector : node.getListConnector()) {
            if ((!connector.getName().equals(currentConnector)) && connector.getBaseSchema().equals(currentConnector)) {
                MetadataToolHelper.copyTable(oldOutputMetadata, node.getMetadataFromConnector(connector.getName()));
            }
        }
        if (outputWasRepository) {
            node.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.REPOSITORY);
        }
        for (ChangeMetadataCommand cmd : propagatedChange) {
            cmd.undo();
        }

        List<ColumnNameChanged> columnNameChanged = MetadataToolHelper.getColumnNameChanged(oldOutputMetadata, newOutputMetadata);
        ColumnListController.updateColumnList(node, columnNameChanged, true);
        // newOutputMetadata.setListColumns(new ArrayList<IMetadataColumn>(oldOutputMetadata.getListColumns()));

        if (!internal) {
            updateColumnList(newOutputMetadata, oldOutputMetadata);
            ((Process) node.getProcess()).checkProcess();
        }
        refreshTHMAPObjectType();
    }

    /**
     * Getter for connection.
     * 
     * @return the connection
     */
    public org.talend.core.model.metadata.builder.connection.Connection getConnection() {
        return this.connection;
    }

    /**
     * Sets the connection.
     * 
     * @param connection the connection to set
     */
    public void setConnection(org.talend.core.model.metadata.builder.connection.Connection connection) {
        this.connection = connection;
    }

    /**
     * qzhang Comment method "setDBTableFieldValue".
     */
    protected void setDBTableFieldValue(Element curNode, String newdbTableName, String olddbTableName) {
        // add the code for the DBTable Field. bug 1304.
        if (curNode != null) {
            String uniqueName = ((Node) curNode).getUniqueName();
            IElementParameter dbTableElementField = curNode.getElementParameterFromField(EParameterFieldType.DBTABLE);
            if (dbTableElementField == null) {
                dbTableElementField = curNode.getElementParameter(EParameterName.TABLE.getName());
            }
            changeTableNameParameter(newdbTableName, olddbTableName, uniqueName, dbTableElementField);
            if (((Node) curNode).isELTComponent()) {
                IElementParameter eltTablename = curNode.getElementParameter("ELT_TABLE_NAME"); //$NON-NLS-1$
                changeTableNameParameter(newdbTableName, olddbTableName, uniqueName, eltTablename);
            }
        }
    }

    protected void setSAPFunctionName(Element curNode, String functionName) {
        if (functionName == null) {
            return;
        }

        //        IElementParameter parameter = curNode.getElementParameter("SAP_FUNCTION"); //$NON-NLS-1$
        // if (parameter != null) {
        // parameter.setValue(TalendTextUtils.addQuotes(functionName));
        // }
    }

    /**
     * DOC qzhang Comment method "changeTableNameParameter".
     * 
     * @param newdbTableName
     * @param olddbTableName
     * @param uniqueName
     * @param dbTableElementField
     */
    private void changeTableNameParameter(String newdbTableName, String olddbTableName, String uniqueName,
            IElementParameter dbTableElementField) {
        if (dbTableElementField != null) {
            if (isNotSim) {
                newdbTableName = newdbTableName == null ? "" : newdbTableName; //$NON-NLS-1$
                dbTableElementField.setValue(TalendTextUtils.addQuotes(newdbTableName));
                return;
            }
            Object value = dbTableElementField.getValue();
            String removeQuotes = TalendTextUtils.removeQuotes((String) value);
            boolean b = value == null || removeQuotes.equals(""); //$NON-NLS-1$
            // add the code for table name equals uniqueName
            b = b || uniqueName.equals(olddbTableName);
            b = b || value != null && removeQuotes.toString().equals(olddbTableName);

            if (b) {
                newdbTableName = newdbTableName == null ? "" : newdbTableName; //$NON-NLS-1$
                dbTableElementField.setValue(TalendTextUtils.addQuotes(newdbTableName));
            }
        }
    }

    private void refreshTHMAPObjectType() {
        if (inputNode != null && inputNode.getComponent().getName().equals("tHMap")) {
            if (inputNode.getElementParameter("SINGLE_COLUMN_OBJECTTYPE") != null) {
                boolean isSingleObject = false;
                if (currentInputMetadata.getListColumns().size() == 1) {
                    if (currentInputMetadata.getListColumns().get(0).getTalendType().equals("id_Object")) {
                        inputNode.getElementParameter("SINGLE_COLUMN_OBJECTTYPE").setValue(true);
                        isSingleObject = true;
                    }
                }
                if (!isSingleObject) {
                    inputNode.getElementParameter("SINGLE_COLUMN_OBJECTTYPE").setValue(false);
                    if (inputNode.getElementParameter("AS_STRING") != null) {
                        inputNode.getElementParameter("AS_STRING").setValue(false);
                        inputNode.getElementParameter("AS_BYTEARRAY").setValue(false);
                        inputNode.getElementParameter("AS_INPUTSTREAM").setValue(false);
                        inputNode.getElementParameter("AS_DOCUMENT").setValue(false);
                    }
                }
            }
        }
        if (node != null && node.getComponent().getName().equals("tHMap")) {
            if (node.getElementParameter("SINGLE_COLUMN_OBJECTTYPE") != null) {
                boolean isSingleObject = false;
                if (currentOutputMetadata.getListColumns().size() == 1) {
                    if (currentOutputMetadata.getListColumns().get(0).getTalendType().equals("id_Object")) {
                        node.getElementParameter("SINGLE_COLUMN_OBJECTTYPE").setValue(true);
                        isSingleObject = true;
                    }
                }
                if (!isSingleObject) {
                    node.getElementParameter("SINGLE_COLUMN_OBJECTTYPE").setValue(false);
                    if (node.getElementParameter("AS_STRING") != null) {
                        node.getElementParameter("AS_STRING").setValue(false);
                        node.getElementParameter("AS_BYTEARRAY").setValue(false);
                        node.getElementParameter("AS_INPUTSTREAM").setValue(false);
                        node.getElementParameter("AS_DOCUMENT").setValue(false);
                    }
                }
            }
        }
    }

}
