package com.mealplanner.dal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mealplanner.config.PropertiesService;
import com.mealplanner.domain.Meal;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Singleton
public class MealRepository {

    public static final String ERROR_TEMPLATE_MULTIPLE_MEALS_FOUND_FOR_ID_AND_USER_ID = "Multiple Meals found for ID [%s] and userID [%s]";
    public static final String ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID = "No Meal found for ID [%s] and user ID [%s]";

    private static final Logger LOGGER = LogManager.getLogger(MealRepository.class);

    private final DynamoDbClient dynamoClient;
    private final PropertiesService properties;

    @Inject
    public MealRepository(final DynamoDbClient dynamoClient, final PropertiesService properties) {
        this.dynamoClient = dynamoClient;
        this.properties = properties;
    }

    public Meal get(final String mealId, final String userId) {
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put("mealId", AttributeValue.builder().s(mealId).build());
        key.put("userId", AttributeValue.builder().s(userId).build());
        final GetItemResponse response = dynamoClient.getItem(GetItemRequest.builder()
                .tableName(properties.getMealsTableName())
                .key(key)
                .build());
        final Map<String, AttributeValue> item = response.item();
        return convertMapToEntity(item);
    }

    public List<Meal> getAllMealsForUser(final String userId) {
        final Map<String, AttributeValue> keys = new HashMap<>();
        keys.put(":userId", AttributeValue.builder().s(userId).build());

        final QueryResponse response = dynamoClient.query(QueryRequest.builder()
                .tableName(properties.getMealsTableName())
                .keyConditionExpression("userId = :userId")
                .expressionAttributeValues(keys)
                .build());

        final List<Meal> meals = new ArrayList<>();
        final List<Map<String, AttributeValue>> entities = response.items();
        for (final Map<String, AttributeValue> entity : entities) {
            meals.add(convertMapToEntity(entity));
        }

        return meals;
    }

    public void delete(final String mealId, final String userId) {
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put("mealId", AttributeValue.builder().s(mealId).build());
        key.put("userId", AttributeValue.builder().s(userId).build());

        dynamoClient.deleteItem(DeleteItemRequest.builder()
                .tableName(properties.getMealsTableName())
                .key(key)
                .build());
    }

    public Meal create() {
        return new Meal();
    }

    public void save(final Meal meal) {
        LOGGER.debug("Saving meal [{}]", meal);

        final Map<String, AttributeValue> key = new HashMap<>();
        key.put("mealId", AttributeValue.builder().s(meal.getId()).build());
        key.put("userId", AttributeValue.builder().s(meal.getUserId()).build());

        final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":description", AttributeValue.builder().s(meal.getDescription()).build());

        dynamoClient.updateItem(UpdateItemRequest.builder()
                .tableName(properties.getMealsTableName())
                .key(key)
                .expressionAttributeValues(expressionAttributeValues)
                .updateExpression("SET description = :description")
                .build());

        LOGGER.info("Saved meal [{}]", meal);
    }

    private Meal convertMapToEntity(final Map<String, AttributeValue> map) {
        final Meal meal = new Meal();
        meal.setId(map.get("mealId").s());
        meal.setUserId(map.get("userId").s());
        meal.setDescription(map.get("description").s());
        return meal;
    }
}
