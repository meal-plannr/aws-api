package com.mealplanner.config;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EnvironmentConfig {

    @Inject
    public EnvironmentConfig() {
    }

    public String getAwsRegion() {
        return System.getenv("region");
    }
}
