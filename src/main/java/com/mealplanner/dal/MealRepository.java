package com.mealplanner.dal;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.mealplanner.domain.Meal;

@Singleton
public class MealRepository {

    public static final String ERROR_TEMPLATE_MULTIPLE_MEALS_FOUND_FOR_ID_AND_USER_ID = "Multiple Meals found for ID [%s] and userID [%s]";
    public static final String ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID = "No Meal found for ID [%s] and user ID [%s]";

    private static final Logger LOGGER = LoggerFactory.getLogger(MealRepository.class);

    private final DynamoDBMapper mapper;
    private final DynamoDbFactory<Meal> dynamoDbFactory;
    private final AmazonKinesis kinesisClient;

    @Inject
    public MealRepository(@Named("mealsDynamoDbMapper") final DynamoDBMapper mapper,
            @Named("mealsDynamoDbFactory") final DynamoDbFactory<Meal> dynamoDbFactory, final AmazonKinesis kinesisClient) {
        this.mapper = mapper;
        this.dynamoDbFactory = dynamoDbFactory;
        this.kinesisClient = kinesisClient;
    }

    public Meal get(final String mealId, final String userId) {
        final Map<String, AttributeValue> attributeValues = dynamoDbFactory.createAttributesMap();
        attributeValues.put(":mealId", dynamoDbFactory.createAttributeValue().withS(mealId));
        attributeValues.put(":userId", dynamoDbFactory.createAttributeValue().withS(userId));

        final DynamoDBQueryExpression<Meal> queryExpression = dynamoDbFactory.createQueryExpression()
                .withKeyConditionExpression("mealId = :mealId and userId = :userId")
                .withExpressionAttributeValues(attributeValues);

        final PaginatedQueryList<Meal> result = mapper.query(Meal.class, queryExpression);
        if (result.size() == 1) {
            return result.get(0);
        } else if (result.isEmpty()) {
            throw new IllegalStateException(String.format(ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID, mealId, userId));
        } else {
            throw new IllegalStateException(String.format(ERROR_TEMPLATE_MULTIPLE_MEALS_FOUND_FOR_ID_AND_USER_ID, mealId, userId));
        }
    }

    public List<Meal> getAllMealsForUser(final String userId) {
        final Map<String, AttributeValue> attributeValues = dynamoDbFactory.createAttributesMap();
        attributeValues.put(":userId", dynamoDbFactory.createAttributeValue().withS(userId));

        final DynamoDBQueryExpression<Meal> queryExpression = dynamoDbFactory.createQueryExpression()
                .withKeyConditionExpression("userId = :userId")
                .withExpressionAttributeValues(attributeValues);

        return mapper.query(Meal.class, queryExpression);
    }

    public void delete(final String mealId, final String userId) {
        final Meal meal = get(mealId, userId);
        mapper.delete(meal);
    }

    public Meal create() {
        return new Meal();
    }

    public void save(final Meal meal) {
        LOGGER.info("Saving meal [{}]", meal);
        mapper.save(meal);

        final PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setStreamName("savedMeals");
        putRecordRequest.setPartitionKey(meal.getUserId());
        putRecordRequest.setData(ByteBuffer.wrap(meal.getId().getBytes()));
        kinesisClient.putRecord(putRecordRequest);
    }
}
