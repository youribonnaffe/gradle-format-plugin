package com.github.youribonnaffe.gradle.format;

import java.util.*;

// From https://github.com/krasa/EclipseCodeFormatter
/*not thread safe*/
class ImportsSorter {

    private final List<String> template = new ArrayList<>();
    private final Map<String, List<String>> matchingImports = new HashMap<>();
    private final ArrayList<String> notMatching = new ArrayList<>();
    private final Set<String> allImportOrderItems = new HashSet<>();

    private ImportsSorter(List<String> importOrder) {
        List<String> importOrderCopy = new ArrayList<>(importOrder);
        normalizeStaticOrderItems(importOrderCopy);
        putStaticItemIfNotExists(importOrderCopy);
        template.addAll(importOrderCopy);
        this.allImportOrderItems.addAll(importOrderCopy);
    }


    static List<String> sort(List<String> imports, List<String> importsOrder) {
        ImportsSorter importsSorter = new ImportsSorter(importsOrder);
        return importsSorter.sort(imports);
    }

    private List<String> sort(List<String> imports) {
        filterMatchingImports(imports);
        mergeNotMatchingItems(false);
        mergeNotMatchingItems(true);
        mergeMatchingItems();
        return getResult();
    }

    private void putStaticItemIfNotExists(List<String> allImportOrderItems) {
        boolean contains = false;
        int indexOfFirstStatic = 0;
        for (int i = 0; i < allImportOrderItems.size(); i++) {
            String allImportOrderItem = allImportOrderItems.get(i);
            if (allImportOrderItem.equals("static ")) {
                contains = true;
            }
            if (allImportOrderItem.startsWith("static ")) {
                indexOfFirstStatic = i;
            }
        }
        if (!contains) {
            allImportOrderItems.add(indexOfFirstStatic, "static ");
        }
    }

    private void normalizeStaticOrderItems(List<String> allImportOrderItems) {
        for (int i = 0; i < allImportOrderItems.size(); i++) {
            String s = allImportOrderItems.get(i);
            if (s.startsWith("\\#")) {
                allImportOrderItems.set(i, s.replace("\\#", "static "));
            }
        }
    }

    /**
     * returns not matching items and initializes internal state
     */
    private void filterMatchingImports(List<String> imports) {
        for (String anImport : imports) {
            String orderItem = getBestMatchingImportOrderItem(anImport);
            if (orderItem != null) {
                if (!matchingImports.containsKey(orderItem)) {
                    matchingImports.put(orderItem, new ArrayList<String>());
                }
                matchingImports.get(orderItem).add(anImport);
            } else {
                notMatching.add(anImport);
            }
        }
        notMatching.addAll(allImportOrderItems);
    }

    private String getBestMatchingImportOrderItem(String anImport) {
        String matchingImport = null;
        for (String orderItem : allImportOrderItems) {
            if (anImport.startsWith(orderItem)) {
                if (matchingImport == null) {
                    matchingImport = orderItem;
                } else {
                    matchingImport = betterMatching(matchingImport, orderItem, anImport);
                }
            }
        }
        return matchingImport;
    }

    /**
     * not matching means it does not match any order item, so it will be appended before or after order items
     */
    private void mergeNotMatchingItems(boolean staticItems) {
        Collections.sort(notMatching);

        int firstIndexOfOrderItem = getFirstIndexOfOrderItem(notMatching, staticItems);
        int indexOfOrderItem = 0;
        for (String notMatchingItem : notMatching) {
            if (!matchesStatic(staticItems, notMatchingItem)) {
                continue;
            }
            boolean isOrderItem = isOrderItem(notMatchingItem, staticItems);
            if (isOrderItem) {
                indexOfOrderItem = template.indexOf(notMatchingItem);
            } else {
                if (indexOfOrderItem == 0 && firstIndexOfOrderItem != 0) {
                    // insert before alphabetically first order item
                    template.add(firstIndexOfOrderItem, notMatchingItem);
                    firstIndexOfOrderItem++;
                } else if (firstIndexOfOrderItem == 0) {
                    // no order is specified
                    if (template.size() > 0 && (template.get(template.size() - 1).startsWith("static"))) {
                        // insert NEW_LINE after last static import
                        template.add(ImportSorterAdapter.NEW_LINE);
                    }
                    template.add(notMatchingItem);
                } else {
                    // insert after the previous order item
                    template.add(indexOfOrderItem + 1, notMatchingItem);
                    indexOfOrderItem++;
                }
            }
        }
    }

    private boolean isOrderItem(String notMatchingItem, boolean staticItems) {
        boolean contains = allImportOrderItems.contains(notMatchingItem);
        return contains && matchesStatic(staticItems, notMatchingItem);
    }

    /**
     * gets first order item from sorted input list, and finds out it's index in template.
     */
    private int getFirstIndexOfOrderItem(List<String> notMatching, boolean staticItems) {
        int firstIndexOfOrderItem = 0;
        for (String notMatchingItem : notMatching) {
            if (!matchesStatic(staticItems, notMatchingItem)) {
                continue;
            }
            boolean isOrderItem = isOrderItem(notMatchingItem, staticItems);
            if (isOrderItem) {
                firstIndexOfOrderItem = template.indexOf(notMatchingItem);
                break;
            }
        }
        return firstIndexOfOrderItem;
    }

    private boolean matchesStatic(boolean staticItems, String notMatchingItem) {
        boolean isStatic = notMatchingItem.startsWith("static ");
        return (isStatic && staticItems) || (!isStatic && !staticItems);
    }

    private void mergeMatchingItems() {
        for (int i = 0; i < template.size(); i++) {
            String item = template.get(i);
            if (allImportOrderItems.contains(item)) {
                // find matching items for order item
                List<String> strings = matchingImports.get(item);
                if (strings == null || strings.isEmpty()) {
                    // if there is none, just remove order item
                    template.remove(i);
                    i--;
                    continue;
                }
                ArrayList<String> matchingItems = new ArrayList<>(strings);
                Collections.sort(matchingItems);

                // replace order item by matching import statements
                // this is a mess and it is only a luck that it works :-]
                template.remove(i);
                if (i != 0 && !template.get(i - 1).equals(ImportSorterAdapter.NEW_LINE)) {
                    template.add(i, ImportSorterAdapter.NEW_LINE);
                    i++;
                }
                if (i + 1 < template.size() && !template.get(i + 1).equals(ImportSorterAdapter.NEW_LINE)
                        && !template.get(i).equals(ImportSorterAdapter.NEW_LINE)) {
                    template.add(i, ImportSorterAdapter.NEW_LINE);
                }
                template.addAll(i, matchingItems);
                if (i != 0 && !template.get(i - 1).equals(ImportSorterAdapter.NEW_LINE)) {
                    template.add(i, ImportSorterAdapter.NEW_LINE);
                }

            }
        }
        // if there is \n on the end, remove it
        if (template.size() > 0 && template.get(template.size() - 1).equals(ImportSorterAdapter.NEW_LINE)) {
            template.remove(template.size() - 1);
        }
    }

    private List<String> getResult() {
        ArrayList<String> strings = new ArrayList<>();

        for (String s : template) {
            if (s.equals(ImportSorterAdapter.NEW_LINE)) {
                strings.add(s);
            } else {
                strings.add("import " + s + ";" + ImportSorterAdapter.NEW_LINE);
            }
        }
        return strings;
    }

    private static String betterMatching(String order1, String order2, String anImport) {
        if (order1.equals(order2)) {
            throw new IllegalArgumentException("orders are same");
        }
        for (int i = 0; i < anImport.length() - 1; i++) {
            if (order1.length() - 1 == i && order2.length() - 1 != i) {
                return order2;
            }
            if (order2.length() - 1 == i && order1.length() - 1 != i) {
                return order1;
            }
            char orderChar1 = order1.length() != 0 ? order1.charAt(i) : ' ';
            char orderChar2 = order2.length() != 0 ? order2.charAt(i) : ' ';
            char importChar = anImport.charAt(i);

            if (importChar == orderChar1 && importChar != orderChar2) {
                return order1;
            } else if (importChar != orderChar1 && importChar == orderChar2) {
                return order2;
            }

        }
        return null;
    }

}
