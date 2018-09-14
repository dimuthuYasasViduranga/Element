package com.element.analytics.elasticSearch.config.controller;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.element.analytics.elasticSearch.Dao.QueryDAO;

@RestController
public class ElasticController {
	
	@Autowired
    private QueryDAO dao;
	
	@PostMapping(value = "/api/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public IndexResponse create(@RequestBody String document) {
		JSONObject root = null;
		try {
			root = new JSONObject(document);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dao.createIndex(root);
	}
	
	@PostMapping(value = "/api/test")
	public String test(HttpServletRequest request) {
		String likes = request.getParameter("likes");
		String shares = request.getParameter("shares");
		String postId = request.getParameter("postId");
		String pageId = request.getParameter("pageId");
		String comments = request.getParameter("comments");
		
		return (likes + shares + postId + pageId + comments);
	}
	
	@GetMapping(value = "/api/get")
	public GetResponse getPostData() {
		return dao.getPostData();
	}
	
}
