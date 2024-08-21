# AWS SDK for Python

This repository contains various scripts to uploads a file to objects-lite using the AWS SDK for Python (Boto3) - https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html

## Prerequisites

Ensure you have the Boto3 library installed. You can install it using pip:

```bash
pip install boto3
```

## Scripts Description
### put-object.py
This script uploads a file to objects-lite using the put_object API.
#### Usage
```bash
python put-object.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path>
```
#### Arguments
```bash
--endpoint : The endpoint URL of the objects-lite service.
--bucket   : The name of the bucket where the object will be uploaded.
--key      : The key (name) of the object to be created in the bucket.
--file     : The path to the file to be uploaded.
```
The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and secret key.
#### Example
```bash
python put-object.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt --file /path/to/file.txt
```
#### Response Details
After a successful upload, the script will print the HTTP status code and ETag of the uploaded object.
#### Limitations
* This script only supports uploading objects smaller than 5 GB. For larger files, consider using multipart upload.
---
### multipart-upload.py
This script uploads a file to objects-lite using the multipart upload APIs.

Multipart upload allows you to upload a large object as a set of smaller parts.

#### Usage
```bash
python multipart-upload.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path> [--part-size <part_size>]
```
#### Arguments
```bash
--endpoint  : The endpoint URL of the objects-lite service.
--bucket    : The name of the bucket where the file will be uploaded.
--key       : The key (name) of the object to be created in the bucket.
--file      : The path to the file to be uploaded.
--part-size : The size of each part in bytes (default is 8 MB).
```
The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and secret key.
#### Example
```bash
python multipart-upload.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt --file /path/to/myfile.txt --part-size 8388608
```
#### Response Details
After a successful upload, the script will print the HTTP status code and ETag of the uploaded object.
#### Limitations
* This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading the file in case of failure.
* This script does not support concurrent/parallel uploads. Each part is uploaded sequentially.
---
### multipart-upload-parallel.py
This script uploads a file to objects-lite using the multipart upload APIs in parallel.

Parallel multipart upload allows you to upload a large object as a set of smaller parts concurrently, which can significantly speed up the upload process.

#### Usage
```bash
python multipart-upload.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path> [--part-size <part_size>] [--max-concurrency <max_concurrency>]
```
#### Arguments
```bash
--endpoint        : The endpoint URL of the objects-lite service.
--bucket          : The name of the bucket where the file will be uploaded.
--key             : The key (name) of the object to be created in the bucket.
--file            : The path to the file to be uploaded.
--part-size       : The size of each part in bytes (default is 8 MB).
--max-concurrency : The maximum number of concurrent uploads (default is 5).
```
The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and secret key.
#### Example
```bash
python multipart-upload.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt --file /path/to/myfile.txt --part-size 8388608 --max-concurrency 8
```
#### Response Details
After a successful upload, the script will print the HTTP status code and ETag of the uploaded object.
#### Limitations
* This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading the file in case of failure.
---
### uploader.py
This script uploads a file to objects-lite using transfer manager.

Transfer managers provided by the Boto3 library offer an abstraction for efficient uploads, including automatic switching to multipart uploads for large files and parallel uploading of parts. More details can be found here: https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3.html#using-transfer-manager

#### Usage
```bash
python uploader.py --endpoint <endpoint_url> --bucket <bucket_name> --key <object_key> --file <file_path> [--multipart-threshold <threshold>] [--multipart-chunk-size <part_size>] [--max-concurrency <concurrency>]
```
#### Arguments
```bash
--endpoint             : The endpoint URL of the S3-compatible object storage service.
--bucket               : The name of the bucket where the file will be uploaded.
--key                  : The key (name) of the object to be created in the bucket.
--file                 : The path to the file to be uploaded.
--multipart-threshold  : The size threshold for multipart upload (default is 8 MB).
--multipart-chunk-size : The size of each part in bytes (default is 8 MB).
--max-concurrency      : The number of concurrent threads for multipart upload (default is 5).
```
The script will prompt for a username and password, which will be encoded in Base64 and used as the access key and secret key.
#### Example
```bash
python uploader.py --endpoint https://<pc-ip>:9440/api/prism/v4.0/objects/ --bucket mybucket --key myfile.txt --file /path/to/myfile.txt --multipart-threshold 8388608 --multipart-chunk-size 8388608 --max-concurrency 5
```
#### Response Details
After a successful upload, the script will print a success message.
#### Limitations
* This script does not handle resumable uploads or retries in case of failure. User needs to consider re-uploading the file in case of failure.