/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import java.io.IOException;
/**
 *
 * @author HiNaMi
 */
public class StopList {
    
    private StopWords stopWords;

    public StopList() {
        this.stopWords = new StopWords();
    }

    public StopList generateStopWords(FileUtil fileUtil) throws IOException {
        final FileUtil fileUtilIterator = fileUtil.iterator();
        while(fileUtilIterator.hasNext()) {
            stopWords.add(fileUtilIterator.next());
        }
        return this;
    }

    public StopWords getStopWords() {
        return stopWords;
    }
}
