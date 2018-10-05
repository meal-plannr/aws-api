package com.mealplanner.config;

import java.net.URI;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Module
public class DaoModule {

    private static final Logger LOGGER = LogManager.getLogger(DaoModule.class);

    @Singleton
    @Provides
    public DynamoDbClient amazonDynamoDb(final PropertiesService properties, final SdkHttpClient httpClient,
            final AwsCredentialsProvider environmentCredentialsProvider) {
        LOGGER.debug("Creating AmazonDynamoDB instance");

        final DynamoDbClientBuilder builder = DynamoDbClient.builder();

        final String endpoint = properties.getDynamoEndpoint();
        if ((endpoint != null) && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        } else {
            builder.region(properties.getAwsRegion());
        }

        builder.httpClient(httpClient);
        builder.credentialsProvider(environmentCredentialsProvider);

        final DynamoDbClient dynamoDB = builder.build();
        LOGGER.debug("Finished creating AmazonDynamoDB");

        return dynamoDB;
    }

    @Singleton
    @Provides
    public SdkHttpClient httpClient() {
        LOGGER.debug("Creating UrlConnectionHttpClient");
        final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
        LOGGER.debug("Finished creating UrlConnectionHttpClient");
        return httpClient;
    }

    @Singleton
    @Provides
    public AwsCredentialsProvider environmentCredentialsProvider() {
        LOGGER.debug("Creating EnvironmentVariableCredentialsProvider");
        final EnvironmentVariableCredentialsProvider provider = EnvironmentVariableCredentialsProvider.create();
        LOGGER.debug("Finished creating EnvironmentVariableCredentialsProvider");
        return provider;
    }
}
