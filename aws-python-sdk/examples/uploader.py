"""
This script uploads a file to objects-lite using the AWS SDK for Python (Boto3) with transfer manager.

Transfer managers provided by the Boto3 library offer an abstraction for efficient uploads, including automatic
switching to multipart uploads for large files and parallel uploading of parts. More details can be found here:
https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3.html#using-transfer-manager

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
import sys
from os.path import dirname, abspath
import urllib3

# Add parent directory to sys.path
sys.path.insert(0, dirname(dirname(abspath(__file__))))

# Import functions from utils.py
from utils import get_credentials, create_s3_client, create_transfer_config, upload_file

# Disable SSL verification warning
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

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

    transfer_config = create_transfer_config(args.multipart_threshold, args.multipart_chunk_size, args.max_concurrency)

    # Upload the file
    upload_file(s3_client, args.file, args.bucket, args.key, transfer_config)

if __name__ == "__main__":
    main()
