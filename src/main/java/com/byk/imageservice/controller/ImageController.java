package com.byk.imageservice.controller;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.byk.imageservice.aws.AWSS3Client;
import com.byk.imageservice.entity.Image;
import com.byk.imageservice.entity.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.Optional;

@Slf4j
@RestController
public class ImageController {
    private AWSS3Client awsS3Client;
    private Bucket bucket;

    @Autowired
    public ImageController(AWSS3Client awsS3Client, Bucket bucket) {
        this.awsS3Client = awsS3Client;
        this.bucket = bucket;
    }

    @GetMapping("/images")
    @ResponseStatus(HttpStatus.OK)
    public ObjectListing getAllImages() {
        try {
            return this.awsS3Client.getAllObjectsFromBucket(bucket.getName());
        } catch (Exception e) {
            final String reason = "Images from bucket:" + bucket.getName() + " could not be found!";
            log.error(reason);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
        }
    }

    @GetMapping("/image")
    @ResponseStatus(HttpStatus.OK)
    public S3Object getImageById(@RequestParam String imageId) {
        try {
            return this.awsS3Client.getOneObjectFromBucket(bucket.getName(), imageId);
        } catch (Exception e) {
            final String reason = "Image with objectKey:" + imageId + " could not be found!";
            log.info(reason);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
        }
    }

    @PostMapping("/image")
    @ResponseStatus(value = HttpStatus.CREATED)
    public PutObjectResult addImage(@Validated @RequestBody Image image) {
        final File file = new File(image.getPathName());
        final String filePath = file.getPath();

        final Optional<String> fileExtension = Optional.of(filePath)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filePath.lastIndexOf(".") + 1));

        final Type type = Type.valueOf(fileExtension.get().toUpperCase());

        switch (type) {
            case JPG:
            case PNG:
                try {
                    return this.awsS3Client.uploadOneObjectToBucket(bucket.getName(), filePath, file);
                } catch (Exception e) {
                    final String reason = "Image with key:" + filePath + " could not be uploaded!";
                    log.warn(reason);
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
                }
            default:
                final String reason = "Please upload an image with the following extensions; JPG, PNG!";
                log.error(reason);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
        }
    }

    @PutMapping("/image")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void updateImage(@Validated @RequestBody Image image) {
        final File file = new File(image.getPathName());
        final String filePath = file.getPath();

        getImageById(filePath);

        try {
            this.awsS3Client.uploadOneObjectToBucket(bucket.getName(), filePath, file);
        } catch (Exception e) {
            final String reason = "Image with key:" + filePath + " could not be updated!";
            log.warn(reason);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
        }
    }

    @DeleteMapping("/image")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void deleteImageById(@RequestParam String imageId) {
        try {
            this.awsS3Client.deleteOneObjectFromBucket(bucket.getName(), imageId);
        } catch (Exception e) {
            final String reason = "Image with objectKey:" + imageId + " could not be deleted!";
            log.error(reason);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
        }
    }

    @DeleteMapping("/images")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void deleteAllImages() {
        try {
            this.awsS3Client.deleteAllObjectsFromBucket(bucket.getName());
        } catch (Exception e) {
            final String reason = "Images from bucket:" + bucket.getName() + " could not be deleted!";
            log.error(reason);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
        }
    }
}
