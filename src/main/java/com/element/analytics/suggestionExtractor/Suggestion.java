/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author HiNaMi
 */
public class Suggestion {
    
    private String line;
    private Set<String> keys;

    public Suggestion() {
    }

    public Suggestion generateSuggestion
        (Map<String, Double> sortedKeyWordCandidates, List<List<String>> vocabulary) {
        
        keys = sortedKeyWordCandidates.keySet();
        for(String k:keys){
            for (int i = 0; i < vocabulary.size(); i++)
            {
                for (int j = 0; j < vocabulary.get(i).size(); j++)
                {
                    if (vocabulary.get(i).get(j).equals(k))
                    {
                        line = vocabulary.get(i).get(0);
                        break;
                    }
                } 
            }
        }
        return this;
    }

    public String getSuggestion() {
        return line;
    }  
}
