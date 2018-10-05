package com.mealplanner.dal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.mealplanner.dal.MealRepository;
import com.mealplanner.domain.Meal;
import com.mealplanner.test.IntegrationTestBase;

import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

public class MealRepositoryIT extends IntegrationTestBase {

    private static final String MEAL1_ID = "meal1";
    private static final String MEAL2_ID = "meal2";
    private static final String MEAL3_ID = "meal3";

    private static final String USER1_ID = "user1";
    private static final String USER2_ID = "user2";

    @Test
    public void new_meal_does_not_have_an_id() {
        final Meal meal = mealRepository.create();

        assertThat(meal.getId()).isNullOrEmpty();
    }

    @Test
    public void meal_is_assigned_an_id_when_saving() {
        final Meal meal = new Meal.Builder().userId(USER1_ID).build();
        mealRepository.save(meal);

        assertThat(meal.getId()).isNotBlank();
    }

    @Test
    public void exception_is_thrown_if_trying_to_retrieve_meal_does_not_match_user() {
        final Meal mealForLoggedInUser = new Meal.Builder().mealId(MEAL1_ID).userId(USER1_ID).build();
        mealRepository.save(mealForLoggedInUser);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> mealRepository.get(MEAL1_ID, USER2_ID))
                .withMessage(MealRepository.ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID, MEAL1_ID, USER2_ID);
    }

    @Test
    public void meal_is_updated_when_saving_meal_with_same_id() {
        final Meal meal1 = new Meal.Builder().mealId(MEAL1_ID).userId(USER1_ID).description("First description").build();
        mealRepository.save(meal1);

        final Meal meal2 = mealRepository.get(MEAL1_ID, USER1_ID);
        final String updatedDescription = "Updated description";
        meal2.setDescription(updatedDescription);
        mealRepository.save(meal2);

        final Meal retrievedMeal = mealRepository.get(MEAL1_ID, USER1_ID);
        assertThat(retrievedMeal.getDescription()).isEqualTo(updatedDescription);
    }

    @Test
    public void all_meals_are_returned_for_a_user() {
        final Meal meal1 = new Meal.Builder().userId(USER1_ID).description("Meal 1").build();
        mealRepository.save(meal1);

        final Meal meal2 = new Meal.Builder().userId(USER1_ID).description("Meal 2").build();
        mealRepository.save(meal2);

        final List<Meal> meals = mealRepository.getAllMealsForUser(USER1_ID);
        assertThat(meals).containsExactlyInAnyOrder(meal1, meal2);
    }

    @Test
    public void only_meals_matching_user_are_returned() {
        final Meal user1Meal1 = new Meal.Builder().userId(USER1_ID).mealId(MEAL1_ID).build();
        mealRepository.save(user1Meal1);

        final Meal user1Meal2 = new Meal.Builder().userId(USER1_ID).mealId(MEAL2_ID).build();
        mealRepository.save(user1Meal2);

        final Meal user2Meal1 = new Meal.Builder().userId(USER2_ID).mealId(MEAL3_ID).build();
        mealRepository.save(user2Meal1);

        final List<Meal> meals = mealRepository.getAllMealsForUser(USER1_ID);
        Assertions.assertThat(meals).containsExactlyInAnyOrder(user1Meal1, user1Meal2);
    }

    @Test
    public void meal_is_deleted_if_found() {
        final Meal user1Meal1 = new Meal.Builder().userId(USER1_ID).mealId(MEAL1_ID).build();
        mealRepository.save(user1Meal1);

        mealRepository.delete(MEAL1_ID, USER1_ID);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> mealRepository.get(MEAL1_ID, USER2_ID))
                .withMessage(MealRepository.ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID, MEAL1_ID, USER2_ID);
    }

    @Test
    public void exception_is_thrown_when_trying_to_delete_a_meal_that_does_not_exist() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> mealRepository.delete(MEAL1_ID, USER1_ID))
                .withMessage(MealRepository.ERROR_TEMPLATE_NO_MEAL_FOUND_FOR_ID_AND_USER_ID, MEAL1_ID, USER1_ID);
    }

    @Test
    public void version_is_set_when_saving_new_entity() {
        final Meal meal = new Meal.Builder().userId(USER1_ID).build();
        mealRepository.save(meal);

        assertThat(meal.getVersion()).isEqualTo(1);
    }

    @Test
    public void version_is_updated_when_saving_existing_entity() {
        final Meal meal = new Meal.Builder().userId(USER1_ID).build();
        mealRepository.save(meal);

        final Meal retrievedMeal = mealRepository.get(meal.getId(), USER1_ID);
        retrievedMeal.setDescription("update description");
        mealRepository.save(retrievedMeal);

        assertThat(retrievedMeal.getVersion()).isEqualTo(2);
    }

    @Test
    public void exception_is_thrown_when_updating_old_version() {
        final Meal meal1 = new Meal.Builder().userId(USER1_ID).description("a meal").build();
        mealRepository.save(meal1);

        final Meal firstRetrievedMeal = mealRepository.get(meal1.getId(), USER1_ID);

        final Meal secondRetrievedMeal = mealRepository.get(meal1.getId(), USER1_ID);
        secondRetrievedMeal.setDescription("an updated meal");
        mealRepository.save(secondRetrievedMeal);

        firstRetrievedMeal.setDescription("an update to a stale meal");
        assertThatExceptionOfType(ConditionalCheckFailedException.class)
                .isThrownBy(() -> mealRepository.save(firstRetrievedMeal));
    }

    @Test
    public void meal_can_be_saved_with_a_date() {
        final LocalDate now = LocalDate.now();
        final Meal meal = new Meal.Builder().userId(USER1_ID).date(now).build();
        mealRepository.save(meal);

        final Meal retrievedMeal = mealRepository.get(meal.getId(), USER1_ID);
        assertThat(retrievedMeal.getDate()).isEqualTo(now);
    }
}