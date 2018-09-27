/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author HiNaMi
 */
public class Vocabulary {
    
    private List<List<String>> lines = new ArrayList<List<String>>();

    public Vocabulary() {
    }

    public Vocabulary generateVocabulary(FileUtil fileUtil) throws IOException {
        final FileUtil fileUtilIterator = fileUtil.iterator();
        while(fileUtilIterator.hasNext()) {
            lines.add(Arrays.asList(fileUtilIterator.next().split("=")));
        }
        System.out.println(lines);        
        return this;
    }

    public List<List<String>> getVocabulary() {
        return lines;
    }  
}
