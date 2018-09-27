/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
/**
 *
 * @author HiNaMi
 */
public class StopWords {
    
    private List<String> words;

    public StopWords () {
        this.words = new ArrayList<String>();
    }

    public void add (String line) {
        if (!line.startsWith("#")) { //add the line which is not a comment
            this.words.add(line);
        }
    }

    public Pattern getStopWordsPattern() {
        final StringBuilder stopWordsPatternBuilder = new StringBuilder();
        for(final String stopWord: words) {
            stopWordsPatternBuilder.append("\\b");
            stopWordsPatternBuilder.append(stopWord);
            stopWordsPatternBuilder.append("\\b");
            stopWordsPatternBuilder.append("|");
        }
        String stopWordsPatternString = stopWordsPatternBuilder.toString();
        String stopWordPatternString  = StringUtils.chop(stopWordsPatternString); //remove last "|"
        return Pattern.compile(stopWordPatternString, Pattern.CASE_INSENSITIVE);
    }
}
