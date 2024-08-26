/*
This module provides utility functions for uploading files to Objectslite using the AWS SDK for Go.

It includes functions for obtaining credentials, creating AWS sessions, and uploading files to S3 buckets using various
methods such as single put-object, multipart upload, and concurrent multipart upload. Additionally, it provides a
function to create an S3 uploader with configurable part size and concurrency level. These utilities simplify the
process of uploading files to Objectslite by handling session creation, credential management, and upload operations.

Functions:
- GetCredentials: Prompt for username and password, then encode them in Base64.
- CreateS3Client: Create a new S3 client with the provided endpoint and credentials.
- PutObject: Upload a file to S3 using put-object.
- MultipartUpload: Perform a multipart upload to S3.
- ConcurrentMultipartUpload: Perform a multipart upload to S3 with concurrency.
- CreateUploader: Create an S3 uploader with the provided configurations.
- UploadFile: Upload a file to S3 using the provided uploader.

Usage:
Import the module and call the desired functions with appropriate arguments.
*/

package utils

import (
	"bytes"
	"crypto/tls"
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
	"os"
	"sort"
	"sync"
	"syscall"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
	"golang.org/x/crypto/ssh/terminal"
)

const (
	defaultRegion = "us-east-1"
)

// GetCredentials prompts for username and password, then encodes them in Base64
func GetCredentials() (string, error) {
	var username string
	fmt.Print("Enter username: ")
	fmt.Scanln(&username)

	fmt.Print("Enter password: ")
	bytePassword, err := terminal.ReadPassword(int(syscall.Stdin))
	if err != nil {
		return "", fmt.Errorf("error reading password: %v", err)
	}
	password := string(bytePassword)
	fmt.Println()

	credentials := fmt.Sprintf("%s:%s", username, password)
	return base64.StdEncoding.EncodeToString([]byte(credentials)), nil
}

// createSession creates a new session with the provided endpoint and credentials.
func createSession(endpoint, encodedCredentials string) (*session.Session, error) {
	return session.NewSession(&aws.Config{
		Region:           aws.String(defaultRegion),
		Endpoint:         aws.String(endpoint),
		Credentials:      credentials.NewStaticCredentials(encodedCredentials, encodedCredentials, ""),
		S3ForcePathStyle: aws.Bool(true),
		HTTPClient: &http.Client{
			Transport: &http.Transport{
				TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
			},
		},
	})
}

// CreateS3Client creates a new S3 client with the provided endpoint and credentials
func CreateS3Client(endpoint, encodedCredentials string) (*s3.S3, error) {
	sess, err := createSession(endpoint, encodedCredentials)
	if err != nil {
		return nil, fmt.Errorf("failed to create session: %v", err)
	}
	return s3.New(sess), nil
}

// PutObject uploads a file to the specified S3 bucket and key using put-object API
func PutObject(svc *s3.S3, bucket, key, filePath string) (*s3.PutObjectOutput, error) {
	fd, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("failed to open file %q: %v", filePath, err)
	}
	defer fd.Close()

	return svc.PutObject(&s3.PutObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(key),
		Body:   fd,
	})
}

// MultipartUpload uploads a file to the specified S3 bucket and key using multipart upload
func MultipartUpload(svc *s3.S3, bucket, key, filePath string, partSize int64) (*s3.CompleteMultipartUploadOutput, error) {
	fd, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("failed to open file %q: %v", filePath, err)
	}
	defer fd.Close()

	// Initiate multipart upload
	createResp, err := svc.CreateMultipartUpload(&s3.CreateMultipartUploadInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(key),
	})
	if err != nil {
		return nil, fmt.Errorf("failed to initiate multipart upload: %v", err)
	}

	var completedParts []*s3.CompletedPart
	buffer := make([]byte, partSize)
	partNumber := int64(1)

	for {
		bytesRead, err := fd.Read(buffer)
		if err != nil && err != io.EOF {
			return nil, fmt.Errorf("failed to read file: %v", err)
		}
		if bytesRead == 0 {
			break
		}

		uploadResp, err := svc.UploadPart(&s3.UploadPartInput{
			Body:          bytes.NewReader(buffer[:bytesRead]),
			Bucket:        aws.String(bucket),
			Key:           aws.String(key),
			PartNumber:    aws.Int64(partNumber),
			UploadId:      createResp.UploadId,
			ContentLength: aws.Int64(int64(bytesRead)),
		})
		if err != nil {
			return nil, fmt.Errorf("failed to upload part %d: %v", partNumber, err)
		}

		completedParts = append(completedParts, &s3.CompletedPart{
			ETag:       uploadResp.ETag,
			PartNumber: aws.Int64(partNumber),
		})
		partNumber++
	}

	// Complete multipart upload
	return svc.CompleteMultipartUpload(&s3.CompleteMultipartUploadInput{
		Bucket:   aws.String(bucket),
		Key:      aws.String(key),
		UploadId: createResp.UploadId,
		MultipartUpload: &s3.CompletedMultipartUpload{
			Parts: completedParts,
		},
	})
}

// ConcurrentMultipartUpload uploads a file to the specified S3 bucket and key using multipart upload with concurrency
func ConcurrentMultipartUpload(svc *s3.S3, bucket, key, filePath string, partSize int64, maxConcurrency int) (*s3.CompleteMultipartUploadOutput, error) {
	if maxConcurrency > 8 {
		return nil, fmt.Errorf("maxConcurrency should be less than or equal to 8")
	}

	fd, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("failed to open file %q: %v", filePath, err)
	}
	defer fd.Close()

	// Initiate multipart upload
	createResp, err := svc.CreateMultipartUpload(&s3.CreateMultipartUploadInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(key),
	})
	if err != nil {
		return nil, fmt.Errorf("failed to initiate multipart upload: %v", err)
	}

	var completedParts []*s3.CompletedPart
	partNumber := int64(1)
	type PartResult struct {
		PartNumber int64
		ETag       *string
		Err        error
	}

	// Channel to collect part results with buffer size equal to maxConcurrency
	partCh := make(chan PartResult, maxConcurrency)
	var wg sync.WaitGroup

	// Semaphore to limit concurrency
	semaphore := make(chan struct{}, maxConcurrency)

	for {
		buffer := make([]byte, partSize)
		bytesRead, err := fd.Read(buffer)
		if err != nil && err != io.EOF {
			return nil, fmt.Errorf("failed to read file: %v", err)
		}
		if bytesRead == 0 {
			break
		}
		wg.Add(1)
		go func(partNumber int64, buffer []byte, bytesRead int) {
			defer wg.Done()
			// Acquire a slot in the semaphore
			semaphore <- struct{}{}
			defer func() { <-semaphore }() // Release the slot when done

			// Upload part
			uploadResp, err := svc.UploadPart(&s3.UploadPartInput{
				Body:          bytes.NewReader(buffer[:bytesRead]),
				Bucket:        aws.String(bucket),
				Key:           aws.String(key),
				PartNumber:    aws.Int64(partNumber),
				UploadId:      createResp.UploadId,
				ContentLength: aws.Int64(int64(bytesRead)),
			})

			if err != nil {
				partCh <- PartResult{PartNumber: partNumber, Err: fmt.Errorf("failed to upload part %d: %v", partNumber, err)}
				return
			}

			// Send part result to channel
			partCh <- PartResult{
				PartNumber: partNumber,
				ETag:       uploadResp.ETag,
				Err:        nil,
			}
		}(partNumber, buffer, bytesRead)

		partNumber++
	}

	// Close the part channel once all parts are processed
	go func() {
		wg.Wait()
		close(partCh)
	}()

	// Collect part results and handle errors
	for partResult := range partCh {
		if partResult.Err != nil {
			return nil, fmt.Errorf("failed to upload part %d with error: %v", partResult.PartNumber, partResult.Err)
		}
		completedParts = append(completedParts, &s3.CompletedPart{
			ETag:       partResult.ETag,
			PartNumber: aws.Int64(partResult.PartNumber),
		})
	}

	// Sort completed parts by PartNumber
	sort.Slice(completedParts, func(i, j int) bool {
		return *completedParts[i].PartNumber < *completedParts[j].PartNumber
	})

	// Complete multipart upload
	return svc.CompleteMultipartUpload(&s3.CompleteMultipartUploadInput{
		Bucket:   aws.String(bucket),
		Key:      aws.String(key),
		UploadId: createResp.UploadId,
		MultipartUpload: &s3.CompletedMultipartUpload{
			Parts: completedParts,
		},
	})
}

// CreateUploader creates an S3 uploader with the provided endpoint, credentials, part size, and max concurrency
func CreateUploader(endpoint, encodedCredentials string, partSize int64, maxConcurrency int) (*s3manager.Uploader, error) {
	sess, err := createSession(endpoint, encodedCredentials)
	if err != nil {
		return nil, fmt.Errorf("failed to create session: %v", err)
	}
	if maxConcurrency > 8 {
		return nil, fmt.Errorf("maxConcurrency should be less than or equal to 8")
	}
	return s3manager.NewUploader(sess, func(u *s3manager.Uploader) {
		u.Concurrency = maxConcurrency
		u.PartSize = partSize
	}), nil
}

// UploadFile uploads a file to the specified S3 bucket and key using the provided uploader
func UploadFile(uploader *s3manager.Uploader, bucket, key, filePath string) (*s3manager.UploadOutput, error) {
	fd, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("failed to open file %q: %v", filePath, err)
	}
	defer fd.Close()

	return uploader.Upload(&s3manager.UploadInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(key),
		Body:   fd,
	})
}
