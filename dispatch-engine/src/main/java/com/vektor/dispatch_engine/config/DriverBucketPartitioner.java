package com.vektor.dispatch_engine.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

public class DriverBucketPartitioner implements Partitioner{

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();

        for (int bucket = 0; bucket < gridSize; bucket++) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("bucket", bucket);
            context.putInt("gridSize", gridSize);
            partitions.put("bucket-" + bucket, context);
        }
        return partitions;
    }
    
}
