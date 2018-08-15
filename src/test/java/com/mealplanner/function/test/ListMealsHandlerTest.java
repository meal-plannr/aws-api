package com.mealplanner.function.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealplanner.config.AppComponent;
import com.mealplanner.config.AppModule;
import com.mealplanner.dal.MealRepository;
import com.mealplanner.domain.Meal;
import com.mealplanner.function.ListMealsHandler;
import com.mealplanner.function.util.ApiGatewayRequest;
import com.mealplanner.function.util.Identity;
import com.mealplanner.function.util.RequestContext;
import com.serverless.ApiGatewayResponse;

import it.cosenonjaviste.daggermock.DaggerMockRule;
import it.cosenonjaviste.daggermock.InjectFromComponent;

@ExtendWith(MockitoExtension.class)
public class ListMealsHandlerTest {

    private static final String USER_ID = "user1";

    //@Rule
    //public static final EnvironmentVariables ENVIRONMENT_VARIABLES = new EnvironmentVariables();

    @Rule
    public DaggerMockRule<AppComponent> rule = new DaggerMockRule<>(AppComponent.class, new AppModule())
            .set(new DaggerMockRule.ComponentSetter<AppComponent>() {
                @Override
                public void setComponent(final AppComponent component) {
                    handler = component.getListMealsHandler();
                }
            });

    @Mock
    private AmazonDynamoDB amazonDynamoDB;

    @Mock
    private MealRepository mealRepository;

    @Mock
    private ApiGatewayRequest request;

    @Mock
    private RequestContext requestContext;

    @Mock
    private Identity identity;

    @Mock
    private Context context;

    //@InjectMocks
    @InjectFromComponent(AppComponent.class)
    private ListMealsHandler handler;

    @BeforeAll
    public static void setEnvVars() {
        //ENVIRONMENT_VARIABLES.set("region", "eu-west-2");
    }

    @Test
    public void all_users_meals_are_returned() throws Exception {
        final List<Meal> meals = Arrays.asList();
        when(mealRepository.getAllMealsForUser(USER_ID)).thenReturn(meals);

        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getIdentity()).thenReturn(identity);
        when(identity.getCognitoIdentityId()).thenReturn(USER_ID);

        final ApiGatewayResponse response = handler.handleRequest(request, context);
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<Meal> actualMeals = objectMapper.readValue(response.getBody(), new TypeReference<List<Meal>>() {
        });

        assertThat(actualMeals).containsExactlyInAnyOrderElementsOf(meals);
    }
}
