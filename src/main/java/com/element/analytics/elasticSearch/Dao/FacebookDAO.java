package com.element.analytics.elasticSearch.Dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.element.analytics.elasticSearch.config.BulkProcessorFactory;
import com.element.analytics.emotionClassifier.VectorSpaceModelTester;
import com.element.analytics.suggestionExtractor.Rake;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.restfb.types.Comment;
import com.restfb.types.Page;
import com.restfb.types.Post;

@Component
public class FacebookDAO {

	final String spamUri = "http://localhost:5000/spam-detection";
	final String emotionalSucessURI = "http://localhost:5000//emotions-success-prediction";
	final String reactionSuccessURI = "http://localhost:5000/reactions-success-prediction";
	private final RestHighLevelClient elasticClient;
	public static String facebookAccessToken = null;
	String homeTown = null;
	String pageUrl = null;
	String userId = null;
	
	private FacebookClient facebookClient;
	
	@Autowired
	public FacebookDAO(RestHighLevelClient elasticClient) {
		super();
		this.elasticClient = elasticClient;
	}
	
	public static String getPageId(String url) {
		String[] parts = url.split("/");
		return parts[parts.length - 1];
	}
	
	@SuppressWarnings("deprecation")
	public String savePage(JSONObject userData) throws JSONException {
		boolean success = false;
		String message = null;
		boolean terminated = false;
		
		JSONObject data = null;
		
		try {
			facebookAccessToken = (String) userData.get("facebookAccessToken");
			pageUrl = (String) userData.get("pageUrl");
			facebookClient = new DefaultFacebookClient(facebookAccessToken);
			
			String pageId = getPageId(pageUrl);
			
			Page page = facebookClient.fetchObject(pageId, Page.class, 
	                 Parameter.with("fields", "name, cover, picture"));
			
			String pageName = page.getName();
			String cover = page.getCover().getSource();
			String pic = page.getPicture().getUrl();
			
			data = new JSONObject()
					.put("pageName", pageName)
					.put("cover", cover)
					.put("picture", pic);
			
			//Saving Page to ES
			
			// BulkProcessor Listener
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
	    	
	    	try {
	    		XContentBuilder builder = XContentFactory.jsonBuilder();
	    		builder.startObject();
	    		{
	    			builder.field("userId", userData.get("userId"));
	    			builder.field("pageId", pageId);
	    			builder.field("pageName", pageName);
	    		}
	    		builder.endObject();
	    		
	    		String uniqueID = UUID.randomUUID().toString();
	    		
	    		IndexRequest userRequest = new IndexRequest("pages", "doc", uniqueID).
	        	        source(builder);

	        	bulkProcessor.add(userRequest);
	        	
	        	terminated = bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);
	        	
	    	} catch(IOException | JSONException | InterruptedException e) {
	    		e.printStackTrace();
	    	}
	    	
		} catch (JSONException e) {
			// TODO Auto-generated catch block
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
				.put("message", message)
				.put("data", data).toString();
		
		return returnObj;
	}
	
	public String getPages(JSONObject userData) throws JSONException, IOException {
		JSONObject[] pages = null;
		boolean success = false;
		String message = null;
		
		facebookAccessToken = (String) userData.get("facebookAccessToken");
		facebookClient = new DefaultFacebookClient(facebookAccessToken);
		
		String userId = (String) userData.get("userId");
		SearchRequest searchRequest = new SearchRequest("pages");
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(10);
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("userId", userId);
		sourceBuilder.query(matchQueryBuilder);
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticClient.search(searchRequest);
		
		SearchHits hits = searchResponse.getHits();
		
		SearchHit[] searchHits = hits.getHits();
		
		if (searchHits.length == 0) {
			success = false;
			message = "No pages found with the provided pageId";
		} else {
			for (SearchHit hit : searchHits) {
				userId = hit.getId();
				String sourceAsString = hit.getSourceAsString();
				Map<String, Object> sourceAsMap = hit.getSourceAsMap();
				String pageId = (String) sourceAsMap.get("pageId");
				String pageName = (String) sourceAsMap.get("pageName");
				
				Page page = facebookClient.fetchObject(pageId, Page.class, 
		                 Parameter.with("fields", "name, cover, picture"));
				
				String cover = page.getCover().getSource();
				String pic = page.getPicture().getUrl();
				
				JSONObject data = new JSONObject()				
						.put("pageId", pageId)
						.put("pageName", pageName)
						.put("cover", cover)
						.put("pic", pic);
				
				if (pages == null) {
					pages = new JSONObject[1];
					pages[0] = data;
				} else {
					int len = pages.length;
					JSONObject[] details = new JSONObject[len + 1];
					System.arraycopy(pages, 0, details, 0, len);
					details[len] = data;
					pages = details;
				}
			}
			success = true;
			message = "Pages were retrieved successfully";
		}
		String returnObj = new JSONObject()
				.put("data", pages)
				.put("success", success)
				.put("message", message).toString();
		
		return returnObj;
	}
	
	public boolean storePost(String postId, String userId, String pageId, String message) throws IOException {
		boolean terminated = false;
		
		GetIndexRequest request = new GetIndexRequest();
		request.indices("posts");
		
		boolean exists = elasticClient.indices().exists(request);
		
		if (exists == true) {
			SearchRequest searchRequest = new SearchRequest("posts");
			// searchRequest.types("doc");
			
			SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
			sourceBuilder.size(1);
			MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("postId", postId);
			sourceBuilder.query(matchQueryBuilder);
			searchRequest.source(sourceBuilder);
			
			SearchResponse searchResponse = elasticClient.search(searchRequest);
			
			SearchHits hits = searchResponse.getHits();
			
			SearchHit[] searchHits = hits.getHits();
			
			if (searchHits.length == 0) {
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
		    	
		    	try {
		    		XContentBuilder builder = XContentFactory.jsonBuilder();
		    		builder.startObject();
		    		{
		    			builder.field("postId", postId);
		    			builder.field("userId", userId);
		    			builder.field("likes", "");
						builder.field("shares", "");
		    			builder.field("message", message);
		    			builder.field("pageId", pageId);
		    			builder.startArray("comments");
						builder.endArray();
					}
		    		builder.endObject();
		    		
		    		IndexRequest userRequest = new IndexRequest("posts", "doc", uniqueID).
		        	        source(builder);

		        	bulkProcessor.add(userRequest);
		        	
		        	terminated = bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);
		        	
		    	} catch(IOException | InterruptedException e) {
		    		e.printStackTrace();
		    	}
		    	
		    	if (!terminated) {
		    		return false;
		    	} else {
		    		return true;
		    	}
			} else {
				return true;
			}
		} else {
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
	    	
	    	try {
	    		XContentBuilder builder = XContentFactory.jsonBuilder();
	    		builder.startObject();
	    		{
	    			builder.field("postId", postId);
	    			builder.field("userId", userId);
	    			builder.field("likes", "");
					builder.field("shares", "");
	    			builder.field("message", message);
	    			builder.field("pageId", pageId);
	    			builder.startArray("comments");
					builder.endArray();
				}
	    		builder.endObject();
	    		
	    		IndexRequest userRequest = new IndexRequest("posts", "doc", uniqueID).
	        	        source(builder);

	        	bulkProcessor.add(userRequest);
	        	
	        	terminated = bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);
	        	
	    	} catch(IOException | InterruptedException e) {
	    		e.printStackTrace();
	    	}
	    	
	    	if (!terminated) {
	    		return false;
	    	} else {
	    		return true;
	    	}
		}
		
		
	}
	
	public String getPosts(JSONObject requestData) throws JSONException, IOException {
		JSONObject[] posts = null;
		
		boolean success = false;
		String message = null;
		facebookAccessToken = (String) requestData.get("facebookAccessToken");
		String pageId = (String) requestData.get("pageId");
		String userId = (String) requestData.get("userId");
		
		facebookClient = new DefaultFacebookClient(facebookAccessToken);
		Connection<Post> page = facebookClient.fetchConnection(pageId+"/posts", Post.class,Parameter.with("limit",10));
		
		List<Post> pagePosts = page.getData();
   
	    for(int i = 0; i < pagePosts.size(); i++) {
	        Post p=pagePosts.get(i);
	        String msg = p.getMessage();
	        String postId = p.getId();
	        Connection<JsonObject> picture = facebookClient.fetchConnection(pageId + "/feed", JsonObject.class,
	        		Parameter.with("fields", "full_picture"));
	        String photoUrl = picture.getData().get(0).get("full_picture").toString();
	        
	        System.out.println("msg: " + msg);
	        
	        if (msg != null) {
	        	JSONObject obj = new JSONObject()
		        		.put("postId", postId)
		        		.put("message", msg)
		        		.put("image", photoUrl);
	        	
	        	if (posts == null) {
					posts = new JSONObject[1];
					posts[0] = obj;
				} else {
					int len = posts.length;
					JSONObject[] newObj = new JSONObject[len + 1];
					System.arraycopy(posts, 0, newObj, 0, len);
					newObj[len] = obj;
					posts = newObj;
				}
	        	
	        	boolean status = storePost(postId, userId, pageId, msg);
	        	
	        	if (status == false) {
	        		String returnObj = "";
		        	/*String returnObj = new JSONObject()
		    				.put("data", null)
		    				.put("success", false)
		    				.put("message", "Error occured while storing to database").toString();*/
		        	
		        	return returnObj;
		        }
	        }
	    }
	    
	    success = true;
		message = "Pages were retrieved successfully";
	    
	    String returnObj = new JSONObject()
				.put("data", posts)
				.put("success", success)
				.put("message", message).toString();
		
		return returnObj;
	}
	
	public boolean storeComments(JSONObject[] comments, String id) throws IOException, JSONException {
		
		XContentBuilder builder = XContentFactory.jsonBuilder();
		
		builder.startObject();
		{
			builder.startArray("comments");
			for (JSONObject obj: comments ) {
				builder.startObject();
				builder.field("message", obj.get("comment"));
//				builder.field("id", obj.get("id"));
				builder.endObject();
			}
			builder.endArray();
		}
		builder.endObject();
		
		UpdateRequest request = new UpdateRequest("posts", "doc", id)
		        .doc(builder);
		
		UpdateResponse updateResponse = elasticClient.update(request);
		
		String index = updateResponse.getIndex();
		
		if (index == "posts") {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("deprecation")
	public String getPostData(JSONObject requestData) throws Exception {
		boolean success = false;
		String message = null;
		
		String id = null;
		
		JSONObject[] comments = null;
		
		String postId = (String) requestData.get("postId");
		String facebookAccessToken = (String) requestData.get("facebookAccessToken");

		SearchRequest searchRequest = new SearchRequest("posts");
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(1);
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("postId", postId);
		sourceBuilder.query(matchQueryBuilder);
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticClient.search(searchRequest);
		
		SearchHits hits = searchResponse.getHits();
		
		SearchHit[] searchHits = hits.getHits();
		
		if (searchHits.length == 0) {
			success = false;
			message = "No posts found with the postId";
			String returnObj = new JSONObject()
					.put("success", success)
					.put("message", message)
					.put("data", null).toString();
			
			return returnObj;
		} else {
			for (SearchHit hit : searchHits) {
				id = hit.getId();
			}
		}
		
		facebookClient = new DefaultFacebookClient(facebookAccessToken);
		
		Connection<Comment> commentConnection = facebookClient.fetchConnection(postId + "/comments", 
	         Comment.class, Parameter.with("limit", 100));
		
		for (List<Comment> commentPage : commentConnection) {
		  for (Comment comment : commentPage) {
		    System.out.println("Id: " + comment.getId());
		    System.out.println("comment: " + comment.getMessage());
		    JSONObject obj = new JSONObject()
	        		.put("id", comment.getId())
	        		.put("comment", comment.getMessage());
        	
        	if (comments == null) {
        		comments = new JSONObject[1];
        		comments[0] = obj;
			} else {
				int len = comments.length;
				JSONObject[] newObj = new JSONObject[len + 1];
				System.arraycopy(comments, 0, newObj, 0, len);
				newObj[len] = obj;
				comments = newObj;
			}
		  }
		}
		
		for (JSONObject obj: comments) {
			System.out.println(obj.get("comment"));
		}
		
		JSONObject requestObj = new JSONObject()
				.put("data", comments);

		JSONObject filteredComments = null;
		String[] arr = null;
		JSONObject[] storingFilteredComments = null;
		
		try {
			filteredComments = sendPostRequest(requestObj, spamUri);
			System.out.println("filteredComments: " + filteredComments);
			System.out.println("comments: " + filteredComments.get("comments"));
			JSONArray commentsArray = (JSONArray) filteredComments.get("comments");
			for(int i = 0 ; i < commentsArray.length(); i++) {
				JSONObject obj = commentsArray.getJSONObject(i);
				JSONObject jsonObj = new JSONObject()
						.put("comment", obj.get("comment"));
				if (storingFilteredComments == null) {
					storingFilteredComments = new JSONObject[1];
					storingFilteredComments[0] = jsonObj;
				} else {
					int len = storingFilteredComments.length;
					JSONObject[] newObj = new JSONObject[len + 1];
					System.arraycopy(storingFilteredComments, 0, newObj, 0, len);
					newObj[len] = jsonObj;
					storingFilteredComments = newObj;
				}
			}
			
			System.out.println("filterddata: " + storingFilteredComments );
//			arr = filteredComments.split(",");
//			for (String tok: arr) {
//				String part = tok.replace("[", "").replace("{", "").replace("}", "");
//				System.out.println("tok: " + tok.replace("[", "").replace("{", "").replace("}", ""));
//				if (part.contains("comment")) {
//					String[] c = part.split(":");
//					String comment = c[1];
//					System.out.println("comment: " + comment);
//					JSONObject jsonObj = new JSONObject()
//							.put("comment", comment);
//					if (storingFilteredComments == null) {
//						storingFilteredComments = new JSONObject[1];
//						storingFilteredComments[0] = jsonObj;
//					} else {
//						int len = storingFilteredComments.length;
//						JSONObject[] newObj = new JSONObject[len + 1];
//						System.arraycopy(storingFilteredComments, 0, newObj, 0, len);
//						newObj[len] = jsonObj;
//						storingFilteredComments = newObj;
//					}
//				}
//			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		boolean storeStatus = storeComments(storingFilteredComments, id);
		
		if (storeStatus == true) {
			success = true;
			message = "All processes were performed successfully";
//     		boolean emotionStatus = performEmotionAnalysis(postId);
			boolean emotionStatus = true;
     		if (emotionStatus == true) {
     			boolean suggestionStatus = performSuggestionExtraction(postId);
     			if (suggestionStatus == true) {
     				boolean successStatus = performSuccessPrediction(postId);
     				if (successStatus) {
     					String result = getFinalOutput(postId);
         				return result;
     				}
     			}
     		}
		} else {
			success = false;
			message = "Error occurred while processing";
		}
		
		String returnObj = new JSONObject()
				.put("success", success)
				.put("message", message)
				.put("data", comments).toString();
		
		return returnObj;
	}
	
	public String getFinalOutput(String postId) throws JSONException, IOException {
		SearchRequest searchRequest = new SearchRequest("posts");
		Map<String, Object> emotions = null;
		List<Object> suggestions  = null;
		Map<String, Object> success = null;
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(1);
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("postId", postId);
		sourceBuilder.query(matchQueryBuilder);
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticClient.search(searchRequest);
		
		SearchHits hits = searchResponse.getHits();
		
		SearchHit[] searchHits = hits.getHits();
		
		if (searchHits.length == 0) {
			String returnObj = new JSONObject()
					.put("success", false)
					.put("message", "Error Occured while processing")
					.put("data", null).toString();
			return returnObj;
		} else {
			for (SearchHit hit : searchHits) {
				String sourceAsString = hit.getSourceAsString();
				Map<String, Object> sourceAsMap = hit.getSourceAsMap();
				emotions = (Map<String, Object>) sourceAsMap.get("emotions");
				suggestions = (List<Object>) sourceAsMap.get("suggestions");
				success = (Map<String, Object>) sourceAsMap.get("success");
			}
			JSONObject data = new JSONObject()
					.put("emotions", emotions)
					.put("suggestions", suggestions)
					.put("successRate", success);
			
			String returnObj = new JSONObject()
					.put("success", true)
					.put("message", "Analysis process completed successfully")
					.put("data", data).toString();
			return returnObj;
		}
	}
	
	private JSONObject sendPostRequest(JSONObject requestObj, String URI) throws Exception {

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(URI);

		post.setHeader("Accept", "application/json");
		post.setHeader("Content-type", "application/json");
	    
		StringEntity entity = new StringEntity(requestObj.toString());

		post.setEntity(entity);
		
		HttpResponse response;
		String result = null;

//		HttpResponse response = httpclient.execute(post);
//		System.out.println("\nSending 'POST' request to URL : " + spamUri);
//		System.out.println("Post parameters : " + post.getEntity());
//		System.out.println("Response Code : " + 
//                                    response.getStatusLine().getStatusCode());
//
//		System.out.println("response: " + response.getEntity().getContent());
//		
//		
//		BufferedReader rd = new BufferedReader(
//                        new InputStreamReader(response.getEntity().getContent()));
//
//		StringBuffer result = new StringBuffer();
////		JSONObject responseData = new JSONObject();
//		String line = "";
//		while ((line = rd.readLine()) != null) {
////			System.out.println("line: " + line);
////			responseData.put("data", line);
//			result.append(line);
//		}
//
////		System.out.println("jsonRes: " + responseData);
////		System.out.println(result.toString());
//		
//		return result.toString();
		
		try {
	        response = httpclient.execute(post);        

	        if (response != null) {

	            // A Simple JSON Response Read
	            InputStream instream = response.getEntity().getContent();
	            result = convertStreamToString(instream);
	            // now you have the string representation of the HTML request
	            System.out.println("RESPONSE: " + result);
	            instream.close();
	            if (response.getStatusLine().getStatusCode() == 200) {
//	                netState.setLogginDone(true);
	            	System.out.println("Successfull");
	            }

	        }
	        // Headers
	        org.apache.http.Header[] headers = response.getAllHeaders();
	        for (int i = 0; i < headers.length; i++) {
	            System.out.println(headers[i]);
	        }
	    } catch (ClientProtocolException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
	    } catch (IOException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
	    }
		JSONObject myObject = new JSONObject(result);
	    return myObject;
	}
	
	private static String convertStreamToString(InputStream is) {

	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();

	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return sb.toString();
	}

	public boolean performEmotionAnalysis(String postId) throws IOException, JSONException {
		String id = null;
		ArrayList comments = null;
		
		int joyCount = 0;
		int angerCount = 0;
		int fearCount = 0;
		int guiltCount = 0;
		int disgutCount = 0;
		int sadnessCount = 0;
		int shameCount = 0;
		
		SearchRequest searchRequest = new SearchRequest("posts");
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(1);
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("postId", postId);
		sourceBuilder.query(matchQueryBuilder);
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticClient.search(searchRequest);
		
		SearchHits hits = searchResponse.getHits();
		
		SearchHit[] searchHits = hits.getHits();
		
		if (searchHits.length == 0) {
			return false;
		} else {
			for (SearchHit hit : searchHits) {
				id = hit.getId();
				String sourceAsString = hit.getSourceAsString();
				Map<String, Object> sourceAsMap = hit.getSourceAsMap();
				comments = (ArrayList) sourceAsMap.get("comments");
			}
		}
		for (Object obj: comments) {
//			String comment = obj.
			String cmnt = obj.toString().substring(9, obj.toString().length()-1);
			VectorSpaceModelTester vst = new VectorSpaceModelTester();
			String val = vst.classifyComments(cmnt);
			if (val.contains("joy")) {
				joyCount++;
			} else if (val.contains("anger")) {
				angerCount++;
			} else if (val.contains("disgust")) {
				disgutCount++;
			} else if (val.contains("fear")) {
				fearCount++;
			} else if (val.contains("guilt")) {
				guiltCount++;
			} else if (val.contains("sadness")) {
				sadnessCount++;
			} else if (val.contains("shame")) {
				shameCount++;
			}
		}
		
		XContentBuilder builder = XContentFactory.jsonBuilder();
		
		builder.startObject();
		{
			builder.startObject("emotions");
				builder.field("joy", joyCount);
				builder.field("anger", angerCount);
				builder.field("disgust", disgutCount);
				builder.field("fear", fearCount);
				builder.field("guilt", guiltCount);
				builder.field("sadness", sadnessCount);
				builder.field("shame", shameCount);
			builder.endObject();
		}
		builder.endObject();
		
		UpdateRequest request = new UpdateRequest("posts", "doc", id)
		        .doc(builder);
		
		UpdateResponse updateResponse = elasticClient.update(request);
		
		String index = updateResponse.getIndex();
		
		if (index == "posts") {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean performSuggestionExtraction(String postId) throws IOException, JSONException {
		String[] reviews = null;
		String id = null;
		ArrayList comments = null;
		JSONArray suggestions = null;
		
		SearchRequest searchRequest = new SearchRequest("posts");
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(1);
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("postId", postId);
		sourceBuilder.query(matchQueryBuilder);
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticClient.search(searchRequest);
		
		SearchHits hits = searchResponse.getHits();
		
		SearchHit[] searchHits = hits.getHits();
		
		if (searchHits.length == 0) {
			return false;
		} else {
			for (SearchHit hit : searchHits) {
				id = hit.getId();
				String sourceAsString = hit.getSourceAsString();
				Map<String, Object> sourceAsMap = hit.getSourceAsMap();
				comments = (ArrayList) sourceAsMap.get("comments");
			}
		}
		
		for (Object obj: comments) {
			String cmnt = obj.toString().substring(9, obj.toString().length()-1);
			System.out.println("comment: " + obj.toString().substring(9, obj.toString().length()-1));
			if (reviews == null) {
				reviews = new String[1];
				reviews[0] = cmnt;
			} else {
				int len = reviews.length;
				String[] newObj = new String[len+1];
				System.arraycopy(reviews, 0, newObj, 0, len);
				newObj[len] = cmnt;
				reviews = newObj;
			}
		}
		
		Rake rake =  new Rake();
		try {
			suggestions = rake.suggestionExtraction(reviews);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XContentBuilder builder = XContentFactory.jsonBuilder();
		
		builder.startObject();
		{
			builder.startArray("suggestions");
			for (int i = 0; i < suggestions.length(); i++ ) {
				builder.startObject();
				builder.field("suggestion", suggestions.getJSONObject(i).get("suggestion"));
				builder.field("count", suggestions.getJSONObject(i).get("count"));
				System.out.println("count111: " + suggestions.getJSONObject(i).get("count"));
				builder.endObject();
			}
			builder.endArray();;
		}
		builder.endObject();
		
		UpdateRequest request = new UpdateRequest("posts", "doc", id)
		        .doc(builder);
		
		UpdateResponse updateResponse = elasticClient.update(request);
		
		String index = updateResponse.getIndex();
		
		if (index == "posts") {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean performSuccessPrediction(String postId) throws Exception {
		String id = null;
		int[] emotionsArray = new int[7];
		int[] statsArray = new int[9];
		Map<String, Object> emotionsObject = null;
		JSONObject emotionsSuccess = null;
		JSONObject statsSuccess = null;
		
		String[] emotionLabels = new String[7];
		emotionLabels[0] = "shame";
		emotionLabels[1] = "sadness";
		emotionLabels[2] = "guilt";
		emotionLabels[3] = "joy";
		emotionLabels[4] = "disgust";
		emotionLabels[5] = "anger";
		emotionLabels[6] = "fear";
		
		statsArray[0] = 10;
		statsArray[1] = 5;
		statsArray[2] = 0;
		statsArray[3] = 3;
		statsArray[4] = 0;
		statsArray[5] = 0;
		statsArray[6] = 15;
		statsArray[7] = 0;
		statsArray[8] = 39;
		
		SearchRequest searchRequest = new SearchRequest("posts");
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(1);
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("postId", postId);
		sourceBuilder.query(matchQueryBuilder);
		searchRequest.source(sourceBuilder);
		
		SearchResponse searchResponse = elasticClient.search(searchRequest);
		
		SearchHits hits = searchResponse.getHits();
		
		SearchHit[] searchHits = hits.getHits();
		
		if (searchHits.length == 0) {
			return false;
		} else {
			for (SearchHit hit : searchHits) {
				id = hit.getId();
				String sourceAsString = hit.getSourceAsString();
				Map<String, Object> sourceAsMap = hit.getSourceAsMap();
				emotionsObject = (Map<String, Object>) sourceAsMap.get("emotions");
			}
		}
		
		for(int i = 0; i < emotionLabels.length; i++) {
			emotionsArray[i] = (int) emotionsObject.get(emotionLabels[i]);
		}
		
		JSONObject emotionsRequest = new JSONObject()
				.put("data", emotionsArray);
		
		emotionsSuccess = sendPostRequest(emotionsRequest, emotionalSucessURI);
		
		System.out.println("emotions: " + emotionsSuccess);
		
		JSONObject statsRequest = new JSONObject()
				.put("data", statsArray);
		
		statsSuccess = sendPostRequest(emotionsRequest, emotionalSucessURI);
		
		System.out.println("stats: " + statsSuccess);
		
		XContentBuilder builder = XContentFactory.jsonBuilder();
		
		builder.startObject();
		{
			builder.startObject("success");
				builder.field("Statistics", statsSuccess.get("success"));
				builder.field("Emotions", emotionsSuccess.get("success"));
			builder.endObject();
		}
		builder.endObject();
		
		UpdateRequest request = new UpdateRequest("posts", "doc", id)
		        .doc(builder);
		
		UpdateResponse updateResponse = elasticClient.update(request);
		
		String index = updateResponse.getIndex();
		
		if (index == "posts") {
			return true;
		} else {
			return false;
		}
	}
}


