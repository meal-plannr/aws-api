package com.mealplanner.config;

import javax.inject.Singleton;

import com.mealplanner.function.CreateMealHandler;
import com.mealplanner.function.DeleteMealHandler;
import com.mealplanner.function.GetMealHandler;
import com.mealplanner.function.ListMealsHandler;
import com.mealplanner.function.PutMealHandler;
import com.mealplanner.function.UpdateSavedMealDateHandler;
import com.mealplanner.function.UpdateSavedMealDescriptionHandler;

import dagger.Component;

@Singleton
@Component(modules = { InfrastructureModule.class, DaoModule.class })
public interface AppComponent {

    void inject(ListMealsHandler handler);

    void inject(GetMealHandler handler);

    void inject(CreateMealHandler handler);

    void inject(PutMealHandler handler);

    void inject(DeleteMealHandler handler);

    void inject(UpdateSavedMealDescriptionHandler savedMealKinesis);

    void inject(UpdateSavedMealDateHandler updateSavedMealDateHandler);
}
