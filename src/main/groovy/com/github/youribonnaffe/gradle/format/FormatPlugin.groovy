package com.github.youribonnaffe.gradle.format
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin

class FormatPlugin implements Plugin<Project> {
    public final static String PLUGIN_NAME = "format";

    public void apply(Project project) {
        if (!project.plugins.hasPlugin(JavaPlugin.class)) {
            project.pluginManager.apply(JavaBasePlugin.class)
        }
        def task = project.tasks.create(PLUGIN_NAME, FormatTask) as FormatTask
        task.model = project.extensions.create(PLUGIN_NAME, FormatExtension)
        task.project = project
    }

}
