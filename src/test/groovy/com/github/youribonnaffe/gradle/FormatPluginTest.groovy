package com.github.youribonnaffe.gradle
import com.github.youribonnaffe.gradle.format.FormatTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FormatPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    public static Project createTestFormatProject(LinkedHashMap params) {
        def project = ProjectBuilder.builder().build();
        project.apply plugin: 'com.github.youribonnaffe.gradle.format'
        if (params != null) {
            if (params.containsKey('format') && params.get('format') != null) {
                project.format.formatOptions = params.get('format')
            }
            if (params.containsKey('import') && params.get('import') != null) {
                project.format.importOrder = params.get('importOrder')
            }
            if (params.containsKey('files') && params.get('files')) {
                project.format.files = params.get('files')
            }
        }
        return project
    }

    public void formatSame(LinkedHashMap params, String start, String result) {
        def sourceFile = throwAwayFileCopy(start)
        formatTask(format: params.containsKey('format') ? throwAwayFileCopy(params.get('format') as String) : null,
                import: params.containsKey('import') ? params.get('import') : null,
                files: sourceFile).doTask()
        assert sourceFile.text == resourceText(result)
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
    public void 'format Java 7 code'() {
        formatSame("JavaCodeUnformatted_Java7.java", "JavaCodeFormatted_Java7.java", format: "formatter_java_7.properties")
    }

    /*
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

    @Test(expected = GradleException.class)
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
        def sourceFile = throwAwayFileCopy("JavaCodeUnformatted_Java8.java")
        formatTask(formatOptions: throwAwayFileCopy("formatter_java_8.properties"), files: sourceFile).doTask()
        assert sourceFile.text == resourceText("JavaCodeFormatted_Java8.java")
    }


    @Test
    public void 'format enum'() {
        def sourceFile = throwAwayFileCopy("EnumUnformatted.java")
        formatTask(formatOptions: throwAwayFileCopy("formatter.properties"), files: sourceFile).doTask()
        assert sourceFile.text == resourceText("Enum.java")
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
    }*/
}
