"""
This script uploads a file to objects-lite using the AWS SDK for Python (Boto3) with transfer manager.

Transfer managers provided by the Boto3 library offer an abstraction for efficient uploads, including automatic
switching to multipart uploads for large files and parallel uploading of parts. More details can be found here:
https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3.html#using-transfer-manager

As a prerequisite, the script requires the Boto3 library to be installed.
Install it using the following command:
    pip install boto3

Usage:
    python uploader.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path>
    [--multipart-threshold <threshold>] [--multipart-chunk-size <part_size>] [--max-concurrency <concurrency>]

Arguments:
    --endpoint             : The endpoint URL of the S3-compatible object storage service.
    --bucket               : The name of the bucket where the file will be uploaded.
    --key                  : The key (name) of the object to be created in the bucket.
    --file                 : The path to the file to be uploaded.
    --multipart-threshold  : The size threshold for multipart upload (default is 8 MB).
    --multipart-chunk-size : The size of each part in bytes (default is 8 MB).
    --max-concurrency      : The number of concurrent threads for multipart upload (default is 5).

The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and
secret key.

Example:
    python uploader.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt --file
    /path/to/myfile.txt --multipart-threshold 8388608 --multipart-chunk-size 8388608 --max-concurrency 5

Response Details:
    After a successful upload, the script will print a success message.

Limitations:
    - This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading
      the file in case of failure.
"""

import argparse
import base64
import boto3
import getpass
from botocore.client import Config
from boto3.s3.transfer import TransferConfig
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

def main():
    # Define command-line arguments
    parser = argparse.ArgumentParser(description='Upload a file to objectslite')
    parser.add_argument('--endpoint', required=True, help='Objectslite endpoint')
    parser.add_argument('--bucket', required=True, help='Bucket name')
    parser.add_argument('--key', required=True, help='Key name')
    parser.add_argument('--file', required=True, help='File to upload')
    parser.add_argument('--multipart-threshold', type=int, default=8 * 1024 * 1024, help='Multipart threshold for \
        multipart upload')
    parser.add_argument('--multipart-chunk-size', type=int, default=8 * 1024 * 1024, help='Part size for multipart \
        upload')
    parser.add_argument('--max-concurrency', type=int, default=5, help='Concurrency level for multipart upload')
    args = parser.parse_args()

    # Get encoded credentials
    encoded_credentials = get_credentials()

    # Create S3 client
    s3_client = create_s3_client(args.endpoint, encoded_credentials)

    # Configure the transfer settings
    transfer_config = TransferConfig(
        multipart_threshold=args.multipart_threshold,
        max_concurrency=args.max_concurrency,
        multipart_chunksize=args.multipart_chunksize,
        use_threads=True
    )

    # Upload the file
    upload_file(s3_client, args.file, args.bucket, args.key, transfer_config)

if __name__ == "__main__":
    main()