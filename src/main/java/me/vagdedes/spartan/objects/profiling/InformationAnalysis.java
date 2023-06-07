package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.CharUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InformationAnalysis {

    // Structure

    private static final Map<String, Map<Integer, Set<Integer>>>
            decimals = Cache.store(new ConcurrentHashMap<>()),
            integers = Cache.store(new ConcurrentHashMap<>());
    private static final Map<Integer, String>
            simplifiedDetection = Cache.store(new ConcurrentHashMap<>());

    public static String[] getHeadArray(String information, int limit) {
        return information.split(" ", limit);
    }

    private static boolean containsDetection(String string, String find) {
        int index = string.indexOf(find);

        if (index > -1) {
            int findLength = find.length(),
                    stringLength = string.length();

            // Start
            if (index == 0) {
                index += findLength;
                return index < stringLength && string.charAt(index) == '_';
            }

            // End
            if (index == (stringLength - 1)) {
                return string.charAt(index - 1) == '_';
            }

            // Between
            if (string.charAt(index - 1) == '_') {
                index += findLength;
                return index >= stringLength || string.charAt(index) == '_';
            }
        }
        return false;
    }

    private static String removeDetectionDetails(String detection) {
        detection = detection.split("\\(")[0];
        boolean foundNumberPreviously = false;
        StringBuilder reconstruct = new StringBuilder();

        for (int position = 0; position < detection.length(); position++) {
            char character = detection.charAt(position);

            if (CharUtils.isNumeric(character)) {
                foundNumberPreviously = true;
            } else {
                if (foundNumberPreviously && character == '.') { // Do not add character as it is part of a decimal
                    continue;
                }
                foundNumberPreviously = false;
                reconstruct.append(character);
            }
        }
        return reconstruct.toString();
    }

    // Orientation

    private final Enums.HackType hackType;
    private final String key;
    private final String[] array;
    private boolean isOption;

    InformationAnalysis(Enums.HackType hackType, String information) {
        this.array = getHeadArray(information, 0);
        this.key = hackType.getStringOrdinal() + array[1];
        this.hackType = hackType;
        this.isOption = false;
    }

    boolean isOption() {
        return isOption;
    }

    String getDetection(boolean details) {
        if (array.length > 1) {
            String currentDetection = array[1];

            if (details) {
                return currentDetection.substring(0, currentDetection.length() - 1); // Remove comma
            }
            int hash = currentDetection.hashCode();
            String cachedDetection = simplifiedDetection.get(hash);

            if (cachedDetection != null) {
                return cachedDetection;
            }
            cachedDetection = removeDetectionDetails(currentDetection.substring(0, currentDetection.length() - 1)); // Remove comma
            Set<String> configKeys = hackType.getCheck().getOptionKeys();
            int configKeyCount = configKeys.size();

            if (configKeyCount > 0) {
                List<String> foundDetections = new ArrayList<>(configKeyCount);

                for (String keyword : cachedDetection.split("-")) {
                    for (String configKey : configKeys) {
                        if (containsDetection(configKey, keyword)) {
                            foundDetections.add(configKey);
                        }
                    }
                }

                if (foundDetections.size() == 1) { // Use the config key instead to cover all the sub-detections
                    cachedDetection = foundDetections.get(0);
                    this.isOption = true;
                }
            }
            simplifiedDetection.put(hash, cachedDetection);
            return cachedDetection;
        }
        return null;
    }

    Double[] getDecimals() {
        int size = array.length;

        if (size >= 2) {
            Map<Integer, Set<Integer>> childMap = decimals.get(key);

            if (childMap == null) {
                childMap = new ConcurrentHashMap<>();
                Map<Integer, Double> positionsAndNumbers = identifyAndShowDecimals(array, size);
                childMap.put(size, positionsAndNumbers.keySet());
                decimals.put(key, childMap);
                return positionsAndNumbers.values().toArray(new Double[0]);
            }
            Set<Integer> positions = childMap.get(size);

            if (positions != null) {
                return showDecimals(array, size, positions);
            }
            Map<Integer, Double> positionsAndNumbers = identifyAndShowDecimals(array, size);
            childMap.put(size, positionsAndNumbers.keySet());
            return positionsAndNumbers.values().toArray(new Double[0]);
        }
        return new Double[]{};
    }

    Integer[] getIntegers() {
        int size = array.length;

        if (size >= 2) {
            Map<Integer, Set<Integer>> childMap = integers.get(key);

            if (childMap == null) {
                childMap = new ConcurrentHashMap<>();
                Map<Integer, Integer> positionsAndNumbers = identifyAndShowIntegers(array, size);
                childMap.put(size, positionsAndNumbers.keySet());
                integers.put(key, childMap);
                return positionsAndNumbers.values().toArray(new Integer[0]);
            }
            Set<Integer> positions = childMap.get(size);

            if (positions != null) {
                return showIntegers(array, size, positions);
            }
            Map<Integer, Integer> positionsAndNumbers = identifyAndShowIntegers(array, size);
            childMap.put(size, positionsAndNumbers.keySet());
            return positionsAndNumbers.values().toArray(new Integer[0]);
        }
        return new Integer[]{};
    }

    // Utils

    private Map<Integer, Double> identifyAndShowDecimals(String[] array, int size) {
        Map<Integer, Double> map = new LinkedHashMap<>(size);

        for (int position = 0; position < array.length; position++) {
            if (position % 2 == 1) {
                Double decimal = AlgebraUtils.returnValidDecimal(array[position].substring(1)); // Removes the comma

                if (decimal != null) {
                    map.put(position, decimal);
                }
            }
        }
        return map;
    }

    private Map<Integer, Integer> identifyAndShowIntegers(String[] array, int size) {
        Map<Integer, Integer> map = new LinkedHashMap<>(size);

        for (int position = 0; position < array.length; position++) {
            if (position % 2 == 1) {
                Integer integer = AlgebraUtils.returnValidInteger(array[position].substring(1)); // Removes the comma

                if (integer != null) {
                    map.put(position, integer);
                }
            }
        }
        return map;
    }

    private Double[] showDecimals(String[] array, int size, Set<Integer> positions) {
        List<Double> numbers = new ArrayList<>(size);

        for (int position : positions) {
            if (position < size) {
                Double decimal = AlgebraUtils.returnValidDecimal(array[position].substring(1)); // Removes the comma

                if (decimal != null) { // basic check to avoid exceptions at a high chance
                    numbers.add(decimal);
                }
            } else {
                break;
            }
        }
        return numbers.toArray(new Double[0]);
    }

    private Integer[] showIntegers(String[] array, int size, Set<Integer> positions) {
        List<Integer> numbers = new ArrayList<>(size);

        for (int position : positions) {
            if (position < size) {
                Integer integer = AlgebraUtils.returnValidInteger(array[position].substring(1)); // Removes the comma

                if (integer != null) { // basic check to avoid exceptions at a high chance
                    numbers.add(integer);
                }
            } else {
                break;
            }
        }
        return numbers.toArray(new Integer[0]);
    }
}
