/*
 * Copyright 2020 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.search.index;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component("search_IndexManager")
public class IndexManager {

    private static final Logger log = LoggerFactory.getLogger(IndexManager.class);

    @Autowired
    protected RestHighLevelClient esClient;

    protected ObjectMapper objectMapper = new ObjectMapper();

    public void createIndex(IndexDefinition indexDefinition) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexDefinition.getIndexName());
        String mappingBody;
        try {
            mappingBody = objectMapper.writeValueAsString(indexDefinition.getMapping());
            log.info("[IVGA] Mapping for index '{}': {}", indexDefinition.getIndexName(), mappingBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to create index '" + indexDefinition.getIndexName() + "': Failed to parse index definition", e);
        }
        request.mapping(mappingBody, XContentType.JSON);
        CreateIndexResponse createIndexResponse = esClient.indices().create(request, RequestOptions.DEFAULT);
        log.info("[IVGA] Result of index '{}' creation (ack={}): {}", indexDefinition.getIndexName(), createIndexResponse.isAcknowledged(), createIndexResponse);
    }

    public void dropIndex(String indexName) throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        AcknowledgedResponse response = esClient.indices().delete(request, RequestOptions.DEFAULT);
        log.info("[IVGA] Delete Index Response = {}", response);
    }

    public boolean isIndexExist(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName);
        return esClient.indices().exists(request, RequestOptions.DEFAULT);
    }

    public boolean isIndexActual(IndexDefinition indexDefinition) throws IOException {
        GetIndexResponse index = getIndex(indexDefinition.getIndexName());
        Map<String, MappingMetadata> mappings = index.getMappings();
        MappingMetadata mappingMetadata = mappings.get(indexDefinition.getIndexName());
        Map<String, Object> currentMapping = mappingMetadata.getSourceAsMap();
        log.info("[IVGA] Current mapping of index '{}': {}", indexDefinition.getIndexName(), currentMapping);

        Map<String, Object> actualMapping = objectMapper.convertValue(indexDefinition.getMapping(), new TypeReference<Map<String, Object>>(){});
        log.info("[IVGA] Actual mapping of index '{}': {}", indexDefinition.getIndexName(), actualMapping);

        return actualMapping.equals(currentMapping);
    }

    public GetIndexResponse getIndex(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName);
        return esClient.indices().get(request, RequestOptions.DEFAULT);
    }
}
