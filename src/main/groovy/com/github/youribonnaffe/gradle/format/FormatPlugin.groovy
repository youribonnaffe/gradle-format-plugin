package com.github.youribonnaffe.gradle.format

import org.gradle.api.Plugin
import org.gradle.api.Project

class FormatPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.task('format', type: FormatTask, description: 'Formats Java source code (style and import order)')
    }
}
