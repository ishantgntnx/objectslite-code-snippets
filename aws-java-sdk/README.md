# AWS SDK for Java

This repository contains various scripts to uploads a file to objects-lite using the AWS SDK for Java ([Reference](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/))

## Prerequisites

Import all the dependencies

```bash
mvn clean package
```

## [Utils.java](./src/main/java/com/utils/Utils.java) usage 
### GetCredentials
This function securely collect username and password from the user and encode them in Base64 format since access and secret key for objects-lite is `base64(<username>:<password>)`.  
#### Input
The function does not take any parameters.
#### Output
Returns a Base64 encoded string of the format username:password.

### CreateS3Client
The CreateS3Client function creates a new S3 client using the provided endpoint and encoded credentials.
#### Input
- **encodedCredentials (String)**: The Base64 encoded credentials for authentication. This will be used both as access and secret key for objects-lite
- **endpoint (String):** The endpoint URL of the objects-lite
#### Output
Returns an S3 client object that can be used to perform various operations on objects-lite.

### PutObject
The PutObject function uploads a file to an S3 bucket using the PutObject method of the S3 client. This function is useful for uploading smaller files directly to objects-lite.  
Reference: https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AbstractAmazonS3.html#putObject-java.lang.String-java.lang.String-java.io.File- 

[Sample Code](./src/main/java/com/examples/PutObject.java)
#### Input
- **s3Client (AmazonS3):** The S3 client
- **bucket (String)**: The name of the bucket
- **key (String)**: The key (path) under which the file will be stored in the bucket
- **filePath (String)**: The path to the file to be uploaded
#### Output
The function does not return a value but prints the ETag of the uploaded object if successful. It throws exception if any error occurs during the upload.

### MultipartUpload
The MultipartUpload function performs a multipart upload to an S3 bucket using the S3 client. This function is useful for uploading large files by splitting them into smaller parts.  

**References:**  
https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html  
https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AbstractAmazonS3.html#initiateMultipartUpload-com.amazonaws.services.s3.model.InitiateMultipartUploadRequest-  


[Sample Code](./src/main/java/com/examples/MultipartUpload.java)
#### Input
- **s3Client (AmazonS3):** The S3 client
- **bucket (String)**: The name of the bucket
- **key (String)**: The key (path) under which the file will be stored in the bucket
- **filePath (String)**: The path to the file to be uploaded
- **partSize (long)**: The size of each part in bytes
#### Output
The function does not return a value but prints the ETag of the uploaded object if successful. It throws exception if any error occurs during the upload.

### ConcurrentMultipartUpload
The ConcurrentMultipartUpload function performs a multipart upload to an S3 bucket using concurrency. This function is useful for uploading large files by splitting them into smaller parts and uploading them concurrently to improve performance.  

**References:**  
https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html  
https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AbstractAmazonS3.html#initiateMultipartUpload-com.amazonaws.services.s3.model.InitiateMultipartUploadRequest-  

[Sample Code](./src/main/java/com/examples/ConcurrentMultipartUpload.java)
#### Input
- **s3Client (AmazonS3):** The S3 client
- **bucket (String)**: The name of the bucket
- **key (String)**: The key (path) under which the file will be stored in the bucket
- **filePath (String)**: The path to the file to be uploaded
- **partSize (long)**: The size of each part in bytes
- **maxConcurrency (int)**: The maximum number of concurrent threads for uploading parts. Must be less than or equal to 8 for objects-lite
#### Output
The function does not return a value but prints the ETag of the uploaded object if successful. It throws exception if any error occurs during the upload.
