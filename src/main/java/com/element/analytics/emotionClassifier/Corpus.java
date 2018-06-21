package com.element.analytics.emotionClassifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class Corpus {
	
	private ArrayList<Document> documents;
	
	private HashMap<String, Set<Document>> invertedIndex;
	
	/**
	 * The constructor - it takes in an arraylist of documents.
	 * It will generate the inverted index based on the documents.
	 */
	public Corpus(ArrayList<Document> documents) {
		this.documents = documents;
		invertedIndex = new HashMap<String, Set<Document>>();
		
		createInvertedIndex();
	}
	
	/**
	 * This method will create an inverted index.
	 * Generate Hashmap, key - term & value - set of documents
	 * that contains term
	 */
	private void createInvertedIndex() {
		System.out.println("Creating the inverted index");
		
		for (Document document : documents) {
			Set<String> terms = document.getTermList();
			
			for (String term : terms) {
				if (invertedIndex.containsKey(term)) {
					Set<Document> list = invertedIndex.get(term);
					list.add(document);
				} else {
					Set<Document> list = new TreeSet<Document>();
					list.add(document);
					invertedIndex.put(term, list);
				}
			}
		}
	}
	
	/**
	 * This method returns the idf for a given term.
	 * return the idf for the term
	 */
	public double getInverseDocumentFrequency(String term) {
		if (invertedIndex.containsKey(term)) {
			double size = documents.size();
			Set<Document> list = invertedIndex.get(term);
			double documentFrequency = list.size();
			
			return Math.log10(size / documentFrequency);
		} else {
			return 0;
		}
	}

	/**
	 * return the documents
	 */
	public ArrayList<Document> getDocuments() {
		return documents;
	}

	/**
	 * return the invertedIndex
	 */
	public HashMap<String, Set<Document>> getInvertedIndex() {
		return invertedIndex;
	}

}
