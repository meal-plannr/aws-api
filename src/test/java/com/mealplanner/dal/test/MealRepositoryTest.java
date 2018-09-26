package com.mealplanner.dal.test;

import static com.mealplanner.dal.MealRepository.ERROR_TEMPLATE_MULTIPLE_MEALS_FOUND_FOR_ID_AND_USER_ID;
import static com.mealplanner.dal.MealRepository.ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.mealplanner.config.PropertiesService;
import com.mealplanner.dal.DynamoDbFactory;
import com.mealplanner.dal.MealRepository;
import com.mealplanner.domain.Meal;

@ExtendWith(MockitoExtension.class)
public class MealRepositoryTest {

    private static final String MEAL_ID = "meal1";
    private static final String USER_ID = "user1";

    private final AttributeValue mealIdAttribute = new AttributeValue();
    private final AttributeValue userIdAttribute = new AttributeValue();
    private final DynamoDBQueryExpression<Meal> queryExpression = new DynamoDBQueryExpression<Meal>();

    @Mock
    private DynamoDBMapper mapper;

    @Mock
    private DynamoDbFactory<Meal> dynamoDbFactory;

    @Mock
    private AmazonKinesis kinesisClient;

    @Mock
    private PropertiesService propertiesService;

    @Mock
    private PaginatedQueryList<Meal> paginatedQueryList;

    @Mock
    private Meal mockMeal;

    @Mock
    private Map<String, AttributeValue> attributeMap;

    @InjectMocks
    private MealRepository mealRepository;

    @Test
    public void user_id_and_meal_id_are_used_to_retrieve_meal() {
        setupGetMealMethod();

        when(paginatedQueryList.size()).thenReturn(1);
        when(paginatedQueryList.get(0)).thenReturn(mockMeal);

        mealRepository.get(MEAL_ID, USER_ID);

        verify(mapper).query(Meal.class, queryExpression);
        assertThat(queryExpression.getExpressionAttributeValues()).isEqualTo(attributeMap);
        verify(attributeMap).put(":mealId", mealIdAttribute);
        verify(attributeMap).put(":userId", userIdAttribute);
        assertThat(queryExpression.getKeyConditionExpression()).isEqualTo("mealId = :mealId and userId = :userId");
    }

    @Test
    public void meal_is_returned_if_one_result_is_found() {
        setupGetMealMethod();

        when(paginatedQueryList.size()).thenReturn(1);
        when(paginatedQueryList.get(0)).thenReturn(mockMeal);

        final Meal meal = mealRepository.get(MEAL_ID, USER_ID);

        Assertions.assertThat(meal).isEqualTo(mockMeal);
    }

    @Test
    public void exception_is_thrown_if_no_meal_found_for_user_and_id() {
        setupGetMealMethod();

        when(paginatedQueryList.isEmpty()).thenReturn(true);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> mealRepository.get(MEAL_ID, USER_ID))
                .withMessage(ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID, MEAL_ID, USER_ID);
    }

    @Test
    public void exception_is_thrown_if_multiple_meals_found_for_user_and_id() {
        setupGetMealMethod();

        when(paginatedQueryList.size()).thenReturn(2);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> mealRepository.get(MEAL_ID, USER_ID))
                .withMessage(ERROR_TEMPLATE_MULTIPLE_MEALS_FOUND_FOR_ID_AND_USER_ID, MEAL_ID, USER_ID);
    }

    @Test
    public void user_id_is_used_to_retrieve_meals() {
        when(dynamoDbFactory.createAttributesMap()).thenReturn(attributeMap);
        when(dynamoDbFactory.createAttributeValue()).thenReturn(userIdAttribute);
        when(dynamoDbFactory.createQueryExpression()).thenReturn(queryExpression);

        mealRepository.getAllMealsForUser(USER_ID);

        verify(mapper).query(Meal.class, queryExpression);
        assertThat(queryExpression.getExpressionAttributeValues()).isEqualTo(attributeMap);
        verify(attributeMap).put(":userId", userIdAttribute);
        assertThat(queryExpression.getKeyConditionExpression()).isEqualTo("userId = :userId");
    }

    @Test
    public void meal_is_deleted_if_found() {
        setupGetMealMethod();

        when(paginatedQueryList.size()).thenReturn(1);
        when(paginatedQueryList.get(0)).thenReturn(mockMeal);

        mealRepository.delete(MEAL_ID, USER_ID);

        verify(mapper).delete(mockMeal);
    }

    @Test
    public void exception_is_thrown_when_deleting_a_meal_that_does_not_exist() {
        setupGetMealMethod();
        when(paginatedQueryList.isEmpty()).thenReturn(true);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> mealRepository.delete(MEAL_ID, USER_ID))
                .withMessage(ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID, MEAL_ID, USER_ID);
    }

    @Test
    public void meal_is_saved() {
        mealRepository.save(mockMeal);

        verify(mapper).save(mockMeal);
    }

    @Test
    public void created_meal_has_no_populated_fields() {
        final Meal createdMeal = mealRepository.create();
        assertAll(() -> assertThat(createdMeal.getId()).isNullOrEmpty(),
                () -> assertThat(createdMeal.getUserId()).isNullOrEmpty(),
                () -> assertThat(createdMeal.getDescription()).isNullOrEmpty());
    }

    private void setupGetMealMethod() {
        when(dynamoDbFactory.createAttributesMap()).thenReturn(attributeMap);
        when(dynamoDbFactory.createAttributeValue()).thenReturn(mealIdAttribute, userIdAttribute);

        when(dynamoDbFactory.createQueryExpression()).thenReturn(queryExpression);

        when(mapper.query(Mockito.eq(Meal.class), Mockito.eq(queryExpression))).thenReturn(paginatedQueryList);
    }
}