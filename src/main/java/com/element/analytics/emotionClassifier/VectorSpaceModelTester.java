package com.element.analytics.emotionClassifier;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;

public class VectorSpaceModelTester {

	public static void main(String[] args) {
		Document query = new Document("E:\\dataStructures\\Emotion Data\\query.txt");
		Document d1 = new Document("E:\\dataStructures\\Emotion Data\\anger.txt");
		Document d2 = new Document("E:\\dataStructures\\Emotion Data\\joy.txt");
		Document d3 = new Document("E:\\dataStructures\\Emotion Data\\sadness.txt");
		Document d4 = new Document("E:\\dataStructures\\Emotion Data\\guilt.txt");
		Document d5 = new Document("E:\\dataStructures\\Emotion Data\\shame.txt");
		Document d6 = new Document("E:\\dataStructures\\Emotion Data\\fear.txt");
		Document d7 = new Document("E:\\dataStructures\\Emotion Data\\disgust.txt");

		ArrayList<Document> documents = new ArrayList<Document>();
		documents.add(query);
		documents.add(d1);
		documents.add(d2);
		documents.add(d3);
		documents.add(d4);
		documents.add(d5);
		documents.add(d6);
		documents.add(d7);

		
		Corpus corpus = new Corpus(documents);
		
		VectorSpaceModel vectorSpace = new VectorSpaceModel(corpus);
		

		for(int i = 1; i < documents.size(); i++) {
			Document doc = documents.get(i);
			System.out.println("\nComparing to " + doc);
			System.out.println(vectorSpace.cosineSimilarity(query, doc));
		}
	}

}
