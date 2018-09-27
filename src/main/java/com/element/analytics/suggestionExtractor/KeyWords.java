/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author HiNaMi
 */
public class KeyWords {
    
    private List<String> value;

    public KeyWords () {
        this.value = new ArrayList<String>();
    }

    public void add (String word) {
        value.add(word);
    }

    public List<String> getKeyWords() {
        return value;
    }
}
