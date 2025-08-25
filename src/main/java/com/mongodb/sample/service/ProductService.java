package com.mongodb.sample.service;

import com.mongodb.sample.model.Product;
import com.mongodb.sample.repository.ProductRepository;
import com.mongodb.sample.utils.ObjectIdConverter;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ProductService {

    private final int maxInflightBatches = 100; // Default max inflight batches for parallel generation

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void batch(int total, int batchSize) {
        log.info("Generating 1M products on MongoDB using custom ObjectId generator UUID...");

        long start = System.currentTimeMillis();
        final int batches = Math.toIntExact((total + (long) batchSize - 1) / batchSize);

        log.info("Generating Products from {} of total {}", batchSize, total);

        // Virtual thread executor
        try (ExecutorService vt = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {

            final Semaphore limiter = new Semaphore(Math.max(1, maxInflightBatches));
            final List<CompletableFuture<Void>> all = new ArrayList<>(batches);
            final AtomicLong progress = new AtomicLong(0);

            for (int b = 0; b < batches; b++) {
                final int batchIndex = b;
                try {
                    limiter.acquire(); // backpressure
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }

                CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                    try {
                        int toCreate = Math.min(batchSize, total - (batchIndex * batchSize));
                        List<Product> products = new ArrayList<>(toCreate);
                        for (int i = 0; i < toCreate; i++) {
                            //Product product = RandomDataGenerator.generateRandomObject(Product.class);
                            Product product = new Product();
                            ObjectId objectId = new ObjectId();
                            log.info("Generated ObjectId: {}", objectId.toHexString());
                            product.setId(objectId.toHexString());
                            product.setId1(ObjectIdConverter.generateLong(objectId));
                            product.setId2(ObjectIdConverter.generateBigInteger(objectId));
                            log.info("id: {}, id1: {}, id2: {}", product.getId(), product.getId1(), product.getId2());
                            products.add(product);
                        }
                        // bulk insert
                        productRepository.bulkInsert(products);

                        long done = progress.addAndGet(toCreate);
                        if (batchIndex % 10 == 0 || done == total) {
                            log.info("Progress {} of total {} ", done, total);
                        }
                    } finally {
                        limiter.release();
                    }
                }, vt);

                all.add(cf);
            }
            // wait all
            CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).join();
        }

        long end = System.currentTimeMillis();
        log.info("Finished generating products in {}", (end - start) / 1000.0);
    }

}
