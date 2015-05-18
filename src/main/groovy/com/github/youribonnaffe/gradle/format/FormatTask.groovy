package com.github.youribonnaffe.gradle.format
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

public class FormatTask extends DefaultTask {

    FormatExtension model
    Project project

    @TaskAction
    public void doTask() {
        List<Callable<Void>> tasks = new ArrayList<>()

        String operationWeDo = ''
        if (model.importOrder == null && model.formatOptions == null) return
        final ImportSorterAdapter importOrder = FormatInputLoad.importOrder(model.importOrder)
        final JavaFormatter format = FormatInputLoad.format(model.formatOptions)
        if (importOrder != null) operationWeDo += 'importOrder '
        if (format != null) operationWeDo += 'format '
        final String operation = operationWeDo
        final LinkedHashMap<String, Exception> errors = new LinkedHashMap<>()

        for (final File file : FormatInputLoad.files(project, model.files)) {
            tasks.add(new Callable<Void>() {
                private void error(Exception e) {
                    errors.put(file.absolutePath, e)
                }

                @Override
                public Void call() {
                    try {
                        if (file.exists() && !file.isDirectory() && file.canRead() && file.canWrite()) {
                            if (importOrder != null) {
                                importOrder.updateImports(file)
                            }
                            if (format != null) {
                                format.formatFile(file)
                            }
                            logger.debug(operation + file.absolutePath)
                        } else {
                            error(new Exception("could not read or write the file ${file.absolutePath}"))
                        }
                    } catch (Exception e) {
                        error(e)
                    }
                    return null;
                }
            });
        }
        try {
            ExecutorService exec = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())
            exec.invokeAll(tasks)
        } catch (InterruptedException e) {
            throw new RuntimeException('could not finish executing format threads', e)
        } finally{
            if(!errors.isEmpty()){
                throw new GradleException("FAILED ${operationWeDo} due to ${errors.toString()}")
            }
        }
    }
}
