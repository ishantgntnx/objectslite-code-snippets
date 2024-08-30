"""
This script uploads a file to objects-lite using the AWS SDK for Python (Boto3) with multipart upload.

Multipart upload allows you to upload a large object as a set of smaller parts.

Usage:
    python multipart-upload.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path>
    [--part-size <part_size>]

Arguments:
    --endpoint  : The endpoint URL of the objects-lite service.
    --bucket    : The name of the bucket where the file will be uploaded.
    --key       : The key (name) of the object to be created in the bucket.
    --file      : The path to the file to be uploaded.
    --part-size : The size of each part in bytes (default is 8 MB).

The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and
secret key.

Example:
    python multipart-upload.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key
    myfile.txt --file /path/to/myfile.txt --part-size 8388608

Response Details:
    After a successful upload, the script will print the HTTP status code and ETag of the uploaded object.

Limitation:
    - This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading
      the file in case of failure.
    - This script does not support concurrent/parallel uploads. Each part is uploaded sequentially.
"""

import argparse
import sys
from os.path import dirname, abspath
import urllib3

# Add parent directory to sys.path
sys.path.insert(0, dirname(dirname(abspath(__file__))))

# Import functions from utils.py
from utils import get_credentials, create_s3_client, multipart_upload

# Disable SSL verification warning
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def main():
    # Define command-line arguments
    parser = argparse.ArgumentParser(description='Upload a file to objectslite')
    parser.add_argument('--endpoint', required=True, help='Objectslite endpoint')
    parser.add_argument('--bucket', required=True, help='Bucket name')
    parser.add_argument('--key', required=True, help='Key name')
    parser.add_argument('--file', required=True, help='File to upload')
    parser.add_argument('--part-size', type=int, default=8 * 1024 * 1024, help='Part size for multipart upload')
    args = parser.parse_args()

    # Get encoded credentials
    encoded_credentials = get_credentials()

    # Create S3 client
    s3_client = create_s3_client(args.endpoint, encoded_credentials)

    # Perform multipart upload
    multipart_upload(s3_client, args.file, args.bucket, args.key, args.part_size)

if __name__ == "__main__":
    main()
