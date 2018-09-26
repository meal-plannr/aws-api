package com.mealplanner.config;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;

import dagger.Module;
import dagger.Provides;

@Module
public class InfrastructureModule {

    @Singleton
    @Provides
    @Named("environment")
    Environment environment() {
        final String environmentEnvVar = System.getenv("MEAL_PLANNR_ENVIRONMENT");
        return environmentEnvVar != null ? Environment.getEnvironmentForCode(environmentEnvVar) : Environment.LOCAL;
    }

    @Singleton
    @Provides
    AmazonKinesis kinesisClient(final PropertiesService properties) {
        final AmazonKinesisClientBuilder builder = AmazonKinesisClientBuilder.standard();
        final String awsRegion = properties.getAwsRegion();
        final String endpoint = properties.getKinesisEndpoint();
        if (StringUtils.isNotBlank(endpoint)) {
            builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, awsRegion));
        } else {
            builder.withRegion(awsRegion);
        }
        return builder.build();
    }
}
