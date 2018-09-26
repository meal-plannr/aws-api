package com.mealplanner.dal;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mealplanner.config.PropertiesService;
import com.mealplanner.domain.Meal;

@Singleton
public class MealRepository {

    public static final String ERROR_TEMPLATE_MULTIPLE_MEALS_FOUND_FOR_ID_AND_USER_ID = "Multiple Meals found for ID [%s] and userID [%s]";
    public static final String ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID = "No Meal found for ID [%s] and user ID [%s]";

    private static final Logger LOGGER = LogManager.getLogger(MealRepository.class);

    private final DynamoDBMapper mapper;
    private final DynamoDbFactory<Meal> dynamoDbFactory;
    private final AmazonKinesis kinesisClient;
    private final PropertiesService properties;

    @Inject
    public MealRepository(@Named("mealsDynamoDbMapper") final DynamoDBMapper mapper,
            @Named("mealsDynamoDbFactory") final DynamoDbFactory<Meal> dynamoDbFactory, final AmazonKinesis kinesisClient, final PropertiesService properties) {
        this.mapper = mapper;
        this.dynamoDbFactory = dynamoDbFactory;
        this.kinesisClient = kinesisClient;
        this.properties = properties;
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
        LOGGER.debug("Saving meal [{}]", meal);
        mapper.save(meal);

        LOGGER.info("Saved meal [{}]", meal);
    }

    public void saveAndSendMessage(final Meal meal) {
        save(meal);
        LOGGER.trace("Creating Kinesis record");
        final PutRecordRequest putRecordRequest = new PutRecordRequest();
        final String streamName = properties.getSavedMealsStreamName();
        LOGGER.debug("Stream name [{}]", streamName);
        putRecordRequest.setStreamName(streamName);
        putRecordRequest.setPartitionKey(meal.getUserId());
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("mealId", meal.getId());
        node.put("userId", meal.getUserId());
        LOGGER.debug("About to send Kinesis record for meal [{}] and user [{}]", meal.getId(), meal.getUserId());
        try {
            final byte[] data = objectMapper.writeValueAsBytes(node);
            putRecordRequest.setData(ByteBuffer.wrap(data));
        } catch (final JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        kinesisClient.putRecord(putRecordRequest);
        LOGGER.debug("Sent Kinesis record for meal [{}] and user [{}]", meal.getId(), meal.getUserId());
    }
}
