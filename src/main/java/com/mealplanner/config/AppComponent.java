package com.mealplanner.config;

import javax.inject.Singleton;

import com.mealplanner.dal.DynamoDbAdapter;
import com.mealplanner.dal.MealRepository;
import com.mealplanner.function.ListMealsHandler;

import dagger.Component;

@Singleton
@Component(modules = { AppModule.class })
public interface AppComponent {

    DynamoDbAdapter getDynamoDbAdapter();

    MealRepository getMealRepository();

    EnvironmentConfig getEnvironmentConfig();

    ListMealsHandler getListMealsHandler();
}
