package com.mongodb.sample.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigInteger;

@Data
@Document(collection = "products")
@CompoundIndexes({
        @CompoundIndex(name = "unique_id1", def = "{'id1': 1}", unique = true),
        @CompoundIndex(name = "unique_id2", def = "{'id2': 1}", unique = true)
})
public class Product {

    private String id;

    @Indexed
    @Field(targetType = FieldType.INT64)
    private Long id1;

    @Indexed
    @Field(targetType = FieldType.DECIMAL128)
    private BigInteger id2;

}