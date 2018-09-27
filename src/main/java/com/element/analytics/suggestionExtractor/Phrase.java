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
public class Phrase implements Iterator<Word> {
    
    private Word[] words;
    private int currentPos;;

    public Phrase() {
    }

    public Phrase addWords(String[] values) {
        if (values != null) {
            this.words = new Word[values.length];
            int i = 0;
            for(String value : values) {
                this.words[i++] = new Word(value);
            }
        }
        return this;
    }

    public boolean isEmpty() {
        return words == null || words.length == 0;
    }

    public Phrase iterator() {
        currentPos = 0;
        return this;
    }

    @Override
    public boolean hasNext() {
        return currentPos < words.length;
    }

    @Override
    public Word next() {
        Word current = words[currentPos];
        currentPos++;
        return current;
    }
}
