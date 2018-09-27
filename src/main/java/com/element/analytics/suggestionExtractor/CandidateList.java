/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import java.util.List;
import java.util.regex.Pattern;
/**
 *
 * @author HiNaMi
 */
public class CandidateList {
    
    private KeyWords keyWords;

    public CandidateList () {
        this.keyWords = new KeyWords();
    }

    public CandidateList generateKeywords (Sentences sentences, StopWords stopWords) {
        Sentences iterator = sentences.iterator();
        while(iterator.hasNext()) {
            final Sentence sentence = iterator.next();
            final Pattern pattern = stopWords.getStopWordsPattern();
            Phrase phrase = sentence.generatePhrasesFrom(pattern);
            addKeyWords(phrase);
        }
        return this;
    }

    private void addKeyWords(Phrase phrase) {
        if (null != phrase && !phrase.isEmpty()) {
            phrase = phrase.iterator();
            addKeyWord(phrase);
        }
    }

    private void addKeyWord(Phrase phrase) {
        while(phrase.hasNext()) {
            final Word word = phrase.next();
            validateAndAdd(word);
        }
    }

    private void validateAndAdd(Word word) {
        if (!word.isEmpty()) {
            keyWords.add(word.getAsLowerCase());
        }
    }

    public List<String> getPhraseList() {
        return keyWords.getKeyWords();
    }
}
