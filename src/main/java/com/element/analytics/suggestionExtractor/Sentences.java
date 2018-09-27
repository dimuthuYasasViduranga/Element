/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import java.util.Iterator;
/**
 *
 * @author HiNaMi
 */
public class Sentences implements Iterator<Sentence> {
    
    private Sentence[] sentences;
    private int currentPos;

    public Sentences () {
    }

    public Sentences addSentences(String[] values) {
        if (values != null) {
            this.sentences = new Sentence[values.length];
            int count = 0;
            for(String value : values) {
                this.sentences[count++] = new Sentence(value);
            }
        }
        return this;
    }


    public Sentences iterator() {
        currentPos = 0;
        return this;
    }

    @Override
    public boolean hasNext() {
        return currentPos < sentences.length;
    }

    @Override
    public Sentence next() {
        Sentence current = sentences[currentPos];
        currentPos++;
        return current;
    }
}
