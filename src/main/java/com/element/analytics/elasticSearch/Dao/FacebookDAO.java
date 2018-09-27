package com.element.analytics.elasticSearch.Dao;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
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
				builder.field("id", obj.get("id"));
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
	public String getPostData(JSONObject requestData) throws JSONException, IOException {
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
			String returnObj = "";
			/*String returnObj = new JSONObject()
					.put("success", success)
					.put("message", message)
					.put("data", null).toString();*/
			
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

//		RestTemplate restTemplate = new RestTemplate();
//		
//		MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
//		jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//		restTemplate.getMessageConverters().add(jsonHttpMessageConverter);
//		
//	    JSONObject[] result = restTemplate.postForObject( spamUri, requestObj, JSONObject[].class);
	    
//		RestTemplate restTemplate = new RestTemplate(); 
//		HttpHeaders httpHeaders = new HttpHeaders();
//		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//		httpHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
//		try {
//		    HttpEntity<String> entityCredentials = new HttpEntity<String>(requestObj.toString(), httpHeaders);
//		    ResponseEntity<JSONObject[]> responseEntity = restTemplate.exchange(spamUri,
//		            HttpMethod.POST, entityCredentials, JSONObject[].class);
//		    if (responseEntity != null) {
//		        response = responseEntity.getBody();
//		    }
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		System.out.println("res: " + response);
		
		boolean storeStatus = storeComments(comments, id);
		
		if (storeStatus == true) {
			success = true;
			message = "All processes were performed successfully";
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
}

//Connection<Post> myFeed = facebookClient.fetchConnection(pageId + "/feed", Post.class);
//// Connections support paging and are iterable
//
//// Iterate over the feed to access the particular pages
//for (List<Post> myFeedPage : myFeed) {
//
//  // Iterate over the list of contained data 
//  // to access the individual object
//  for (Post post : myFeedPage) {
//    System.out.println("Post: " + post);
//    System.out.println("reations: " + post.getReactionsCount());
//    System.out.println("likes: " + post.getLikesCount());
//    System.out.println("sdf" + post.getCommentsCount());
//  }
//}

//Connection<Comment> commentConnection 
//   = facebookClient.fetchConnection("315719342564507_318957702240671" + "/comments", 
//         Comment.class, Parameter.with("limit", 10));
//
//int personalLimit = 50;
//
//for (List<Comment> commentPage : commentConnection) {
//  for (Comment comment : commentPage) {
//    System.out.println("Id: " + comment.getId());
//    System.out.println("comment: " + comment.getMessage());
//    personalLimit--;
//
//    // break both loops
////    if (personalLimit == 0) {
////       return;
////    }
//  }
//}
//
//Page page = facebookClient.fetchObject("foodiestoressl", Page.class, 
//        Parameter.with("fields", "fan_count"));
