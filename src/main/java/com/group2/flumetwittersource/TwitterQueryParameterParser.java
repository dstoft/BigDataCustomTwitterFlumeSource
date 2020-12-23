package com.group2.flumetwittersource;

public class TwitterQueryParameterParser {

    private String[] getValuesFromCSV(String csvString) {
        String[] values;

        if (csvString.trim().length() == 0) {
            values = new String[0];
        } else {
            values = csvString.split(",");
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }
        }

        return values;
    }

    public String[] getKeywords(String keywordsString) {
        return getValuesFromCSV(keywordsString);
    }
}