package com.mealplanner.config;

import java.io.IOException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.regions.Region;

@Singleton
public class PropertiesService {

    public static final String CI_PROPERTIES_FILENAME = "ci.properties";
    public static final String LOCAL_PROPERTIES_FILENAME = "local.properties";

    public static final String ERROR_UNKNOWN_ENVIRONMENT = "Unknown environment [%s] to get a properties file for";

    public static final String AWS_REGION = "aws.region";
    public static final String DYNAMO_MEALS_TABLE_NAME = "dynamo.mealsTableName";
    public static final String DYNAMO_ENDPOINT = "dynamo.endpoint";

    private static final Logger LOGGER = LogManager.getLogger(PropertiesService.class);

    private final Properties props = new Properties();
    private final Environment environment;

    @Inject
    public PropertiesService(@Named("environment") final Environment environment) {
        LOGGER.debug("Creating PropertiesService instance");

        this.environment = environment;

        if (environment == Environment.PRODUCTION) {
            props.setProperty(AWS_REGION, System.getenv("region"));
            props.setProperty(DYNAMO_MEALS_TABLE_NAME, System.getenv("tableName"));
        } else {
            final String fileName = getPropertiesFilename(environment);
            try {
                LOGGER.debug("Loading properties from file [{}]", fileName);
                props.load(getClass().getClassLoader().getResourceAsStream(fileName));
                LOGGER.debug("Finished loading properties");
            } catch (final IOException e) {
                final String error = String.format("Error loading properties file [%s]", fileName);
                LOGGER.error(error, e);
                throw new IllegalStateException(error, e);
            }
        }

        LOGGER.debug("Finished creating PropertiesService instance");
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

    public Region getAwsRegion() {
        return Region.of(props.getProperty(AWS_REGION));
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
}