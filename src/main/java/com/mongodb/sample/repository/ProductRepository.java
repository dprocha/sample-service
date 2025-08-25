package com.mongodb.sample.repository;

import com.mongodb.sample.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String>, CustomMongoRepository<Product> {
}
