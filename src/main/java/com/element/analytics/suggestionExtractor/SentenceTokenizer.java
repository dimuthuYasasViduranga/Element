/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

/**
 *
 * @author HiNaMi
 */
public class SentenceTokenizer {
    
    private String grammar;

    public SentenceTokenizer () {
        this.grammar =  "[.!?,;:\\t\\\\-\\\\\"\\\\(\\\\)\\\\\\'\\u2019\\u2013]";
    }

    public SentenceTokenizer (String grammar) {
        this.grammar = grammar;
    }

    public Sentences split (String input) {
        return new Sentences().addSentences(input.split(grammar));
    }
}
