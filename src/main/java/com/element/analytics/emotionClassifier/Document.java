package com.element.analytics.emotionClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

public class Document implements Comparable<Document> {
	
	private HashMap<String, Integer> termFrequency;
	
	private String filename;
	
	/**
	 * The constructor.
	 * Takes the name of a file to read.
	 */
	public Document(String filename) {
		this.filename = filename;
		termFrequency = new HashMap<String, Integer>();
		
		readFileAndPreProcess();
	}
	
	/**
	 * Calculate term frequency - tf value (count of each word)
	 * Every word is converted to lower case.
	 * Every character that is not a letter or a digit is removed. 
	 */
	private void readFileAndPreProcess() {
		try {
			Scanner in = new Scanner(new File(filename));
			System.out.println("Reading file: " + filename + " and preprocessing");
			
			while (in.hasNext()) {
				String nextWord = in.next();
				
				String filteredWord = nextWord.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
				
				if (!(filteredWord.equalsIgnoreCase(""))) {
					if (termFrequency.containsKey(filteredWord)) {
						int oldCount = termFrequency.get(filteredWord);
						termFrequency.put(filteredWord, ++oldCount);
					} else {
						termFrequency.put(filteredWord, 1);
					}
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method will return the term frequency for a given word.
	 */
	public double getTermFrequency(String word) {
		if (termFrequency.containsKey(word)) {
			return termFrequency.get(word);
		} else {
			return 0;
		}
	}
	
	/**
	 * This method will return a set of all the terms which occur in this document.
	 */
	public Set<String> getTermList() {
		return termFrequency.keySet();
	}

	@Override
	public int compareTo(Document other) {
		return filename.compareTo(other.getFileName());
	}

	/**
	 * return the filename
	 */
	private String getFileName() {
		return filename;
	}
	
	/**
	 * return the filename
	 */
	public String toString() {
		return filename;
	}
	
	

}
