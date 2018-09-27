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
public class StringUtil {
    
    public static boolean isNumber(final String str) {
        return str.matches("[0-9.]");
    }
}
