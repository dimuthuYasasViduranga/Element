package com.element.analytics.elasticSearch.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.element.analytics.elasticSearch.Dao.FacebookDAO;
import com.element.analytics.elasticSearch.Dao.QueryDAO;
import com.element.analytics.elasticSearch.Dao.UserDao;
import com.element.analytics.emotionClassifier.VectorSpaceModelTester;

@RestController
public class ElasticController {
	
	@Autowired
    private QueryDAO queryDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private FacebookDAO facebookDao;
	
	@PostMapping(value = "/api/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Result create(@RequestBody String document) {
		JSONObject root = null;
		try {
			root = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queryDao.createIndex(root);
	}
	
	@GetMapping(value = "/api/test")
	public boolean test() throws IOException, JSONException {
		return facebookDao.performSuggestionExtraction("315719342564507_318957702240671");
	}
	
	// @CrossOrigin(origins = "http://localhost:3000")
	@PostMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public String createUsers(@RequestBody String document) throws JSONException {
		JSONObject user = null;
		try {
			user = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return userDao.createUser(user);
	}
	

	@PostMapping(value = "/api/getEmCategry",consumes = "text/plain")
	public String getEmotionCategory(@RequestBody String payload) {
		System.out.println(payload);
		VectorSpaceModelTester vst = new VectorSpaceModelTester();
		return vst.classifyComments(payload);

	}

	@PostMapping(value = "/api/validate-user", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public String validateUser(@RequestBody String document) throws JSONException, IOException {
		JSONObject credentials = null;
		try {
			credentials = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return userDao.VerifyPassword(credentials);
	}
	
	@GetMapping(value = "/api/get")
	public GetResponse getPostData() {
		return queryDao.getPostData();
	}
	
	@PostMapping(value = "/api/addPage", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addPage(@RequestBody String document) throws JSONException {
		JSONObject userData = null;
		try {
			userData = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return facebookDao.savePage(userData);
	}
	
	@PostMapping(value = "/api/getPages", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public String getPages(@RequestBody String document) throws JSONException, IOException {
		JSONObject userData = null;
		try {
			userData = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return facebookDao.getPages(userData);
	}
	
	@PostMapping(value = "/api/getPosts", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public String getPosts(@RequestBody String document) throws JSONException, IOException {
		JSONObject requestData = null;
		try {
			requestData = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return facebookDao.getPosts(requestData);
	}
	
	@PostMapping(value = "/api/getPostDetails", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public String getPostData(@RequestBody String document) throws JSONException, IOException {
		JSONObject requestData = null;
		try {
			requestData = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return facebookDao.getPostData(requestData);
	}
}

