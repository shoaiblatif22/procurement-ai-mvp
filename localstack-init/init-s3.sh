#!/bin/bash
# Runs automatically when LocalStack starts
# Creates the S3 bucket for local development

echo "Creating S3 bucket for local development..."
awslocal s3 mb s3://procurement-docs --region eu-west-2
awslocal s3api put-bucket-versioning \
    --bucket procurement-docs \
    --versioning-configuration Status=Enabled
echo "LocalStack S3 bucket ready."
