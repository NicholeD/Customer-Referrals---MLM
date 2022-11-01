#!/bin/bash
set -eo pipefail
TEMPLATE=ReferralService-template.yml

./gradlew :ReferralServiceLambda:build -i

echo "Deleting Application UNIT_FIVE_APPLICATION_STACK"
echo "This may take 2-3 minutes...  But if takes more than 5 minutes then it may have failed. Check your CloudFormation Stack on the AWS UI for errors."
aws cloudformation delete-stack --stack-name $UNIT_FIVE_SERVICE_STACK_DEV
aws cloudformation wait stack-delete-complete --stack-name $UNIT_FIVE_SERVICE_STACK_DEV

aws cloudformation package --template-file $TEMPLATE --s3-bucket $UNIT_FIVE_ARTIFACT_BUCKET --output-template-file referral-service-development.yml
aws cloudformation deploy --template-file referral-service-development.yml --stack-name $UNIT_FIVE_SERVICE_STACK_DEV --capabilities CAPABILITY_NAMED_IAM
