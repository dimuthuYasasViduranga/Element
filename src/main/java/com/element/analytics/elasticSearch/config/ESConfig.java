package com.element.analytics.elasticSearch.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ESConfig {
	@Value("${elasticsearch.client.hostame}")
	private String hostName;
	
	@Value("${elasticsearch.client.port}")
	private int port;
	
	@Bean(destroyMethod = "close")
	public RestHighLevelClient ElasticClient() {
		RestHighLevelClient elasticClient = new RestHighLevelClient(
		        RestClient.builder(
		                new HttpHost(hostName, port, "http"),
		                new HttpHost("localhost", 9201, "http")));
		return elasticClient;
	}
}
