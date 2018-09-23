package com.element.analytics.elasticSearch.Dao;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.element.analytics.elasticSearch.config.BulkProcessorFactory;
import com.element.analytics.passwordUtils.PasswordEncryption;

@Component
public class UserDao {
	private final RestHighLevelClient elasticClient;
	
	@Autowired
    public UserDao(RestHighLevelClient elasticClient) {
		super();
		this.elasticClient = elasticClient;
	}

	public String createUser(JSONObject user) throws JSONException {
		
		boolean terminated = false;
		
    	boolean success = false;
    	String message = "Error occurred while execution";
		
    	BulkProcessor.Listener listener = new BulkProcessor.Listener() {
    	    @Override
    	    public void beforeBulk(long executionId, BulkRequest request) {
    	    	int numberOfActions = request.numberOfActions(); 
    	        System.out.println("Executing bulk [{}] with {} requests" +
    	                executionId + numberOfActions);
    	    }

    	    @Override
    	    public void afterBulk(long executionId, BulkRequest request,
    	            BulkResponse response) {
    	    	if (response.hasFailures()) {
    	            System.out.println("Bulk [{}] executed with failures" + executionId);
    	        } else {
    	            System.out.println("Bulk [{}] completed in {} milliseconds" +
    	                    executionId + response.getTook().getMillis());
    	        }
    	    }

    	    @Override
    	    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
    	        System.out.println("Failed to execute bulk" + failure);
    	    }
    	};
    	
    	BulkProcessorFactory bulkProcessorFactory = new BulkProcessorFactory(elasticClient);
    	
    	BulkProcessor bulkProcessor = bulkProcessorFactory.getBulkProcessor(listener);
    	
    	String uniqueID = UUID.randomUUID().toString();
    	
    	String password = (String) user.get("password");
    	String salt = new PasswordEncryption().getSalt(30);
    	String securePassword = new PasswordEncryption().generateSecurePassword(password, salt);
    	
    	try {
    		XContentBuilder builder = XContentFactory.jsonBuilder();
    		builder.startObject();
    		{
    			builder.field("fname", user.get("fname"));
    			builder.field("lname", user.get("lname"));
    			builder.field("email", user.get("email"));
    			builder.field("salt", salt);
    			builder.field("password", securePassword);
    		}
    		builder.endObject();
    		
    		IndexRequest userRequest = new IndexRequest("users", "doc", uniqueID).
        	        source(builder);

        	bulkProcessor.add(userRequest);
        	
        	terminated = bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);
        	
    	} catch(IOException | JSONException | InterruptedException e) {
    		e.printStackTrace();
    	}
    	
    	if (!terminated) {
    		success = true;
    		message = "Some Requests have not been processed";
    	} else {
    		success = true;
    		message = "All requests were processed successfully";
    	}
    	
    	String returnObj = new JSONObject()
    			.put("success", success)
				.put("message", message).toString();
				
		return returnObj;
    }
	
	public String VerifyPassword(JSONObject credentials) throws JSONException, IOException {
		String email = (String) credentials.get("email");
		String providedPassword = (String) credentials.get("password");
		String userId = "";
		String securePassword = "";
		String salt = "";
		String accessToken = null;
		boolean success = false;
		
		String message = "Error occurred while execution";
		
		SearchRequest searchRequest = new SearchRequest("users");
		// searchRequest.types("doc");
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(1);
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("email", email);
		sourceBuilder.query(matchQueryBuilder);
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticClient.search(searchRequest);
		
		SearchHits hits = searchResponse.getHits();
		
		SearchHit[] searchHits = hits.getHits();
		for (SearchHit hit : searchHits) {
			userId = hit.getId();
			String sourceAsString = hit.getSourceAsString();
			Map<String, Object> sourceAsMap = hit.getSourceAsMap();
			securePassword = (String) sourceAsMap.get("password");
			salt = (String) sourceAsMap.get("salt");
		}
		
		boolean passwordMatch = new PasswordEncryption().verifyUserPassword(providedPassword, securePassword, salt);
		
		if(passwordMatch) 
        {
            message = "Provided user password is correct.";
            accessToken = new PasswordEncryption().issueSecureToken(userId, providedPassword);
            success = true;
        } else {
            message = "Provided password is incorrect";
        }
		
		String returnObj = new JSONObject()
				.put("accessToken", accessToken)
				.put("message", message)
				.put("success", success).toString();
		
		return returnObj;
	}
}
