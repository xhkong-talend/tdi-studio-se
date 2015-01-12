// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.json.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.general.Project;
import org.talend.repository.ProjectManager;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * DOC wanghong class global comment. Detailled comment
 */
public class JSONUtil {

    public static final String TMP_JSON_FILE = "tempJSONFile" + '.' + "xml"; //$NON-NLS-1$

    public static final String JSON_FILE = '.' + "xml"; //$NON-NLS-1$

    // for json from url
    public static String tempJSONXsdPath = null;

    public static boolean validateLabelForJSON(String label) {
        if (label == null) {
            return false;
        }
        if (label.length() < 1) {
            return false;
        }
        char firstChar = label.charAt(0);
        // see bug 10359,support begin with "_".
        if (!Character.isLetter(firstChar) && !('_' == firstChar)) {
            return false;
        }
        //        if (label.toLowerCase().startsWith("xml")) { //$NON-NLS-1$
        // return false;
        // }
        char[] array = label.toCharArray();
        for (char element : array) {
            if (Character.isSpaceChar(element) || Character.isWhitespace(element)) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateLabelForFixedValue(String label) {
        if (label == null) {
            return false;
        }
        if (label.length() < 1) {
            return false;
        }
        if (label.toLowerCase().startsWith(JSON_FILE)) {
            return false;
        }
        // char[] array = label.toCharArray();
        // for (int i = 0; i < array.length; i++) {
        // if (Character.isSpaceChar(array[i]) || Character.isWhitespace(array[i])) {
        // return false;
        // }
        // }
        return true;
    }

    public static boolean validateLabelForNameSpace(String label) {
        if (label == null) {
            return false;
        }
        if (label.toLowerCase().startsWith(JSON_FILE)) {
            return false;
        }
        if (label.contains(".")) { //$NON-NLS-1$
            return false;
        }
        if (!("".equals(label)) && !("".equals(label.trim()))) { //$NON-NLS-1$ //$NON-NLS-2$
            char firstChar = label.charAt(0);
            if (!Character.isLetter(firstChar)) {
                return false;
            }
            char[] array = label.toCharArray();
            for (char element : array) {
                if (Character.isSpaceChar(element) || Character.isWhitespace(element)) {
                    return false;
                }
            }

        }
        return true;
    }

    public static String changeJsonToXml(String jsonPath) {
        Project project = ProjectManager.getInstance().getCurrentProject();
        IProject fsProject = null;
        try {
            fsProject = ResourceUtils.getProject(project);
        } catch (PersistenceException e2) {
            ExceptionHandler.process(e2);
        }
        if (fsProject == null) {
            return jsonPath;
        }
        String temPath = fsProject.getLocationURI().getPath() + File.separator + "temp" + File.separator + "jsonwizard"
                + File.separator;

        ConvertJSONString convertJSON = new ConvertJSONString();

        java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream();
        InputStream inStream = null;
        File file = new File(jsonPath);

        // String filename = file.getName().replaceAll("\\.", "_");
        // filename = "tempTest";
        boolean isFromUrl = false;
        boolean illegalURL = false;
        InputStream input = null;

        if (file.exists()) {
            if (file.isDirectory()) {
                return "";
            }
            try {
                input = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                ExceptionHandler.process(e);
            }
        } else {
            isFromUrl = true;
            try {
                input = new URL(jsonPath).openStream();
            } catch (MalformedURLException e) {
                illegalURL = true;
            } catch (IOException e) {
                illegalURL = true;
            }
            if (illegalURL) {
                return "";
            }
        }

        try {
            String jsonStr = IOUtils.toString(input);

            convertJSON.setJsonString(jsonStr);

            convertJSON.generate();
            jsonStr = convertJSON.getJsonString4XML();

            ObjectMapper objMapper = new ObjectMapper();
            Map<String, Object> jsonMap = objMapper.readValue(jsonStr, Map.class);
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);

            xmlModule.addKeySerializer(Object.class, new JsonSerializer<Object>() {

                private BiMap<String, String> nameMap = HashBiMap.create();

                private int index = 0;

                @Override
                public void serialize(Object value, JsonGenerator jgen, SerializerProvider privider) throws IOException,
                        com.fasterxml.jackson.core.JsonProcessingException {
                    String regexString = "\\b[a-zA-Z]\\w*\\b"; //$NON-NLS-1$
                    String originalValue = value.toString();
                    String talendSchema = "RealKey"; //$NON-NLS-1$
                    ToXmlGenerator xgen = (ToXmlGenerator) jgen;
                    if (!originalValue.matches(regexString)) {
                        String finalValue = nameMap.get(originalValue);
                        if (finalValue == null) {
                            String replacedValue = originalValue.replaceAll("[\\W]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
                            finalValue = "Talend_" + replacedValue; //$NON-NLS-1$
                            if (nameMap.containsValue(finalValue)) {
                                finalValue = "Talend" + (index++) + "_" + replacedValue; //$NON-NLS-1$ //$NON-NLS-2$
                            }
                            nameMap.put(originalValue, finalValue);
                        }
                        xgen.writeFieldName(finalValue);
                        Map<String, String> attributes = new HashMap<String, String>();
                        attributes.put(talendSchema, originalValue);
                        xgen.setNextAttributes(attributes);
                    } else {
                        jgen.writeFieldName(originalValue);
                    }
                }
            });

            XmlMapper xmlMapper = new XmlMapper(xmlModule);
            xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
            xmlMapper.disable(SerializationFeature.WRAP_ROOT_VALUE);
            String xmlString = xmlMapper.writeValueAsString(jsonMap);

            File xmlFolder = new File(temPath);
            if (!xmlFolder.exists()) {
                xmlFolder.mkdirs();
            }
            temPath = temPath + TMP_JSON_FILE;
            FileWriter writer = new FileWriter(temPath);
            writer.write(xmlString);
            writer.flush();
            writer.close();

            if (isFromUrl) {
                tempJSONXsdPath = temPath;
            }
        } catch (java.lang.Exception e) {
            ExceptionHandler.process(e);
        } finally {
            try {
                outStream.close();
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException e) {
                ExceptionHandler.process(e);
            }

        }
        return temPath;
    }

    public static void deleteWizardTempFiles() {
        tempJSONXsdPath = null;
        Project project = ProjectManager.getInstance().getCurrentProject();
        IProject fsProject = null;
        try {
            fsProject = ResourceUtils.getProject(project);
        } catch (PersistenceException e2) {
            ExceptionHandler.process(e2);
        }
        if (fsProject == null) {
            return;
        }
        String tempPath = fsProject.getLocationURI().getPath() + File.separator + "temp" + File.separator + "wizard";
        File tempWizardDir = new File(tempPath);
        tempWizardDir.delete();
        String tempjsonPath = fsProject.getLocationURI().getPath() + File.separator + "temp" + File.separator + "jsonwizard";
        File tempjsonWizardDir = new File(tempjsonPath);
        if (tempjsonWizardDir.exists()) {
            tempjsonWizardDir.delete();
        }
    }
}
