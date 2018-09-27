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
public class Word {
    
    private String value;

    public Word(String value) {
        this.value = value;
    }

    public boolean isEmpty() {
        return value == null || value.trim().isEmpty();
    }

    public String getAsLowerCase() {
        return value.trim().toLowerCase();
    }
}
