package com.github.youribonnaffe.gradle.format

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class FormatInputLoad {

    static ImportSorterAdapter importOrder(Object importOrderSource) {
        ImportSorterAdapter importConfig = null
        if (importOrderSource != null) {
            if (importOrderSource instanceof ArrayList) {
                importConfig = new ImportSorterAdapter(importOrderSource as ArrayList<String>);
            } else {
                if (importOrderSource instanceof String) {
                    importOrderSource = new File((String) importOrderSource);
                }
                if (!(importOrderSource instanceof File)) throw new GradleException("import order must be either a file, not specified, or a string path");
                File f = (File) importOrderSource;
                checkValidReadFile(f);
                try {
                    importConfig = ImportSorterAdapter.createFromFile(f);
                } catch (IOException e) {
                    throw new GradleException("could not parse " + f.getAbsolutePath());
                }
            }
        }
        return importConfig

    }

    static JavaFormatter format(Object formatSource) {
        Properties formatProperties = null;
        if (formatSource != null) {
            if (formatSource instanceof String) {
                formatSource = new File((String) formatSource);
            }
            if (!(formatSource instanceof File)) {
                throw new GradleException("format must be either a file, not specified, or a string path");
            }
            File f = (File) formatSource;
            checkValidReadFile(f);
            if (f.getName().endsWith(".properties")) {
                formatProperties = readPropertyFormat(f);
            } else if (f.getName().endsWith(".xml")) {
                formatProperties = readXmlFormat(f);
            } else {
                throw new GradleException("Unsupported format file " + f.getAbsolutePath());
            }
        }
        return new JavaFormatter(formatProperties);
    }

    static FileCollection files(Project project, Object fileSource) {
        if (fileSource == null && project.getPlugins().hasPlugin(JavaPlugin.class)) {
            JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
            SourceSet main = javaConvention.getSourceSets().create(SourceSet.MAIN_SOURCE_SET_NAME);
            SourceSet test = javaConvention.getSourceSets().create(SourceSet.TEST_SOURCE_SET_NAME);
            fileSource = main.getAllJava().plus(test.getAllJava());
        }

        if (fileSource instanceof Closure) {
            Closure<?> fileClosure = (Closure<?>) fileSource;
            fileSource = fileClosure.call();
        }
        if (fileSource instanceof File) {
            fileSource = project.files(fileSource.getAbsoluteFile())
        }

        if (!(fileSource instanceof FileCollection)) {
            throw new IllegalArgumentException("file collection or closure returning a file collection expected, got " +
                    (fileSource == null ? "not specified" : fileSource.getClass()));
        }

        return (FileCollection) fileSource;
    }

    private static void checkValidReadFile(File f) {
        if (!f.exists()) {
            throw new GradleException("format file " + f.getAbsolutePath() + " does not exists");
        }
        if (f.isDirectory()) {
            throw new GradleException("format file " + f.getAbsolutePath() + " is a directory");
        }
        if (!f.canRead()) {
            throw new GradleException("format file " + f.getAbsolutePath() + " is not readable");
        }
    }

    private static Properties readPropertyFormat(File f) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(f));
        } catch (IOException e) {
            throw new GradleException("could not read property file " + f.getAbsolutePath(), e);
        }
        return properties;
    }

    private static Node getChildElement(String name, NodeList nodes) {
        Node node;
        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            if (node.getNodeName().equals(name)) {
                break;
            }
        }
        if (node == null) invalidXml("could not found " + name + " tag");
        return node;

    }

    private static Properties readXmlFormat(File f) {
        Properties properties = new Properties();
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            Node profile = getChildElement("profile", getChildElement("profiles", document.getChildNodes()).getChildNodes());
            NodeList settingNodes = profile.getChildNodes();
            for (int i = 0; i < settingNodes.getLength(); i++) {
                Node setting = settingNodes.item(i);
                if (!setting.getNodeName().equals("setting")) continue;
                NamedNodeMap attr = setting.getAttributes();
                Node idAttribute = attr.getNamedItem("id");
                Node valueAttribute = attr.getNamedItem("value");
                if (idAttribute == null || valueAttribute == null) invalidXml("missing id or value in setting");
                properties.put(idAttribute.getNodeValue(), valueAttribute.getNodeValue());
            }
        } catch (SAXException e) {
            throw new GradleException("could not parse " + f.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new GradleException("could not read " + f.getAbsolutePath(), e);
        } catch (ParserConfigurationException e) {
            throw new GradleException("wrong parser configuration " + f.getAbsolutePath(), e);
        }
        return properties;
    }

    private static void invalidXml(String message) {
        String reason = "invalid xml: ";
        if (message != null) reason += " " + message;
        throw new GradleException(reason);
    }


}
