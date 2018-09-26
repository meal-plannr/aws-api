package com.mealplanner.test.pipelines;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.mealplanner.domain.Meal;
import com.mealplanner.test.IntegrationTestBase;

@EnabledIfEnvironmentVariable(named = "MEAL_PLANNR_ENVIRONMENT", matches = "ci")
public class SavingMealPipelineIT extends IntegrationTestBase {
    @Test
    public void meal_is_saved_to_database_and_updated_asynchronously() {
        final String userId = "user1";
        final Meal meal = new Meal.Builder().userId(userId).build();
        mealRepository.saveAndSendMessage(meal);
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(getMealDescription(meal)).isEqualTo("UPDATED"));
        final LocalDate now = LocalDate.now();
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(getMealDate(meal)).isBeforeOrEqualTo(now));
    }

    private String getMealDescription(final Meal meal) {
        return mealRepository.get(meal.getId(), meal.getUserId()).getDescription();
    }

    private LocalDate getMealDate(final Meal meal) {
        return mealRepository.get(meal.getId(), meal.getUserId()).getDate();
    }
}