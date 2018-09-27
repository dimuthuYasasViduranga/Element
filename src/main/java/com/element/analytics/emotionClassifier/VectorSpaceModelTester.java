package com.element.analytics.emotionClassifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.max;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static org.hamcrest.Matchers.equalTo;

public class VectorSpaceModelTester {
		
	public String classifyComments(String Comment) {
		
		 BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter("C:\\SLIIT\\Research\\Emotion Data\\query.txt"));
				writer.write(Comment);
			     
			    writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		    

		Document query = new Document("C:\\SLIIT\\Research\\Emotion Data\\query.txt");
		Document d1 = new Document("C:\\SLIIT\\Research\\Emotion Data\\joy.txt");
		Document d2 = new Document("C:\\SLIIT\\Research\\Emotion Data\\anger.txt");
		Document d3 = new Document("C:\\SLIIT\\Research\\Emotion Data\\sadness.txt");
		Document d4 = new Document("C:\\SLIIT\\Research\\Emotion Data\\guilt.txt");
		Document d5 = new Document("C:\\SLIIT\\Research\\Emotion Data\\shame.txt");
		Document d6 = new Document("C:\\SLIIT\\Research\\Emotion Data\\fear.txt");
		Document d7 = new Document("C:\\SLIIT\\Research\\Emotion Data\\disgust.txt");

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
		
		HashMap<Document,Double> docCosine = new HashMap<Document,Double>();

		for(int i = 1; i < documents.size(); i++) {
			Document doc = documents.get(i);
			System.out.println("\nComparing to " + doc);
			System.out.println(vectorSpace.cosineSimilarity(query, doc));
			docCosine.put(doc, vectorSpace.cosineSimilarity(query, doc));
		}
		
		List<Entry<Document, Double>> val = select(docCosine.entrySet(), having(on(Map.Entry.class).getValue(), 
                equalTo(max(docCosine, on(Double.class)))));
		
		
		 System.out.println(
	                select(docCosine.entrySet(), having(on(Map.Entry.class).getValue(), 
	                        equalTo(max(docCosine, on(Double.class)))))
	        );
		 
		 return val.toString();
	}

}
