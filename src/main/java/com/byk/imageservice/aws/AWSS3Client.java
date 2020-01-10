package com.byk.imageservice.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Slf4j
public class AWSS3Client {
    private final AmazonS3 amazonS3;

    public AWSS3Client() {
        this.amazonS3 = amazonS3();
    }

    public AWSCredentials awsCredentials() {
        return new BasicAWSCredentials(
                "<ACCESS_KEY_HERE>",
                "<SECRET_KEY_HERE>"
        );
    }

    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials()))
                .withRegion(Regions.EU_WEST_2)
                .build();
    }

    public Bucket createBucket(String bucketName) {
        if (amazonS3.doesBucketExistV2(bucketName)) {
            log.info("Bucket name '" + bucketName + "' already exist. Will try to retrieve the bucket.");

            final Optional<Bucket> optionalBucket = amazonS3.listBuckets().stream().filter(bucket -> bucket.getName().equals(bucketName)).findFirst();
            if (optionalBucket.isPresent()) {
                return optionalBucket.get();
            }
        }

        return amazonS3.createBucket(bucketName);
    }

    public ObjectListing getAllObjectsFromBucket(String bucketName) {
        return amazonS3.listObjects(bucketName);
    }

    public S3Object getOneObjectFromBucket(String bucketName, String objectKey) {
        return amazonS3.getObject(bucketName, objectKey);
    }

    public PutObjectResult uploadOneObjectToBucket(String bucketName, String key, File file) {
        return amazonS3.putObject(bucketName, key, file);
    }

    public void deleteOneObjectFromBucket(String bucketName, String objectKey) {
        amazonS3.deleteObject(bucketName, objectKey);
    }

    public void deleteAllObjectsFromBucket(String bucketName) {
        ObjectListing objectListing = getAllObjectsFromBucket(bucketName);

        while (true) {
            for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                deleteOneObjectFromBucket(bucketName, s3ObjectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
    }
}
