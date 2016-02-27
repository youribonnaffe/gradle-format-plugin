package com.github.youribonnaffe.gradle.format

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

public class FormatTask extends DefaultTask {
    def FileCollection files = project.sourceSets.main.java + project.sourceSets.test.java
    def File configurationFile
    def List<String> importsOrder
    def File importsOrderConfigurationFile

    @TaskAction
    void format() {
        final Properties settings = loadSettings()
        final formatter = new JavaFormatter(settings)
        def importSorter
        if (importsOrder) {
            importSorter = new ImportSorterAdapter(importsOrder)
        }
        if (importsOrderConfigurationFile) {
            importSorter = new ImportSorterAdapter(importsOrderConfigurationFile.newInputStream())
        }

        List<Callable<Void>> formatTasks = new ArrayList<>()

        String operationWeDo = ''
        if (importSorter != null) operationWeDo += 'importOrder '
        if (formatter != null) operationWeDo += 'format '
        if (importSorter == null && formatter == null) return
        final String operation = operationWeDo
        final Map<String, Exception> errors = new ConcurrentHashMap<>()

        files.each { file ->
            if (file.exists() && !file.isDirectory() && file.canRead() && file.canWrite()) {
                formatTasks.add(new Callable<Void>() {
                    private void error(Exception e) {
                        errors.put(file.absolutePath, e)
                        getLogger().info(file.absolutePath, e)
                    }

                    public void format() {
                        if (formatter != null) {
                            formatter.formatFile(file)
                        }
                    }

                    public void sortImport() {
                        if (importSorter != null) {
                            file.with {
                                write importSorter.sortImports(file.text)
                            }
                        }
                    }

                    @Override
                    public Void call() {
                        try {
                            sortImport()
                            format()
                            logger.debug(operation + file.absolutePath)
                        } catch (Exception e) {
                            error(e)
                        }
                        return null
                    }
                })
            } else {
                errors.put(file.absolutePath, new Exception("could not read or write file"))
            }
        }
        try {
            ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            exec.invokeAll(formatTasks)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt()
            throw new GradleException("could not finish executing format threads", e)
        } finally {
            if (!errors.isEmpty()) {
                throw new GradleException("FAILED " + operationWeDo + errors.toString(), errors.values().find{ true })
            }
        }
    }

    private Properties loadSettings() {
        if (!configurationFile) {
            logger.info "Formatting default configuration"
            return null
        } else if (configurationFile.name.endsWith(".properties")) {
            logger.info "Formatting using configuration file $configurationFile"
            return loadPropertiesSettings()
        } else if (configurationFile.name.endsWith(".xml")) {
            logger.info "Formatting using configuration file $configurationFile"
            return loadXmlSettings()
        } else {
            throw new GradleException("Configuration should be .xml or .properties file")
        }
    }

    private Properties loadPropertiesSettings() {
        Properties settings = new Properties()
        settings.load(configurationFile.newInputStream())
        return settings
    }

    private Properties loadXmlSettings() {
        Properties settings = new Properties()

        def xmlSettings = new XmlParser().parse(configurationFile)
        xmlSettings.profile.setting.each {
            settings.setProperty(it.@id, it.@value)
        }
        return settings
    }

}
