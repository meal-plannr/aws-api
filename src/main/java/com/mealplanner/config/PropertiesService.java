package com.mealplanner.config;

import java.io.IOException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Singleton
public class PropertiesService {

    public static final String CI_PROPERTIES_FILENAME = "ci.properties";
    public static final String LOCAL_PROPERTIES_FILENAME = "local.properties";

    public static final String ERROR_UNKNOWN_ENVIRONMENT = "Unknown environment [%s] to get a properties file for";

    public static final String AWS_REGION = "aws.region";
    public static final String DYNAMO_MEALS_TABLE_NAME = "dynamo.mealsTableName";
    public static final String DYNAMO_ENDPOINT = "dynamo.endpoint";
    public static final String KINESIS_ENDPOINT = "kinesis.endpoint";
    public static final String KINESIS_SAVED_MEALS_STREAM_NAME = "kinesis.savedMealsStreamName";

    private static final Logger LOGGER = LogManager.getLogger(PropertiesService.class);

    private final Properties props = new Properties();
    private final Environment environment;

    @Inject
    public PropertiesService(@Named("environment") final Environment environment) {
        this.environment = environment;

        if (environment == Environment.PRODUCTION) {
            props.setProperty(AWS_REGION, System.getenv("region"));
            props.setProperty(DYNAMO_MEALS_TABLE_NAME, System.getenv("tableName"));
            props.setProperty(KINESIS_SAVED_MEALS_STREAM_NAME, System.getenv("savedMealsStreamName"));
        } else {
            final String fileName = getPropertiesFilename(environment);
            try {
                props.load(getClass().getClassLoader().getResourceAsStream(fileName));
            } catch (final IOException e) {
                final String error = String.format("Error loading properties file [%s]", fileName);
                LOGGER.error(error, e);
                throw new IllegalStateException(error, e);
            }
        }
    }

    private String getPropertiesFilename(final Environment environment) {
        switch (environment) {
        case CI:
            return CI_PROPERTIES_FILENAME;
        case LOCAL:
            return LOCAL_PROPERTIES_FILENAME;
        default:
            throw new IllegalStateException(String.format(ERROR_UNKNOWN_ENVIRONMENT, environment));
        }
    }

    public String getAwsRegion() {
        return props.getProperty(AWS_REGION);
    }

    public String getMealsTableName() {
        return props.getProperty(DYNAMO_MEALS_TABLE_NAME);
    }

    public String getDynamoEndpoint() {
        return props.getProperty(DYNAMO_ENDPOINT);
    }

    public boolean isLocalEnvironment() {
        return environment == Environment.LOCAL;
    }

    public String getKinesisEndpoint() {
        return props.getProperty(KINESIS_ENDPOINT);
    }

    public String getSavedMealsStreamName() {
        return props.getProperty(KINESIS_SAVED_MEALS_STREAM_NAME);
    }
}