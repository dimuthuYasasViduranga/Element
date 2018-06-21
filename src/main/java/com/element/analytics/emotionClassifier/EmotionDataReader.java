package com.element.analytics.emotionClassifier;

import java.io.BufferedReader;
import java.io.FileReader;

public class EmotionDataReader {
	
	public static void main(String[] args) {
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new FileReader("E:/dataStructures/EmotionDataset.csv"));
			String rowData = "";
			br.readLine();
			String[] dataByRow;
			while((rowData = br.readLine()) != null) {
				
				dataByRow = rowData.split(",");
				String data = dataByRow[36];
				if(data.equalsIgnoreCase("guilt")) {
					String expression = String.valueOf(dataByRow[40]);
					System.out.println(expression);
				}
			}
			
		}catch (Exception e) {
		System.out.println(e.getStackTrace());
		}

	}

}
