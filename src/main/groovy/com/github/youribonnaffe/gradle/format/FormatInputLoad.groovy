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
                importConfig = new ImportSorterAdapter(importOrderSource as ArrayList<String>)
            } else {
                if (importOrderSource instanceof String) {
                    importOrderSource = new File((String) importOrderSource)
                }
                if (!(importOrderSource instanceof File)) throw new GradleException("import order must be either a file, not specified, or a string path")
                File f = (File) importOrderSource
                checkValidReadFile(f)
                try {
                    importConfig = ImportSorterAdapter.createFromFile(f)
                } catch (IOException e) {
                    throw new GradleException("could not parse ${f.absolutePath}")
                }
            }
        }
        return importConfig

    }

    static JavaFormatter format(Object formatSource) {
        Properties formatProperties = null
        if (formatSource != null) {
            if (formatSource instanceof String) {
                formatSource = new File((String) formatSource)
            }
            if (!(formatSource instanceof File)) {
                throw new GradleException("format must be either a file, not specified, or a string path")
            }
            File f = (File) formatSource
            checkValidReadFile(f)
            if (f.name.endsWith(".properties")) {
                formatProperties = readPropertyFormat(f)
            } else if (f.name.endsWith(".xml")) {
                formatProperties = readXmlFormat(f)
            } else {
                throw new GradleException("Unsupported format file ${f.absolutePath}")
            }
        }
        return new JavaFormatter(formatProperties)
    }

    static FileCollection files(Project project, Object fileSource) {
        if (fileSource == null && project.plugins.hasPlugin(JavaPlugin.class)) {
            JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention.class)
            SourceSet main = javaConvention.sourceSets.create(SourceSet.MAIN_SOURCE_SET_NAME)
            SourceSet test = javaConvention.sourceSets.create(SourceSet.TEST_SOURCE_SET_NAME)
            fileSource = main.allJava.plus(test.allJava)
        }

        if (fileSource instanceof Closure) {
            Closure<?> fileClosure = (Closure<?>) fileSource
            fileSource = fileClosure.call()
        }
        if (fileSource instanceof File) {
            fileSource = project.files(fileSource.absoluteFile)
        }

        if (!(fileSource instanceof FileCollection)) {
            throw new IllegalArgumentException("file collection or closure returning a file collection expected, got " +
                    (fileSource == null ? "not specified" : fileSource.class))
        }

        return (FileCollection) fileSource
    }

    private static void checkValidReadFile(File f) {
        if (!f.exists()) {
            throw new GradleException("format file ${f.absolutePath} does not exists")
        }
        if (f.isDirectory()) {
            throw new GradleException("format file ${f.absolutePath} is a directory")
        }
        if (!f.canRead()) {
            throw new GradleException("format file ${f.absolutePath} is not readable")
        }
    }

    private static Properties readPropertyFormat(File f) {
        Properties properties = new Properties()
        try {
            properties.load(new FileInputStream(f))
        } catch (IOException e) {
            throw new GradleException("could not read property file ${f.absolutePath}", e)
        }
        return properties
    }

    private static Node getChildElement(String name, NodeList nodes) {
        Node node
        for (int i = 0; i < nodes.length; i++) {
            node = nodes.item(i)
            if (node.nodeName.equals(name)) {
                break
            }
        }
        if (node == null) invalidXml("could not found ${name} tag")
        return node

    }

    private static Properties readXmlFormat(File f) {
        Properties properties = new Properties()
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)
            Node profile = getChildElement("profile", getChildElement("profiles", document.childNodes).childNodes)
            NodeList settingNodes = profile.childNodes
            for (int i = 0; i < settingNodes.length; i++) {
                Node setting = settingNodes.item(i)
                if (!setting.nodeName.equals("setting")) continue
                NamedNodeMap attr = setting.attributes
                Node idAttribute = attr.getNamedItem("id")
                Node valueAttribute = attr.getNamedItem("value")
                if (idAttribute == null || valueAttribute == null) invalidXml("missing id or value in setting")
                properties.put(idAttribute.nodeValue, valueAttribute.nodeValue)
            }
        } catch (SAXException e) {
            throw new GradleException("could not parse ${f.absolutePath}", e)
        } catch (IOException e) {
            throw new GradleException("could not read ${f.absolutePath}", e)
        } catch (ParserConfigurationException e) {
            throw new GradleException("wrong parser configuration ${f.absolutePath}", e)
        }
        return properties
    }

    private static void invalidXml(String message) {
        String reason = "invalid xml: "
        if (message != null) reason += " " + message
        throw new GradleException(reason)
    }


}
