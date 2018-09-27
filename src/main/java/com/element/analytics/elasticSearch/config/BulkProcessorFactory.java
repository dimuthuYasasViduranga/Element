package com.element.analytics.elasticSearch.config;

import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class BulkProcessorFactory {
	
	private final RestHighLevelClient elasticClient;
	
	public BulkProcessorFactory(RestHighLevelClient client) {
		this.elasticClient = client;
	}

	@Bean
	public BulkProcessor getBulkProcessor(BulkProcessor.Listener listener) {
		BulkProcessor.Builder builder = BulkProcessor.builder(elasticClient::bulkAsync, listener);
		builder.setBulkActions(500); 
		builder.setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB));
		builder.setConcurrentRequests(0); 
		builder.setFlushInterval(TimeValue.timeValueSeconds(10L)); 
		builder.setBackoffPolicy(BackoffPolicy
		        .constantBackoff(TimeValue.timeValueSeconds(1L), 3));
		
		return builder.build();
	}
}
