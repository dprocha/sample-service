package com.mongodb.sample.controller;

import com.mongodb.sample.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Operations for batch products insertion in MongoDB")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(value = "/batch", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Insert All products using batch process",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Batch insert started"),
                    @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
            }
    )
    public ResponseEntity<Void> batch(
            @Parameter(description = "Total number of products to insert (default is 1,000,000)", example = "1000000")
            @RequestParam(name = "total", required = false, defaultValue = "1000000") int total,
            @Parameter(description = "Batch size for each insert operation (default is 10,000)", example = "10000")
            @RequestParam(name = "batchSize", required = false, defaultValue = "10000") int batchSize) {
        productService.batch(total, batchSize);
        log.info("Batch insert completed");
        return ResponseEntity.noContent().build();
    }
}
