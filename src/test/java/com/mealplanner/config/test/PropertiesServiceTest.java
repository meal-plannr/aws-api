package com.mealplanner.config.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mealplanner.config.Environment;
import com.mealplanner.config.PropertiesService;

@ExtendWith(MockitoExtension.class)
public class PropertiesServiceTest {

    @Rule
    public static final EnvironmentVariables ENVIRONMENT_VARIABLES = new EnvironmentVariables();

    @BeforeAll
    public static void setEnvVars() {
        ENVIRONMENT_VARIABLES.set("region", "my-region");
        ENVIRONMENT_VARIABLES.set("tableName", "the-table");
        ENVIRONMENT_VARIABLES.set("savedMealsStreamName", "the-saved-meals-stream");
    }

    @Test
    public void production_values_take_precedence_over_everything() {
        final PropertiesService service = new PropertiesService(Environment.PRODUCTION);

        final Properties props = new Properties.Builder()
                .awsRegion("my-region")
                .mealsTableName("the-table")
                .dynamoEndpoint(null)
                .isLocalEnvironment(false)
                .kinesisEndPoint(null)
                .savedMealsStreamName("the-saved-meals-stream")
                .build();
        assertPropertyValues(service, props);
    }

    @Test
    public void ci_values_take_precendence_over_local() {
        final PropertiesService service = new PropertiesService(Environment.CI);

        final Properties props = new Properties.Builder()
                .awsRegion("eu-west-2")
                .mealsTableName("ci-meals")
                .dynamoEndpoint(null)
                .isLocalEnvironment(false)
                .kinesisEndPoint(null)
                .savedMealsStreamName("ci-saved-meals")
                .build();
        assertPropertyValues(service, props);
    }

    @Test
    public void local_values_are_used_by_default() {
        final PropertiesService service = new PropertiesService(Environment.LOCAL);

        final Properties props = new Properties.Builder()
                .awsRegion("eu-west-1")
                .mealsTableName("meals")
                .dynamoEndpoint("http://localhost:4569")
                .isLocalEnvironment(true)
                .kinesisEndPoint("http://localhost:4568")
                .savedMealsStreamName("saved-meals")
                .build();
        assertPropertyValues(service, props);
    }

    private final void assertPropertyValues(final PropertiesService service, final Properties expectedProperties) {
        assertAll(() -> assertThat(service.getAwsRegion()).isEqualTo(expectedProperties.getAwsRegion()),
                () -> assertThat(service.getMealsTableName()).isEqualTo(expectedProperties.getMealsTableName()),
                () -> assertThat(service.getDynamoEndpoint()).isEqualTo(expectedProperties.getDynamoEndpoint()),
                () -> assertThat(service.isLocalEnvironment()).isEqualTo(expectedProperties.isLocalEnvironment()),
                () -> assertThat(service.getKinesisEndpoint()).isEqualTo(expectedProperties.getKinesisEndpoint()),
                () -> assertThat(service.getSavedMealsStreamName()).isEqualTo(expectedProperties.getSavedMealsStreamName()));
    }

    static class Properties {
        private final String awsRegion;
        private final String mealsTableName;
        private final String dynamoEndpoint;
        private final boolean isLocalEnvironment;
        private final String kinesisEndpoint;
        private final String savedMealsStreamName;

        public Properties(final Builder builder) {
            this.awsRegion = builder.awsRegion;
            this.mealsTableName = builder.mealsTableName;
            this.dynamoEndpoint = builder.dynamoEndpoint;
            this.isLocalEnvironment = builder.isLocalEnvironment;
            this.kinesisEndpoint = builder.kinesisEndpoint;
            this.savedMealsStreamName = builder.savedMealsStreamName;
        }

        public String getAwsRegion() {
            return awsRegion;
        }

        public String getMealsTableName() {
            return mealsTableName;
        }

        public String getDynamoEndpoint() {
            return dynamoEndpoint;
        }

        public boolean isLocalEnvironment() {
            return isLocalEnvironment;
        }

        public String getKinesisEndpoint() {
            return kinesisEndpoint;
        }

        public String getSavedMealsStreamName() {
            return savedMealsStreamName;
        }

        static class Builder {
            private String awsRegion;
            private String mealsTableName;
            private String dynamoEndpoint;
            private boolean isLocalEnvironment;
            private String kinesisEndpoint;
            private String savedMealsStreamName;

            public Builder awsRegion(final String awsRegion) {
                this.awsRegion = awsRegion;
                return this;
            }

            public Builder mealsTableName(final String mealsTableName) {
                this.mealsTableName = mealsTableName;
                return this;
            }

            public Builder dynamoEndpoint(final String dynamoEndpoint) {
                this.dynamoEndpoint = dynamoEndpoint;
                return this;
            }

            public Builder isLocalEnvironment(final boolean isLocalEnvironment) {
                this.isLocalEnvironment = isLocalEnvironment;
                return this;
            }

            public Builder kinesisEndPoint(final String kinesisEndpoint) {
                this.kinesisEndpoint = kinesisEndpoint;
                return this;
            }

            public Builder savedMealsStreamName(final String savedMealsStreamName) {
                this.savedMealsStreamName = savedMealsStreamName;
                return this;
            }

            public Properties build() {
                return new Properties(this);
            }

        }
    }
}