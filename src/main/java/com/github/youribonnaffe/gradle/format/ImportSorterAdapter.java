package com.github.youribonnaffe.gradle.format;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * From https://github.com/krasa/EclipseCodeFormatter
 *
 * @author Vojtech Krasa
 */
public class ImportSorterAdapter {
    public static final String NEW_LINE = "\n";

    private final List<String> sortInOrder;

    public ImportSorterAdapter(final List<String> importsOrder) {
        this.sortInOrder = new ArrayList<>(importsOrder);
    }

    public static ImportSorterAdapter createFromFile(final File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            TreeMap<Integer, String> entries = new TreeMap<>();
            for (String line; (line = br.readLine()) != null; ) {
                line = line.trim();
                if (line.startsWith("#")) continue;
                String[] elements = line.split("=");
                entries.put(Integer.parseInt(elements[0]), elements[1]);
            }
            return new ImportSorterAdapter(new ArrayList<>(entries.values()));
        }
    }

    private static class ImportMeta {
        public final List<String> imports;
        public final int firstLine;
        public final int lastLine;

        private ImportMeta(List<String> imports, int firstLine, int lastLine) {
            this.imports = imports;
            this.firstLine = firstLine;
            this.lastLine = lastLine;
        }
    }

    private ImportMeta collectImports(final File document) throws FileNotFoundException {
        final Scanner scanner = new Scanner(document);
        int firstImportLine = 0;
        int lastImportLine = 0;
        int atLine = 0;
        List<String> imports = new ArrayList<>();
        while (scanner.hasNext()) {
            atLine++;
            String next = scanner.nextLine();
            if (next == null) {
                break;
            }
            String IMPORT_LINE_START = "import";
            if (next.startsWith(IMPORT_LINE_START)) {
                if (firstImportLine == 0) {
                    firstImportLine = atLine;
                }
                lastImportLine = atLine;
                int endIndex = next.indexOf(";");
                imports.add(next.substring(IMPORT_LINE_START.length() + 1, endIndex != -1 ? endIndex : next.length()));

            }
        }
        return new ImportMeta(imports, firstImportLine, lastImportLine);
    }

    public String sortImports(final File document) throws FileNotFoundException {
        ImportMeta importMeta = collectImports(document);
        List<String> sortedImports = ImportsSorter.sort(importMeta.imports, sortInOrder);
        return applyImportsToDocument(document, importMeta.firstLine, importMeta.lastLine, sortedImports);
    }

    public void updateImports(final File document) throws IOException {
        String newContent = sortImports(document);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(document.getAbsolutePath()))) {
            writer.write(newContent);
            writer.flush();
        }
    }

    private String applyImportsToDocument(final File document, final int firstImportLine, final int lastImportLine,
                                          final List<String> strings) throws FileNotFoundException {
        final Scanner scanner = new Scanner(document);
        int currentLine = 0;
        final StringBuilder sb = new StringBuilder();
        String line;
        while (scanner.hasNext()) {
            currentLine++;
            line = scanner.nextLine();
            if (line == null) {
                break;
            }
            if (currentLine >= firstImportLine && currentLine <= lastImportLine) {
                for (String string : strings) {
                    sb.append(string);
                }
                for (int i = 0; i < lastImportLine - firstImportLine; i++) {
                    //noinspection UnusedAssignment
                    line = scanner.nextLine();
                    ++currentLine;
                }
            } else {
                sb.append(line).append(NEW_LINE);
            }
        }
        return sb.toString();
    }

}
