package com.mongodb.sample.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

@Slf4j
public class CustomMongoRepositoryImpl<T> implements CustomMongoRepository<T> {

    private final MongoTemplate mongoTemplate;

    public CustomMongoRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void bulkInsert(List<T> list) {
        Class<T> clazz = (Class<T>) list.get(0).getClass();
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz);
        bulkOps.insert(list).execute();
        log.info("Inserted {} documents into collection {}", list.size(), mongoTemplate.getCollectionName(clazz));
    }
}