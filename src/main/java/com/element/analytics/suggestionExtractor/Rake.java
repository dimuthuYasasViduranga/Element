/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.element.analytics.suggestionExtractor;

import org.json.JSONObject;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
/**
 *
 * @author HiNaMi
 */
public class Rake {
    
    public static JSONArray array = new JSONArray();

    private List<String> separateWords(final String text, final int minimumWordReturnSize) {

        final List<String> separateWords = new ArrayList<String>();
        final String[] words = text.split("[^a-zA-Z0-9_\\+\\-/]");

        if (words != null && words.length > 0) {

            for (final String word : words) {

                String wordLowerCase = word.trim().toLowerCase();

                if (wordLowerCase.length() > 0 && wordLowerCase.length() > minimumWordReturnSize &&
                        !StringUtil.isNumber(wordLowerCase)) {

                    separateWords.add(wordLowerCase);
                }
            }
        }

        return separateWords;
    }



    public Map<String,Double> calculateWordScores(List<String> phraseList) {

        final Map<String, Integer> wordFrequency = new HashMap<String, Integer>();
        final Map<String, Integer> wordDegree = new HashMap<String, Integer>();
        final Map<String, Double> wordScore = new HashMap<String, Double>();

        for (final String phrase : phraseList) {

            final List<String> wordList = separateWords(phrase, 0);
            final int wordListLength = wordList.size();
            final int wordListDegree = wordListLength - 1;

            for (final String word : wordList) {

                if (!wordFrequency.containsKey(word)) {
                    wordFrequency.put(word, 0);
                }

                if (!wordDegree.containsKey(word)) {
                    wordDegree.put(word, 0);
                }

                wordFrequency.put(word, wordFrequency.get(word) + 1);
                wordDegree.put(word, wordDegree.get(word) + wordListDegree);
            }
        }

        final Iterator<String> wordIterator = wordFrequency.keySet().iterator();

        while (wordIterator.hasNext()) {
            final String word = wordIterator.next();

            wordDegree.put(word, wordDegree.get(word) + wordFrequency.get(word));

            if (!wordScore.containsKey(word)) {
                wordScore.put(word, 0.0);
            }

            wordScore.put(word, wordDegree.get(word) / (wordFrequency.get(word) * 1.0));
        }

        return wordScore;
    }

    public Map<String, Double> generateCandidateKeywordScores(List<String> phraseList,
                                                              Map<String, Double> wordScore) {

        final Map<String, Double> keyWordCandidates = new HashMap<String, Double>();

        for (String phrase : phraseList) {

            final List<String> wordList = separateWords(phrase, 0);
            double candidateScore = 0;

            for (final String word : wordList) {
                candidateScore += wordScore.get(word);
            }

            keyWordCandidates.put(phrase, candidateScore);
        }

        return keyWordCandidates;
    }
    
    public LinkedHashMap<String, Double> sortKeyWordCandidates
            (Map<String,Double> keywordCandidates) {

        final LinkedHashMap<String, Double> sortedKeyWordCandidates = new LinkedHashMap<String, Double>();
        int totaKeyWordCandidates = keywordCandidates.size();
        final List<Map.Entry<String, Double>> keyWordCandidatesAsList =
                new LinkedList<Map.Entry<String, Double>>(keywordCandidates.entrySet());

        Collections.sort(keyWordCandidatesAsList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((Map.Entry<String, Double>)o2).getValue()
                        .compareTo(((Map.Entry<String, Double>)o1).getValue());
            }
        });

        totaKeyWordCandidates = totaKeyWordCandidates / 3;
        for(final Map.Entry<String, Double> entry : keyWordCandidatesAsList) {
            sortedKeyWordCandidates.put(entry.getKey(), entry.getValue());
            if (--totaKeyWordCandidates == 0) {
                break;
            }
        }

        return sortedKeyWordCandidates;
    }
            
    public void getSuggestionCount(Suggestion suggestion) throws JSONException {
        
        int count = 0;
        
        for (int i=0; i<array.length(); i++) {
            
            JSONObject object = array.getJSONObject(i);
            String sug = object.getString("suggestion");
            
            if (sug.equals(suggestion.getSuggestion())) {
                
                count = object.getInt("count");
                array.remove(i);
                break;
            }
        }
        
        if (suggestion.getSuggestion() != null) {
            JSONObject obj = new JSONObject();
            obj.put("suggestion", suggestion.getSuggestion());    
            obj.put("count", count + 1);
            array.put(obj);
        } else {
        }
    }        
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        final String[] comments = new String[5];
        comments[0] = "My review includes good and bad experience. My coffee and quiche were lovely. However, I asked for take away chock chip cookie whilst paying the bill. A young waiter took a box of cookies and was going to take out one for me. His older colleague told him to use one on display. I asked if that cookie is fresh. He said of course. When we tried it at home it was old and yucky. Suggestion-teach your staff not to sell display foods!";
        comments[1] = "Friendly staff. AC chambers. Foods are not delicious.";
        comments[2] = "Food is not tasty.";
        comments[3] = "Tasteless food.";
        comments[4] = "Food is tasteless.";
         
        for(final String text : comments){
            
            final Rake rakeInstance = new Rake();

            final Sentences sentences = new SentenceTokenizer().split(text);
            final StopList stopList = new StopList().generateStopWords(new FileUtil("C:\\Stoplist.txt"));
            final CandidateList candidateList = new CandidateList().generateKeywords(sentences, stopList.getStopWords());


            final Map<String, Double> wordScore = rakeInstance.calculateWordScores(candidateList.getPhraseList());
            final Map<String, Double> keywordCandidates = rakeInstance.generateCandidateKeywordScores(candidateList.getPhraseList(), wordScore);

            System.out.println("keyWordCandidates = "+ keywordCandidates);

            System.out.println("sortedKeyWordCandidates = " +
                    rakeInstance.sortKeyWordCandidates(keywordCandidates));

            final Vocabulary vocabulary = new Vocabulary().generateVocabulary(new FileUtil("C:\\Vocabulary.txt"));

            final Suggestion suggestion = new Suggestion()
                    .generateSuggestion(keywordCandidates, vocabulary.getVocabulary());

            System.out.println("Suggestion = " + suggestion.getSuggestion());
            
            rakeInstance.getSuggestionCount(suggestion);
            
        }
    
        System.out.println(array);
    }
    
}
