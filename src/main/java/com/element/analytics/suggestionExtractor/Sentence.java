/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 *
 * @author HiNaMi
 */
public class Sentence {
   
    private String value;

    public Sentence (String value) {
        this.value = value;
    }

    public Phrase generatePhrasesFrom (Pattern stopWordPattern) {
        Matcher matcher = stopWordPattern.matcher(value);
        String sentenceWithoutStopWords = matcher.replaceAll("|");
        return new Phrase().addWords(sentenceWithoutStopWords.split("\\|"));
    }
}
