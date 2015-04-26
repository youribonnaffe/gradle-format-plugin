package com.github.youribonnaffe.gradle

import com.github.youribonnaffe.gradle.format.FormatTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FormatPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    public static Project createTestFormatProject(LinkedHashMap values) {
        def project = ProjectBuilder.builder().build();
        project.apply plugin: 'com.github.youribonnaffe.gradle.format'
        if (values != null) {
            if (values.containsKey('formatOptions')) {
                project.format.formatOptions = values.get('formatOptions')
            }
            if (values.containsKey('importOrder')) {
                project.format.importOrder = values.get('importOrder')
            }
            if (values.containsKey('files')) {
                project.format.files = values.get('files')
            }
        }
        return project
    }

    public static FormatTask formatTask(LinkedHashMap values) {
        return createTestFormatProject(values).tasks.format as FormatTask
    }

    private File throwAwayFileCopy(String filename) {
        def file = folder.newFile(filename)
        file.write(resourceText(filename))
        file
    }

    private String resourceText(String filename) {
        def name = "/" + filename
        def r = getClass().getResourceAsStream(name)
        if (r == null) throw new FileNotFoundException('not found: ' + name)
        return r.text
    }

    @Test
    public void 'format task created'() {
        assert formatTask() != null
    }

    @Test
    public void 'load properties settings'() {
        def sourceFile = throwAwayFileCopy("JavaCodeUnformatted.java")
        formatTask(formatOptions: throwAwayFileCopy("formatter.properties"), files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeFormatted.java")
    }


    @Test
    public void 'load XML settings'() {
        def sourceFile = throwAwayFileCopy("JavaCodeUnformatted.java")
        formatTask(formatOptions: throwAwayFileCopy("formatter.xml"), files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeFormatted.java")
    }

    @Test(expected = GradleException)
    public void 'load unknown settings'() {
        formatTask(formatOptions: throwAwayFileCopy("formatter.unknown")).doTask()
    }

    @Test
    public void 'load null settings'() {
        def sourceFile = throwAwayFileCopy("JavaCodeUnformatted.java")
        formatTask(files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeFormattedDefaultSettings.java")
    }

    @Test
    public void 'format Java 8 code'() {
        def sourceFile = throwAwayFileCopy("JavaCodeUnformatted.java")
        formatTask(formatOptions: throwAwayFileCopy("formatter_java_8.properties"), files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeFormatted.java")
    }

    @Test
    public void 'sort imports'() {
        def sourceFile = throwAwayFileCopy("JavaCodeUnsortedImports.java")
        formatTask(importOrder: ["java", "javax", "org", "\\#com"], files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeSortedImports.java")
    }


    @Test
    public void 'sort imports reading Eclipse file'() {
        def sourceFile = throwAwayFileCopy("JavaCodeUnsortedImports.java")
        formatTask(importOrder: throwAwayFileCopy('import.properties'), files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeSortedImports.java")
    }

    @Test
    public void 'sort imports and format code'() {
        def sourceFile = throwAwayFileCopy("JavaUnsortedImportsAndCodeUnformatted.java")
        formatTask(formatOptions: throwAwayFileCopy('formatter.properties'),
                importOrder: throwAwayFileCopy('import.properties'), files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeSortedImportsCodeFormatted.java")
    }
}
