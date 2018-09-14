package com.element.analytics.elasticSearch.Dao;

import java.io.IOException;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryDAO {
	
	private final RestHighLevelClient elasticClient;
	private IndexResponse indexResponse;
	private GetResponse getResponse;
	
	@Autowired
    public QueryDAO(RestHighLevelClient client){
		this.elasticClient = client;
    }
	
	public IndexResponse createIndex(JSONObject root) {
		
		try {
			
			JSONArray sportsArray = root.getJSONArray("comments");
			
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			{
				builder.field("likes", root.get("likes"));
				builder.field("shares", root.get("shares"));
				builder.field("postId", root.get("postId"));
				builder.field("pageId", root.get("pageId"));
				builder.startArray("comments");
				for (int i = 0; i < sportsArray.length(); i++ ) {
					builder.startObject();
					builder.field("createdTime", sportsArray.getJSONObject(i).getString("created_time"));
					builder.field("message", sportsArray.getJSONObject(i).getString("created_time"));
					builder.field("id", sportsArray.getJSONObject(i).getString("id"));
					builder.endObject();
				}
				builder.endArray();
			}
			builder.endObject();
			
			IndexRequest indexRequest = new IndexRequest("customer_responses", "data", "1").source(builder);
			
			indexResponse = elasticClient.index(indexRequest);
			
			return indexResponse;
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public GetResponse getPostData() {
		GetRequest request = new GetRequest("customer_responses", "data", "1");
		try {
			getResponse = elasticClient.get(request);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return getResponse;
	}

}
