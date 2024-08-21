"""
This script uploads a file to objects-lite using the AWS SDK for Python (Boto3) with put_object.

As a prerequisite, the script requires the Boto3 library to be installed.
Install it using the following command:
    pip install boto3

Usage:
    python put-object.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path>

Arguments:
    --endpoint : The endpoint URL of the objects-lite service.
    --bucket   : The name of the bucket where the object will be uploaded.
    --key      : The key (name) of the object to be created in the bucket.
    --file     : The path to the file to be uploaded.

The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and
secret key.

Example:
    python put-object.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt
    --file /path/to/file.txt

Response Details:
    After a successful upload, the script will print the HTTP status code and ETag of the uploaded object.

Limitation:
    - This script only supports uploading objects smaller than 5 GB. For larger files, consider using multipart upload.
"""

import argparse
import base64
import boto3
import getpass
from botocore.client import Config
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

def upload_file(s3_client, file_path, bucket, key):
    """Upload a file to S3 using put_object."""
    try:
        response = s3_client.put_object(Bucket=bucket, Key=key, Body=file_path)
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
    args = parser.parse_args()

    # Get encoded credentials
    encoded_credentials = get_credentials()

    # Create S3 client
    s3_client = create_s3_client(args.endpoint, encoded_credentials)

    # Perform file upload
    upload_file(s3_client, args.file, args.bucket, args.key)

if __name__ == "__main__":
    main()