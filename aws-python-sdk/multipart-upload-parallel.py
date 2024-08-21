"""
This script uploads a file to objects-lite using the AWS SDK for Python (Boto3) with parallel multipart upload.

Parallel multipart upload allows you to upload a large object as a set of smaller parts concurrently, which can
significantly speed up the upload process.

As a prerequisite, the script requires the Boto3 library to be installed.
Install it using the following command:
    pip install boto3

Usage:
    python multipart-upload.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path>
    [--part-size <part_size>] [--max-concurrency <max_concurrency>]

Arguments:
    --endpoint        : The endpoint URL of the objects-lite service.
    --bucket          : The name of the bucket where the file will be uploaded.
    --key             : The key (name) of the object to be created in the bucket.
    --file            : The path to the file to be uploaded.
    --part-size       : The size of each part in bytes (default is 8 MB).
    --max-concurrency : The maximum number of concurrent uploads (default is 5).

The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and
secret key.

Example:
    python multipart-upload.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key
    myfile.txt --file /path/to/myfile.txt --part-size 8388608 --max-concurrency 8

Response Details:
    After a successful upload, the script will print the HTTP status code and ETag of the uploaded object.

Limitation:
    - This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading
      the file in case of failure.
"""

import argparse
import base64
import boto3
import getpass
from botocore.client import Config
import urllib3
from concurrent.futures import ThreadPoolExecutor, as_completed

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

def multipart_upload(s3_client, file_path, bucket, key, part_size, max_concurrency):
    """Perform a multipart upload to S3 with concurrency."""
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
        print(f"HTTP Status Code: {response['ResponseMetadata']['HTTPStatusCode']}")
        print(f"ETag: {response['ETag']}")
    except Exception as e:
        print(f"Failed to upload file to objectslite with error: {e}")

def main():
    # Define command-line arguments
    parser = argparse.ArgumentParser(description='Upload a file to objectslite')
    parser.add_argument('--endpoint', required=True, help='Objectslite endpoint')
    parser.add_argument('--bucket', required=True, help='Bucket name')
    parser.add_argument('--key', required=True, help='Key name')
    parser.add_argument('--file', required=True, help='File to upload')
    parser.add_argument('--part-size', type=int, default=8 * 1024 * 1024, help='Part size for multipart upload')
    parser.add_argument('--max-concurrency', type=int, default=5, help='Maximum number of concurrent uploads')
    args = parser.parse_args()

    # Get encoded credentials
    encoded_credentials = get_credentials()

    print(encoded_credentials)

    # Create S3 client
    s3_client = create_s3_client(args.endpoint, encoded_credentials)

    # Perform multipart upload
    multipart_upload(s3_client, args.file, args.bucket, args.key, args.part_size, args.max_concurrency)

if __name__ == "__main__":
    main()