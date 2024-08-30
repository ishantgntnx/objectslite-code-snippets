"""
This module provides utility functions for uploading files to the objects-lite using the AWS SDK for Python (Boto3).
It includes functions for creating an S3 client, configuring transfer settings, and performing uploads with support for
multipart uploads and concurrent uploads.

Functions:
- get_credentials: Prompt for username and password, then encode them in Base64.
- create_s3_client: Create an S3 client with the given endpoint and credentials.
- create_transfer_config: Create a transfer configuration for multipart uploads.
- upload_file: Upload a file to the specified S3 bucket with the given configuration.
- put_object: Upload a file to S3 using put_object.
- upload_part: Upload a single part to S3.
- multipart_upload: Perform a multipart upload to S3.
- concurrent_multipart_upload: Perform a multipart upload to S3 with concurrency.

Dependencies:
- boto3: AWS SDK for Python
- urllib3: HTTP library with thread-safe connection pooling

Usage:
Import the module and call the desired functions with appropriate arguments.

Example:
    import utils
    credentials = utils.get_credentials()
    s3_client = utils.create_s3_client(endpoint, credentials)
    put_object(s3_client, file, bucket, key)
"""

import argparse
import base64
import boto3
import getpass
from botocore.client import Config
from boto3.s3.transfer import TransferConfig
from concurrent.futures import ThreadPoolExecutor, as_completed
import os
import urllib3

# Disable SSL verification warning
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def get_credentials():
    """Prompt for username and password, then encode them in Base64."""
    username = input("Enter username: ").strip()
    password = getpass.getpass("Enter password: ").strip()
    credentials = f"{username}:{password}"
    return base64.b64encode(credentials.encode()).decode()

def create_s3_client(endpoint, encoded_credentials):
    """Create an S3 client with the given endpoint and credentials."""
    session = boto3.session.Session()
    return session.client(
        's3',
        endpoint_url=endpoint,
        aws_access_key_id=encoded_credentials,
        aws_secret_access_key=encoded_credentials,
        verify=False  # Disable SSL verification
    )

def put_object(s3_client, file_path, bucket, key):
    """Upload a file to S3 using put_object."""
    try:
        with open(file_path, 'rb') as file_data:
            response = s3_client.put_object(Bucket=bucket, Key=key, Body=file_data)
        print("File uploaded successfully to objectslite")
        print(f"HTTP Status Code: {response['ResponseMetadata']['HTTPStatusCode']}")
        print(f"ETag: {response['ETag']}")
    except Exception as e:
        print(f"Failed to upload file to objectslite with error: {e}")

def multipart_upload(s3_client, file_path, bucket, key, part_size):
    """Perform a multipart upload to S3."""
    try:
        # Initiate the multipart upload
        create_response = s3_client.create_multipart_upload(Bucket=bucket, Key=key)
        upload_id = create_response['UploadId']
        parts = []

        # Read the file and upload parts
        with open(file_path, 'rb') as file:
            part_number = 1
            while True:
                data = file.read(part_size)
                if not data:
                    break
                part_response = upload_part(s3_client, bucket, key, upload_id, part_number, data)
                parts.append({
                    'PartNumber': part_number,
                    'ETag': part_response['ETag']
                })
                part_number += 1

        # Complete the multipart upload
        complete_response = s3_client.complete_multipart_upload(
            Bucket=bucket,
            Key=key,
            UploadId=upload_id,
            MultipartUpload={'Parts': parts}
        )
        print("File uploaded successfully to objectslite")
        print(f"HTTP Status Code: {complete_response['ResponseMetadata']['HTTPStatusCode']}")
        print(f"ETag: {complete_response['ETag']}")
    except Exception as e:
        print(f"Failed to upload file to objectslite with error: {e}")

def concurrent_multipart_upload(s3_client, file_path, bucket, key, part_size, max_concurrency):
    """Perform a multipart upload to S3 with concurrency."""
    assert max_concurrency <= 8, "max_concurrency must be less than or equal to 8"
    try:
        # Initiate the multipart upload
        create_response = s3_client.create_multipart_upload(Bucket=bucket, Key=key)
        upload_id = create_response['UploadId']
        parts = []

        # Read the file and upload parts concurrently
        with open(file_path, 'rb') as file, ThreadPoolExecutor(max_workers=max_concurrency) as executor:
            futures = []
            part_number = 1
            while True:
                data = file.read(part_size)
                if not data:
                    break
                futures.append(executor.submit(upload_part, s3_client, bucket, key, upload_id, part_number, data))
                part_number += 1

            for future in as_completed(futures):
                parts.append(future.result())

        # Complete the multipart upload
        complete_response = s3_client.complete_multipart_upload(
            Bucket=bucket,
            Key=key,
            UploadId=upload_id,
            MultipartUpload={'Parts': sorted(parts, key=lambda x: x['PartNumber'])}
        )
        print("File uploaded successfully to objectslite")
        print(f"HTTP Status Code: {complete_response['ResponseMetadata']['HTTPStatusCode']}")
        print(f"ETag: {complete_response['ETag']}")
    except Exception as e:
        print(f"Failed to upload file to objectslite with error: {e}")

def create_transfer_config(multipart_threshold, multipart_chunksize, max_concurrency):
    """Create a transfer configuration for multipart uploads."""
    assert max_concurrency <= 8, "max_concurrency must be less than or equal to 8"
    return TransferConfig(
        multipart_threshold=multipart_threshold,
        max_concurrency=max_concurrency,
        multipart_chunksize=multipart_chunksize,
        use_threads=True
    )

def upload_file(s3_client, file, bucket, key, transfer_config):
    """Upload a file to the specified S3 bucket with the given configuration."""
    try:
        s3_client.upload_file(
            Filename=file,
            Bucket=bucket,
            Key=key,
            Config=transfer_config
        )
        print("File uploaded successfully to objectslite")
    except Exception as e:
        print(f"Failed to upload file to objectslite with error: {e}")

def upload_part(s3_client, bucket, key, upload_id, part_number, data):
    """Upload a single part to S3."""
    part_response = s3_client.upload_part(
        Bucket=bucket,
        Key=key,
        PartNumber=part_number,
        UploadId=upload_id,
        Body=data
    )
    return {'PartNumber': part_number, 'ETag': part_response['ETag']}
