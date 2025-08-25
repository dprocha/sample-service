package com.mongodb.sample.repository;

import java.util.List;

public interface CustomMongoRepository<T> {

    void bulkInsert(List<T> list);
}
