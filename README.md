# aws-api
API implementation based on AWS Lambdas

## Status

[![Test coverage](https://sonarcloud.io/api/project_badges/measure?project=aws-api&metric=coverage)](https://sonarcloud.io/dashboard?id=aws-api) [![Quality](https://sonarcloud.io/api/project_badges/measure?project=aws-api&metric=alert_status)](https://sonarcloud.io/dashboard?id=aws-api) [![Tech debt](https://sonarcloud.io/api/project_badges/measure?project=aws-api&metric=sqale_index)](https://sonarcloud.io/dashboard?id=aws-api)

## Running integration tests

The integration tests rely on [Localstack](https://github.com/localstack/localstack) to provide mocked AWS services. Running tests does not automatically start Localstack, you need to run it locally.

### Running Localstack

1. Install Localstack

   `pip install localstack` or

   `pip install --user localstack` on Mac

2. Start Localstack

   `ENTRYPOINT=-d SERVICES=dynamodb,kinesis localstack start --docker`

   `ENTRYPOINT=-d` runs Localstack as a daemon in the background

## Running CircleCI locally

It's possible to run the CircleCI build locally so you can verify changes to the config/CI specific changes.

1. Install [CircleCI Local CLI](https://circleci.com/docs/2.0/local-cli/)

2. Setup circleci

    `circleci setup`

3. Run

    `circleci local execute -e AWS_ACCESS_KEY_ID=<access key ID> -e AWS_SECRET_ACCESS_KEY=<secret key>`

    Replace `<access key ID>` and `<secret key>` with values for an account that has access to deploy the Serverless infrastructure
