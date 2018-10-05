package com.mealplanner.test;

import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;

import com.mealplanner.config.PropertiesService;
import com.mealplanner.dal.MealRepository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class IntegrationTestBase {

    private static final Logger LOGGER = LogManager.getLogger(IntegrationTestBase.class);

    private static boolean localSetupComplete = false;

    private final AppTestComponent appComponent;

    @Inject
    protected MealRepository mealRepository;

    @Inject
    PropertiesService properties;

    @Inject
    protected DynamoDbClient amazonDynamoDb;

    public IntegrationTestBase() {
        appComponent = DaggerAppTestComponent.builder().build();
        appComponent.inject(this);
    }

    @BeforeEach
    public void setup() throws Exception {
        if (properties.isLocalEnvironment() && !localSetupComplete) {
            LOGGER.debug("Starting local setup");

            createMealsTable();

            localSetupComplete = true;
            LOGGER.debug("Local setup complete");
        }

        deleteMeals();
    }

    private void createMealsTable() throws Exception {
        LOGGER.debug("Creating meals table if it doesn't already exist");

        final String mealsTableName = properties.getMealsTableName();

        TableUtils.createTableIfNotExists(amazonDynamoDb, CreateTableRequest.builder()
                .tableName(mealsTableName)
                .keySchema(
                        KeySchemaElement.builder()
                                .keyType(KeyType.HASH)
                                .attributeName("userId")
                                .build(),
                        KeySchemaElement.builder()
                                .keyType(KeyType.RANGE)
                                .attributeName("mealId")
                                .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("userId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("mealId")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(1L)
                        .writeCapacityUnits(1L)
                        .build())
                .build());

        TableUtils.waitUntilActive(amazonDynamoDb, mealsTableName, 5000, 100);

        LOGGER.debug("Meals table created");
    }

    private void deleteMeals() {
        LOGGER.debug("Deleting all meals");

        final ScanResponse response = amazonDynamoDb.scan(ScanRequest.builder()
                .tableName(properties.getMealsTableName())
                .attributesToGet("mealId", "userId")
                .build());

        for (final Map<String, AttributeValue> entity : response.items()) {
            amazonDynamoDb.deleteItem(DeleteItemRequest.builder()
                    .tableName(properties.getMealsTableName())
                    .key(entity)
                    .build());
        }
    }
}