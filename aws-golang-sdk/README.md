# AWS SDK for Golang

This repository contains various scripts to uploads a file to objects-lite using the AWS SDK for Golang ([Reference](https://docs.aws.amazon.com/sdk-for-go/api/service/s3/))

## Prerequisites

Import all the dependencies

```bash
go mod tidy
```

## [utils/utils.go](./utils/utils.go) usage 
### GetCredentials
This function securely collect username and password from the user and encode them in Base64 format since access and secret key for objects-lite is `base64(<username>:<password>)`.  
#### Input
The function does not take any parameters.
#### Output
Returns a Base64 encoded string of the format username:password and error, if any.

### CreateS3Client
The CreateS3Client function creates a new S3 client using the provided endpoint and encoded credentials.
#### Input
- **endpoint (str):** The endpoint URL of the objects-lite
- **encodedCredentials (str)**: The Base64 encoded credentials for authentication. This will be used both as access and secret key for objects-lite
#### Output
Returns an S3 client object that can be used to perform various operations on objects-lite and error, if any.

### PutObject
The PutObject function uploads a file to an S3 bucket using the PutObject method of the S3 client. This function is useful for uploading smaller files directly to objects-lite.  
Reference: https://docs.aws.amazon.com/sdk-for-go/api/service/s3/#S3.PutObject  

[Sample Code](./examples/put-object.go)
#### Input
- **svc (\*s3.S3):** A pointer to an s3.S3 service client
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
- **filePath (str)**: The path to the file to be uploaded
#### Output
A pointer to the result of the PutObject operation (***s3.PutObjectOutput**) and error, if any.

### MultipartUpload
The MultipartUpload function performs a multipart upload to an S3 bucket using the S3 client. This function is useful for uploading large files by splitting them into smaller parts.  

**References:**  
https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html  
https://docs.aws.amazon.com/sdk-for-go/api/service/s3/#S3.CreateMultipartUpload  


[Sample Code](./examples/multipart-upload.go)
#### Input
- **svc (\*s3.S3):** A pointer to an s3.S3 service client
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
- **filePath (str)**: The path to the file to be uploaded
- **partSize (int)**: The size of each part in bytes
#### Output
A pointer to the result of the CompleteMultipartUpload(***s3.CompleteMultipartUploadOutput**) operation and error, if any.

### ConcurrentMultipartUpload
The ConcurrentMultipartUpload function performs a multipart upload to an S3 bucket using concurrency. This function is useful for uploading large files by splitting them into smaller parts and uploading them concurrently to improve performance.  

**References:**  
https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html  
https://docs.aws.amazon.com/sdk-for-go/api/service/s3/#S3.CreateMultipartUpload 

[Sample Code](./examples/concurrent-multipart-upload.go)
#### Input
- **svc (\*s3.S3):** A pointer to an s3.S3 service client
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
- **filePath (str)**: The path to the file to be uploaded
- **partSize (int)**: The size of each part in bytes
- **maxConcurrency (int)**: The maximum number of concurrent threads for uploading parts. Must be less than or equal to 8 for objects-lite
#### Output
A pointer to the result of the CompleteMultipartUpload(***s3.CompleteMultipartUploadOutput**) operation and error, if any.

### CreateUploader
The CreateUploader function creates an S3 uploader via AWS SDK's s3manager package with the provided endpoint, credentials, part size, and maximum concurrency.  

**References:**  
https://docs.aws.amazon.com/sdk-for-go/api/service/s3/s3manager/ 
#### Input
- **endpoint (str):** The endpoint URL of the objects-lite
- **encodedCredentials (str)**: The Base64 encoded credentials for authentication.
- **partSize (int)**: The size threshold (in bytes) for when to use multipart uploads and size of each part while performing multipart uploads.
- **maxConcurrency (int)**: The maximum number of concurrent threads for uploading parts. Must be less than or equal to 8
#### Output
A pointer to the created S3 uploader(***s3manager.Uploader**) and error, if any.

### UploadFile
The UploadFile function uploads a file to objects-lite using a provided uploader.  

**References:**  
https://docs.aws.amazon.com/sdk-for-go/api/service/s3/s3manager/#Uploader.Upload 

[Sample Code](./examples/uploader.go)
#### Input
- **uploader (\*s3manager.Uploader):** A pointer to an s3manager.Uploader instance used to upload the file.
- **bucket (str)**: The name of the bucket
- **key (str)**: The key (path) under which the file will be stored in the bucket
- **filePath (str)**: The path to the file to be uploaded
#### Output
A pointer to the result of the Upload operation(***s3manager.UploadOutput**) and error, if any.