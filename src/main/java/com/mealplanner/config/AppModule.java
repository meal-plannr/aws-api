package com.mealplanner.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private static final String AWS_REGION = System.getenv("region");

    @Provides
    public AmazonDynamoDB providesAmazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(AWS_REGION)
                .build();
    }
}
