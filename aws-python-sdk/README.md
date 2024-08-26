# AWS SDK for Python

This repository contains various scripts to uploads a file to objects-lite using the AWS SDK for Python ([Boto3](https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html))

## Prerequisites

Ensure you have the Boto3 library installed. You can install it using pip:

```bash
pip install boto3
```

## Usage
### get_credentials
This function securely collect username and password from the user and encode them in Base64 format since access and secret key for objects-lite is `base64(<username>:<password>)`.  
#### Input
The function does not take any parameters.
#### Output
Returns a Base64 encoded string of the format username:password.

### create_s3_client
The create_s3_client function creates an S3 client with the specified endpoint and encoded credentials. This client can be used to then interact with objects-lite.
#### Input
- **endpoint (str):** The endpoint URL of the objects-lite
- **encoded_credentials (str)**: The Base64 encoded credentials for authentication. This will be used both as access and secret key for objects-lite
#### Output
Returns an S3 client object that can be used to perform various operations on objects-lite.

### put_object
The put_object function uploads a file to an S3 bucket using the put_object method of the Boto3 S3 client. This function is useful for uploading smaller files directly to objects-lite.  
Reference: https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3/client/put_object.html  

[Sample Code](./examples/put-object.py)
#### Input
- **s3_client (boto3.client):** The Boto3 S3 client object
- **file_path (str)**: The path to the file to be uploaded
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
#### Output
The function does not return a value but prints the HTTP status code and ETag of the uploaded object if successful. If an error occurs, it prints an error message.

### multipart_upload
The multipart_upload function performs a multipart upload to an S3 bucket using the Boto3 S3 client. This function is useful for uploading large files by splitting them into smaller parts.  

**References:**  
https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html  
https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3/client/create_multipart_upload.html  

[Sample Code](./examples/multipart-upload.py)
#### Input
- **s3_client (boto3.client):** The Boto3 S3 client object
- **file_path (str)**: The path to the file to be uploaded
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
- **part_size (int)**: The size of each part in bytes
#### Output
The function does not return a value but prints the HTTP status code and ETag of the uploaded object if successful. If an error occurs, it prints an error message.

### concurrent_multipart_upload
The concurrent_multipart_upload function performs a multipart upload to an S3 bucket using concurrency. This function is useful for uploading large files by splitting them into smaller parts and uploading them concurrently to improve performance.  

**References:**  
https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html  
https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3/client/create_multipart_upload.html  

[Sample Code](./examples/concurrent-multipart-upload.py)
#### Input
- **s3_client (boto3.client):** The Boto3 S3 client object
- **file_path (str)**: The path to the file to be uploaded
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
- **part_size (int)**: The size of each part in bytes
- **max_concurrency (int)**: The maximum number of concurrent threads for uploading parts. Must be less than or equal to 8 for objects-lite
#### Output
The function does not return a value but prints the HTTP status code and ETag of the uploaded object if successful. If an error occurs, it prints an error message.

### create_transfer_config
The create_transfer_config function creates a transfer configuration for multipart uploads to Amazon S3. This configuration is used to control the behavior of multipart uploads, including the threshold for multipart uploads, the size of each part, and the maximum number of concurrent threads.  

**References:**  
https://boto3.amazonaws.com/v1/documentation/api/latest/reference/customizations/s3.html#boto3.s3.transfer.TransferConfig 
#### Input
- **multipart_threshold (int):** The size threshold (in bytes) for when to use multipart uploads. Files larger than this threshold will be uploaded in parts
- **multipart_chunksize (int)**: The size (in bytes) of each part when performing a multipart upload
- **max_concurrency (int)**: The maximum number of concurrent threads for uploading parts. Must be less than or equal to 8
#### Output
Returns a TransferConfig object configured with the specified parameters to be used further for uploading.

### upload_file
The upload_file function uploads a file to objects-lite using a given transfer configuration.  

**References:**  
https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3/client/upload_file.html# 

[Sample Code](./examples/uploader.py)
#### Input
- **s3_client (boto3.client):** The Boto3 S3 client object
- **file_path (str)**: The path to the file to be uploaded
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
- **transfer_config (boto3.s3.transfer.TransferConfig)**: The transfer configuration object that specifies parameters for the upload. Can be created using **create_transfer_config** function
#### Output
The function does not return a value but prints a success message if the upload is successful, or an error message if the upload fails.