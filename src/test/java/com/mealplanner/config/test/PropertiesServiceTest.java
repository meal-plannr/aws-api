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

import software.amazon.awssdk.regions.Region;

@ExtendWith(MockitoExtension.class)
public class PropertiesServiceTest {

    @Rule
    public static final EnvironmentVariables ENVIRONMENT_VARIABLES = new EnvironmentVariables();

    @BeforeAll
    public static void setEnvVars() {
        ENVIRONMENT_VARIABLES.set("region", Region.AWS_GLOBAL.id());
        ENVIRONMENT_VARIABLES.set("tableName", "the-table");
    }

    @Test
    public void production_values_take_precedence_over_everything() {
        final PropertiesService service = new PropertiesService(Environment.PRODUCTION);

        final Properties props = new Properties.Builder()
                .awsRegion(Region.AWS_GLOBAL)
                .mealsTableName("the-table")
                .dynamoEndpoint(null)
                .isLocalEnvironment(false)
                .build();
        assertPropertyValues(service, props);
    }

    @Test
    public void ci_values_take_precendence_over_local() {
        final PropertiesService service = new PropertiesService(Environment.CI);

        final Properties props = new Properties.Builder()
                .awsRegion(Region.EU_WEST_2)
                .mealsTableName("ci-meals")
                .dynamoEndpoint(null)
                .isLocalEnvironment(false)
                .build();
        assertPropertyValues(service, props);
    }

    @Test
    public void local_values_are_used_by_default() {
        final PropertiesService service = new PropertiesService(Environment.LOCAL);

        final Properties props = new Properties.Builder()
                .awsRegion(Region.EU_WEST_1)
                .mealsTableName("meals")
                .dynamoEndpoint("http://localhost:4569")
                .isLocalEnvironment(true)
                .build();
        assertPropertyValues(service, props);
    }

    private final void assertPropertyValues(final PropertiesService service, final Properties expectedProperties) {
        assertAll(() -> assertThat(service.getAwsRegion()).isEqualTo(expectedProperties.getAwsRegion()),
                () -> assertThat(service.getMealsTableName()).isEqualTo(expectedProperties.getMealsTableName()),
                () -> assertThat(service.getDynamoEndpoint()).isEqualTo(expectedProperties.getDynamoEndpoint()),
                () -> assertThat(service.isLocalEnvironment()).isEqualTo(expectedProperties.isLocalEnvironment()));
    }

    static class Properties {
        private final Region awsRegion;
        private final String mealsTableName;
        private final String dynamoEndpoint;
        private final boolean isLocalEnvironment;

        public Properties(final Builder builder) {
            this.awsRegion = builder.awsRegion;
            this.mealsTableName = builder.mealsTableName;
            this.dynamoEndpoint = builder.dynamoEndpoint;
            this.isLocalEnvironment = builder.isLocalEnvironment;
        }

        public Region getAwsRegion() {
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

        static class Builder {
            private Region awsRegion;
            private String mealsTableName;
            private String dynamoEndpoint;
            private boolean isLocalEnvironment;

            public Builder awsRegion(final Region awsRegion) {
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

            public Properties build() {
                return new Properties(this);
            }

        }
    }
}