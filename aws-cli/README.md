# AWS CLI

This repository contains various AWS CLI commands that can be used to upload a file to objects-lite ([Reference](https://aws.amazon.com/cli/))

## Prerequisites

Install AWS CLI
https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html

Configure AWS CLI with objects-lite using `pc-ip`, `username` and `password`.
```
aws configure set aws_access_key_id $(echo -n "<username>:<password>" | openssl base64)
aws configure set aws_secret_access_key $(echo -n "<username>:<password>" | openssl base64)
aws configure set endpoint_url https://<pc-ip>:9440/api/prism/v4.0/objects/
```
Here, access and secret keys for objects-lite are of form `base64(<username>:<password>)`.  

### Put Object
To upload a file using PutObject API using AWS CLI, `aws s3api put-object` can be used.

**References:**  
https://docs.aws.amazon.com/cli/latest/reference/s3api/put-object.html

#### Usage 
```
aws s3api put-object --bucket <bucket-name> --body <file-path> --key <object-key> --no-verify-ssl
```

##### Parameters
- --bucket: The name of the bucket where the object will be stored.
- --key: The key (name) for the object in the bucket.
- --body: The path to the file to upload.
- --no-verify-ssl: Used to disable SSL certificate verification.

#### Output
After the file is uploaded successfully, the command will print the ETag of the uploaded object.

#### Limitations
- This command only supports uploading objects smaller than 5 GB. Consider using multipart upload or high-level `aws s3 cp` command for larger files.

### Multipart Upload
AWS CLI multipart upload commands can be used to perform multipart upload APIs on objects-lite. Following is the list of commands to be used:
- `aws s3api create-multipart-upload`: Command to initiate the multipart upload and get an upload ID.
- `aws s3api upload-part`: Command to upload each part separately. The corresponding part number and upload ID need to be specified.
- `aws s3api complete-multipart-upload`: This command completes the upload. UploadId and the list of parts with their ETags need to be specified.

**References:**  
https://docs.aws.amazon.com/cli/latest/reference/s3api/create-multipart-upload.html  
https://docs.aws.amazon.com/cli/latest/reference/s3api/upload-part.html  
https://docs.aws.amazon.com/cli/latest/reference/s3api/complete-multipart-upload.html

#### Usage 
```
aws s3api create-multipart-upload --bucket <bucket-name> --key <object-key> --no-verify-ssl
aws s3api upload-part --bucket <bucket-name> --key <object-key> --part-number <part-number> --body <file-path> --upload-id <upload-id> --no-verify-ssl
...
aws s3api complete-multipart-upload --bucket <bucket-name> --key <object-key> --upload-id <upload-id> --multipart-upload file://<parts-file> --no-verify-ssl
```

##### Parameters
- --bucket: The name of the bucket where the object will be stored.
- --key: The key (name) for the object in the bucket.
- --no-verify-ssl: Used to disable SSL certificate verification.
- --part-number: The part number of the part being uploaded.
- --body: The path to the file to upload as part.
- --upload-id: The upload ID received from the `create-multipart-upload` command.  
- --multipart-upload: The JSON file containing the list of parts and their ETags.  

##### Example JSON file
```
{
  "Parts": [
    {
      "ETag": "\"etag1\"",
      "PartNumber": 1
    },
    {
      "ETag": "\"etag2\"",
      "PartNumber": 2
    },
    {
      "ETag": "\"etag3\"",
      "PartNumber": 3
    }
  ]
}
```

#### Output
The `create-multipart-upload` command will return UploadId for the multipart upload to be used in further APIs.  
Further, `upload-part` will return etag for the corresponding part uploaded.  
Finally, `complete-multipart-upload` will return the etag for the finalized uploaded object.

#### Limitations
- These commands do not handle resumable uploads or retries in case of failure. In that case, the user needs to consider re-uploading the file.
- These commands does not support concurrent/parallel uploads. Each part is uploaded sequentially. Consider using high-level `aws s3 cp` command to achieve concurrency.

### High-level upload command
The AWS CLI command provides a high-level command, `aws s3 cp,` that allows users to copy files from local storage to S3-compatible endpoints. It simplifies the uploading process and supports automatic multipart uploads for large files. It also provides support for concurrent uploads, improving performance.

**References:**  
https://docs.aws.amazon.com/cli/latest/reference/s3/cp.html

#### Prerequisites
Configure the multipart threshold, chunk size and concurrency using commands:
```
aws configure set s3.multipart_threshold <multipart-threshold>
aws configure set s3.multipart_chunksize <multipart-chunksize>
aws configure set s3.max_concurrent_requests <max-concurrent-requests>
```
where  
`multipart-threshold` is the size threshold for when to use multipart uploads. This value can be specified in bytes or using size suffixes like KB, MB, or GB.  
`multipart-chunksize` is the size of each part in a multipart upload. (e.g., 5MB).  
`max-concurrent-requests` is the maximum number of concurrent requests for multipart uploads.

<b>NOTE:</b> Make sure `max-concurrent-requests` is set to a value less than or equal to 8 for objects-lite.

#### Usage 
```
aws s3 cp <file-path> s3://<bucket-name>/<object-key>  --no-verify-ssl
```

##### Parameters
- \<file-path\>: The path to the file to upload.
- \<bucket-name>\: The bucket's name where the object will be stored.
- \<object-key>\: The key (name) for the object in the bucket.
- --no-verify-ssl: Used to disable SSL certificate verification.

#### Output
After the file is uploaded successfully, the command will print a success message.

#### Limitations
- This command does not handle resumable uploads or retries in case of failure. In that case, the user needs to consider re-uploading the file.
