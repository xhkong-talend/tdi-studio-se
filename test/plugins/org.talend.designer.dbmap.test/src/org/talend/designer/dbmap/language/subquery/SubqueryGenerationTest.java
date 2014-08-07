package org.talend.designer.dbmap.language.subquery;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.talend.core.model.context.JobContext;
import org.talend.core.model.context.JobContextManager;
import org.talend.core.model.context.JobContextParameter;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataColumn;
import org.talend.core.model.metadata.MetadataTable;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.process.INode;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.dbmap.DbMapComponent;
import org.talend.designer.dbmap.external.data.ExternalDbMapData;
import org.talend.designer.dbmap.external.data.ExternalDbMapEntry;
import org.talend.designer.dbmap.external.data.ExternalDbMapTable;
import org.talend.designer.dbmap.language.generation.DbGenerationManager;
import org.talend.designer.dbmap.language.generation.GenericDbGenerationManager;
import org.talend.designer.dbmap.language.hive.HiveGenerationManager;
import org.talend.designer.dbmap.language.mysql.MysqlGenerationManager;
import org.talend.designer.dbmap.language.oracle.OracleGenerationManager;
import org.talend.designer.dbmap.language.postgres.PostgresGenerationManager;
import org.talend.designer.dbmap.language.teradata.TeradataGenerationManager;

@SuppressWarnings("nls")
public class SubqueryGenerationTest {

    DbMapComponent startComponent;

    DbMapComponent refComponent;

    String sqlOperatorInOn = "=";

    String sqlOperatorInWhere = ">";

    private String getDefaultExpectedStringWithoutContext() {
        return "\"SELECT"
                + " stu_info.stu_id AS stu_id, stu_info.stu_name AS stu_name, stu_info.class_name AS class_name, class_room.name AS room_name, class_room.location AS room_location"
                + " FROM"
                + " ( SELECT student.id AS stu_id, student.name AS stu_name, classes.id AS class_id, classes.name AS class_name, classes.room_id AS room_id"
                + " FROM student INNER JOIN classes ON( classes.id " + sqlOperatorInOn + " student.class_id ) WHERE student.id "
                + sqlOperatorInWhere + " 0 ) stu_info INNER JOIN class_room ON( class_room.id " + sqlOperatorInOn
                + " stu_info.room_id ) WHERE stu_info.stu_id " + sqlOperatorInWhere + " 0\"";
    }

    private String getDefaultExpectedStringWithContext() {
        return "\"SELECT stu_info.stu_id AS stu_id, stu_info.stu_name AS stu_name, stu_info.class_name AS class_name, \" +context.class_room+ \".name AS room_name, \" +context.class_room+ \".location AS room_location"
                + " FROM"
                + " ( SELECT \" +context.student+ \".id AS stu_id, \" +context.student+ \".name AS stu_name, \" +context.classes+ \".id AS class_id, \" +context.classes+ \".name AS class_name, \" +context.classes+ \".room_id AS room_id"
                + " FROM \" +context.student+ \" INNER JOIN \" +context.classes+ \" ON( \" +context.classes+ \".id "
                + sqlOperatorInOn
                + " \" +context.student+ \".class_id ) WHERE \" +context.student+ \".id "
                + sqlOperatorInWhere
                + " 0"
                + " ) stu_info INNER JOIN \" +context.class_room+ \" ON( \" +context.class_room+ \".id "
                + sqlOperatorInOn
                + " stu_info.room_id )" + " WHERE stu_info.stu_id " + sqlOperatorInWhere + " 0\"";
    }

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testBuildSqlSelect_Mysql_WithoutContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        MysqlGenerationManager manager = new MysqlGenerationManager();
        MysqlGenerationManager refManager = new MysqlGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithoutContext();

        executeGenerateSqlWithoutContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Mysql_WithContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        MysqlGenerationManager manager = new MysqlGenerationManager();
        MysqlGenerationManager refManager = new MysqlGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithContext();

        executeGenerateSqlWithContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_GenericDb_WithoutContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        GenericDbGenerationManager manager = new GenericDbGenerationManager();
        GenericDbGenerationManager refManager = new GenericDbGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithoutContext();

        executeGenerateSqlWithoutContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_GenericDb_WithContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        GenericDbGenerationManager manager = new GenericDbGenerationManager();
        GenericDbGenerationManager refManager = new GenericDbGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithContext();

        executeGenerateSqlWithContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Hive_WithoutContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        HiveGenerationManager manager = new HiveGenerationManager();
        HiveGenerationManager refManager = new HiveGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithoutContext();

        executeGenerateSqlWithoutContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Hive_WithContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        HiveGenerationManager manager = new HiveGenerationManager();
        HiveGenerationManager refManager = new HiveGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithContext();

        executeGenerateSqlWithContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Oracle_WithoutContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        OracleGenerationManager manager = new OracleGenerationManager();
        OracleGenerationManager refManager = new OracleGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithoutContext();

        executeGenerateSqlWithoutContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Oracle_WithContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        OracleGenerationManager manager = new OracleGenerationManager();
        OracleGenerationManager refManager = new OracleGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithContext();

        executeGenerateSqlWithContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Teradata_WithoutContext() {
        sqlOperatorInOn = "EQ";
        sqlOperatorInWhere = "GT";

        TeradataGenerationManager manager = new TeradataGenerationManager();
        TeradataGenerationManager refManager = new TeradataGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithoutContext();

        executeGenerateSqlWithoutContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Teradata_WithContext() {
        sqlOperatorInOn = "EQ";
        sqlOperatorInWhere = "GT";

        TeradataGenerationManager manager = new TeradataGenerationManager();
        TeradataGenerationManager refManager = new TeradataGenerationManager();

        String expectedQuery = getDefaultExpectedStringWithContext();

        executeGenerateSqlWithContext(manager, refManager, expectedQuery, false);
    }

    @Test
    public void testBuildSqlSelect_Postgres_WithoutContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        PostgresGenerationManager manager = new PostgresGenerationManager();
        PostgresGenerationManager refManager = new PostgresGenerationManager();

        String expectedQuery = "\"SELECT \\\"stu_info\\\".\\\"stu_id\\\" AS \\\"stu_id\\\", \\\"stu_info\\\".\\\"stu_name\\\" AS \\\"stu_name\\\","
                + " \\\"stu_info\\\".\\\"class_name\\\" AS \\\"class_name\\\", \\\"testdb\\\".\\\"class_room\\\".\\\"name\\\" AS \\\"room_name\\\","
                + " \\\"testdb\\\".\\\"class_room\\\".\\\"location\\\" AS \\\"room_location\\\""
                + " FROM ( SELECT \\\"testdb\\\".\\\"student\\\".\\\"id\\\" AS \\\"stu_id\\\", \\\"testdb\\\".\\\"student\\\".\\\"name\\\" AS \\\"stu_name\\\","
                + " \\\"testdb\\\".\\\"classes\\\".\\\"id\\\" AS \\\"class_id\\\", \\\"testdb\\\".\\\"classes\\\".\\\"name\\\" AS \\\"class_name\\\","
                + " \\\"testdb\\\".\\\"classes\\\".\\\"room_id\\\" AS \\\"room_id\\\""
                + " FROM \\\"testdb\\\".\\\"student\\\" INNER JOIN \\\"testdb\\\".\\\"classes\\\""
                + " ON( \\\"testdb\\\".\\\"classes\\\".\\\"id\\\" = \\\"testdb\\\".\\\"student\\\".\\\"class_id\\\" )"
                + " WHERE \\\"testdb\\\".\\\"student\\\".\\\"id\\\" > 0"
                + " ) \\\"stu_info\\\" INNER JOIN \\\"testdb\\\".\\\"class_room\\\""
                + " ON( \\\"testdb\\\".\\\"class_room\\\".\\\"id\\\" = \\\"stu_info\\\".\\\"room_id\\\" )"
                + " WHERE \\\"stu_info\\\".\\\"stu_id\\\" > 0\"";

        executeGenerateSqlWithoutContext(manager, refManager, expectedQuery, true);
    }

    @Test
    public void testBuildSqlSelect_Postgres_WithContext() {
        sqlOperatorInOn = "=";
        sqlOperatorInWhere = ">";

        PostgresGenerationManager manager = new PostgresGenerationManager();
        PostgresGenerationManager refManager = new PostgresGenerationManager();

        String expectedQuery = "\"SELECT \\\"stu_info\\\".\\\"stu_id\\\" AS \\\"stu_id\\\","
                + " \\\"stu_info\\\".\\\"stu_name\\\" AS \\\"stu_name\\\", \\\"stu_info\\\".\\\"class_name\\\" AS \\\"class_name\\\","
                + " \" +context.testdb+ \".\" +context.class_room+ \".\\\"name\\\" AS \\\"room_name\\\","
                + " \" +context.testdb+ \".\" +context.class_room+ \".\\\"location\\\" AS \\\"room_location\\\""
                + " FROM ( SELECT \" +context.testdb+ \".\" +context.student+ \".\\\"id\\\" AS \\\"stu_id\\\","
                + " \" +context.testdb+ \".\" +context.student+ \".\\\"name\\\" AS \\\"stu_name\\\","
                + " \" +context.testdb+ \".\" +context.classes+ \".\\\"id\\\" AS \\\"class_id\\\","
                + " \" +context.testdb+ \".\" +context.classes+ \".\\\"name\\\" AS \\\"class_name\\\","
                + " \" +context.testdb+ \".\" +context.classes+ \".\\\"room_id\\\" AS \\\"room_id\\\""
                + " FROM \" +context.testdb+ \".\" +context.student+ \" INNER JOIN \" +context.testdb+ \".\" +context.classes+ \""
                + " ON( \" +context.testdb+ \".\" +context.classes+ \".\\\"id\\\" = \" +context.testdb+ \".\" +context.student+ \".\\\"class_id\\\" )"
                + " WHERE \" +context.testdb+ \".\" +context.student+ \".\\\"id\\\" > 0"
                + " ) \\\"stu_info\\\" INNER JOIN \" +context.testdb+ \".\" +context.class_room+ \""
                + " ON( \" +context.testdb+ \".\" +context.class_room+ \".\\\"id\\\" = \\\"stu_info\\\".\\\"room_id\\\" )"
                + " WHERE \\\"stu_info\\\".\\\"stu_id\\\" > 0\"";

        executeGenerateSqlWithContext(manager, refManager, expectedQuery, true);
    }

    private void executeGenerateSqlWithoutContext(DbGenerationManager manager, DbGenerationManager refManager,
            String expectedQuery, boolean needSchema) {
        // without context
        String deliverTableName = "stu_info";
        String studentTableName = "student";
        String classesTableName = "classes";
        String classRoomTableName = "class_room";
        String outputTableName = "stu_info_list";
        String schemaName = "";
        if (needSchema) {
            schemaName = "testdb";
        }
        init(schemaName, studentTableName, classesTableName, deliverTableName, classRoomTableName, outputTableName);
        doReturn(refManager).when(refComponent).getGenerationManager();
        String query = manager.buildSqlSelect(startComponent, outputTableName);
        assertNotNull(query);
        query = query.replaceAll("\\s+", " ");

        assertEquals(expectedQuery, query);

    }

    private void executeGenerateSqlWithContext(DbGenerationManager manager, DbGenerationManager refManager, String expectedQuery,
            boolean needSchema) {

        // with context
        String deliverTableName = "stu_info";
        String studentTableName = "context.student";
        String classesTableName = "context.classes";
        String classRoomTableName = "context.class_room";
        String outputTableName = "stu_info_list";
        String schemaName = "";
        if (needSchema) {
            schemaName = "context.testdb";
        }
        init(schemaName, studentTableName, classesTableName, deliverTableName, classRoomTableName, outputTableName);
        JobContext newContext = new JobContext("Default");
        List<IContextParameter> newParamList = new ArrayList<IContextParameter>();
        newContext.setContextParameterList(newParamList);
        JobContextParameter param = new JobContextParameter();
        param.setName("student");
        newParamList.add(param);
        param = new JobContextParameter();
        param.setName("classes");
        newParamList.add(param);
        param = new JobContextParameter();
        param.setName("class_room");
        newParamList.add(param);
        if (needSchema) {
            param = new JobContextParameter();
            param.setName("testdb");
            newParamList.add(param);
        }
        startComponent.getProcess().getContextManager().setDefaultContext(newContext);
        doReturn(refManager).when(refComponent).getGenerationManager();
        String query = manager.buildSqlSelect(startComponent, outputTableName);
        query = query.replaceAll("\\s+", " ");

        assertEquals(expectedQuery, query);
    }

    private void init(String schemaName, String studentTableName, String classesTableName, String deliverTableName,
            String classRoomTableName, String outputTableName) {
        Process process = mock(Process.class);

        String[] studentTableEntities = { "id", "name", "class_id" };
        String[] classesTableEntities = { "id", "name", "room_id" };
        String[] deliverTableEntities = { "stu_id", "stu_name", "class_id", "class_name", "room_id" };

        String studentTableOfficialName = studentTableName;
        String classesTableOfficialName = classesTableName;
        String classRoomTableOfficialName = classRoomTableName;
        if (!StringUtils.isEmpty(schemaName)) {
            studentTableOfficialName = schemaName + "." + studentTableName;
            classesTableOfficialName = schemaName + "." + classesTableName;
            classRoomTableOfficialName = schemaName + "." + classRoomTableName;
        }

        String[] deliverTableExpressions = { studentTableOfficialName + ".id", studentTableOfficialName + ".name",
                classesTableOfficialName + ".id", classesTableOfficialName + ".name", classesTableOfficialName + ".room_id" };

        refComponent = spy(new DbMapComponent());
        initRefComponent(process, refComponent, schemaName, studentTableName, studentTableEntities, classesTableName,
                classesTableEntities, deliverTableName, deliverTableEntities, deliverTableExpressions);
        doReturn(true).when(refComponent).isELTComponent();
        EmfComponent emfComponent = mock(EmfComponent.class);
        when(emfComponent.getName()).thenReturn("tELTXXMap");
        doReturn(emfComponent).when(refComponent).getComponent();

        String[] classRoomTableEntities = { "id", "name", "location" };
        String[] outputTableEntities = { "stu_id", "stu_name", "class_name", "room_name", "room_location" };
        String[] outputTableExpressions = { deliverTableName + ".stu_id", deliverTableName + ".stu_name",
                deliverTableName + ".class_name", classRoomTableOfficialName + ".name", classRoomTableOfficialName + ".location" };

        startComponent = new DbMapComponent();
        initStartComponent(process, startComponent, refComponent, schemaName, deliverTableName, deliverTableEntities,
                classRoomTableName, classRoomTableEntities, outputTableName, outputTableEntities, outputTableExpressions);

    }

    private void initRefComponent(Process process, DbMapComponent component, String schema_name, String main_table,
            String[] mainTableEntities, String lookup_table, String[] lookupEndtities, String out_table,
            String[] outTableEntities, String[] outTableExpressions) {
        List<IConnection> incomingConnections = new ArrayList<IConnection>();
        incomingConnections.add(mockConnection(schema_name, main_table, mainTableEntities));
        incomingConnections.add(mockConnection(schema_name, lookup_table, lookupEndtities));
        initComponent(process, component, schema_name, main_table, mainTableEntities, lookup_table, lookupEndtities, out_table,
                outTableEntities, outTableExpressions, incomingConnections, true);
    }

    /**
     * 
     * DOC cmeng Comment method "initStartComponent".
     * 
     * @param _startComponent
     * @param _refComponent Maybe this parameter <b>**must**</b> be a mock instance
     * @param main_table
     * @param mainTableEntities
     * @param lookup_table
     * @param lookupEndtities
     * @param out_table
     * @param outTableEntities
     * @param outTableExpressions
     */
    private void initStartComponent(Process process, DbMapComponent _startComponent, DbMapComponent _refComponent,
            String schemaName, String main_table, String[] mainTableEntities, String lookup_table, String[] lookupEndtities,
            String out_table, String[] outTableEntities, String[] outTableExpressions) {
        List<IConnection> incomingConnections = new ArrayList<IConnection>();
        incomingConnections.add(mockConnection(_refComponent, schemaName, main_table, mainTableEntities, true));
        incomingConnections.add(mockConnection(schemaName, lookup_table, lookupEndtities));
        initComponent(process, _startComponent, schemaName, main_table, mainTableEntities, lookup_table, lookupEndtities,
                out_table, outTableEntities, outTableExpressions, incomingConnections, false);
    }

    private void initComponent(Process process, DbMapComponent component, String schemaName, String main_table,
            String[] mainTableEntities, String lookup_table, String[] lookupEndtities, String out_table,
            String[] outTableEntities, String[] outTableExpressions, List<IConnection> incomingConnections,
            boolean addSchemaToMainTable) {

        component.setIncomingConnections(incomingConnections);

        ExternalDbMapData externalData = new ExternalDbMapData();
        List<ExternalDbMapTable> inputs = new ArrayList<ExternalDbMapTable>();
        List<ExternalDbMapTable> outputs = new ArrayList<ExternalDbMapTable>();
        // main table
        ExternalDbMapTable inputTable = new ExternalDbMapTable();
        String mainTableOfficialName = main_table;
        String lookupTableOfficialName = lookup_table;
        if (!StringUtils.isEmpty(schemaName)) {
            if (addSchemaToMainTable) {
                mainTableOfficialName = schemaName + "." + main_table;
            }
            lookupTableOfficialName = schemaName + "." + lookup_table;
        }
        inputTable.setTableName(mainTableOfficialName);
        inputTable.setName(mainTableOfficialName);
        List<ExternalDbMapEntry> entities = getMetadataEntities(mainTableEntities, new String[3]);
        inputTable.setMetadataTableEntries(entities);
        inputs.add(inputTable);
        // lookup table
        inputTable = new ExternalDbMapTable();
        inputTable.setTableName(lookupTableOfficialName);
        inputTable.setName(lookupTableOfficialName);
        entities = getMetadataEntities(lookupEndtities, new String[] { sqlOperatorInOn }, new String[] { mainTableOfficialName
                + "." + mainTableEntities[mainTableEntities.length - 1] });
        inputTable.setMetadataTableEntries(entities);
        inputTable.setJoinType("INNER_JOIN");
        inputs.add(inputTable);

        // output
        ExternalDbMapTable outputTable = new ExternalDbMapTable();
        outputTable.setTableName(out_table);
        outputTable.setName(out_table);

        /**
         * condition filters
         */
        List<ExternalDbMapEntry> conditions = new ArrayList<ExternalDbMapEntry>();
        conditions.add(new ExternalDbMapEntry(mainTableOfficialName + "." + mainTableEntities[0] + " " + sqlOperatorInWhere
                + " 0"));
        outputTable.setCustomWhereConditionsEntries(conditions);

        outputTable.setMetadataTableEntries(getMetadataEntities(outTableEntities, outTableExpressions));
        outputs.add(outputTable);

        externalData.setInputTables(inputs);
        externalData.setOutputTables(outputs);
        component.setExternalData(externalData);
        List<IMetadataTable> metadataList = new ArrayList<IMetadataTable>();
        MetadataTable metadataTable = getMetadataTable(outTableEntities);
        metadataTable.setLabel(out_table);
        metadataList.add(metadataTable);
        component.setMetadataList(metadataList);

        doReturn(new JobContextManager()).when(process).getContextManager();
        component.setProcess(process);
    }

    private MetadataTable getMetadataTable(String[] entitiesName) {
        MetadataTable table = new MetadataTable();
        for (String element : entitiesName) {
            MetadataColumn column = new MetadataColumn();
            column.setLabel(element);
            table.getListColumns().add(column);
        }
        return table;
    }

    private List<ExternalDbMapEntry> getMetadataEntities(String[] entitiesName, String[] expressions) {
        return getMetadataEntities(entitiesName, new String[0], expressions);
    }

    private List<ExternalDbMapEntry> getMetadataEntities(String[] entitiesName, String[] operators, String[] expressions) {
        List<ExternalDbMapEntry> entities = new ArrayList<ExternalDbMapEntry>();
        for (int i = 0; i < entitiesName.length; i++) {
            ExternalDbMapEntry entity = new ExternalDbMapEntry();
            entity.setName(entitiesName[i]);
            if (i < expressions.length && !"".equals(expressions[i]) && expressions[i] != null) {
                entity.setExpression(expressions[i]);
            }
            if (i < operators.length && operators[i] != null && !operators[i].isEmpty()) {
                entity.setOperator(operators[i]);
                entity.setJoin(true);
            }
            entities.add(entity);
        }
        return entities;
    }

    private IConnection mockConnection(String schemaName, String tableName, String[] columns) {
        Node node = mock(Node.class);
        when(node.isELTComponent()).thenReturn(true);
        EmfComponent component = mock(EmfComponent.class);
        when(component.getName()).thenReturn("tELTXXInputTable");
        when(node.getComponent()).thenReturn(component);
        return mockConnection(node, schemaName, tableName, columns, false);
    }

    private IConnection mockConnection(INode node, String schemaName, String tableName, String[] columns, boolean isRefLink) {
        Connection connection = mock(Connection.class);
        String tableNameOfficial = tableName;
        ElementParameter param = new ElementParameter(node);
        if (!StringUtils.isEmpty(schemaName)) {
            if (!isRefLink) {
                tableNameOfficial = schemaName + "." + tableName;
            }
            param.setName("ELT_SCHEMA_NAME");
            param.setValue(schemaName);
            doReturn(param).when(node).getElementParameter("ELT_SCHEMA_NAME");
            param = new ElementParameter(node);
        }
        param.setName("ELT_TABLE_NAME");
        param.setValue(tableName);
        doReturn(param).when(node).getElementParameter("ELT_TABLE_NAME");
        when(connection.getName()).thenReturn(tableNameOfficial);
        when(connection.getSource()).thenReturn(node);
        IMetadataTable table = new MetadataTable();
        table.setLabel(tableName);
        table.setTableName(tableName);
        List<IMetadataColumn> listColumns = new ArrayList<IMetadataColumn>();
        for (String columnName : columns) {
            IMetadataColumn column = new MetadataColumn();
            column.setLabel(columnName);
            column.setOriginalDbColumnName(columnName);
            listColumns.add(column);
        }
        table.setListColumns(listColumns);
        when(connection.getMetadataTable()).thenReturn(table);

        return connection;
    }

}
