package com.byk.imageservice.controller;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.byk.imageservice.aws.AWSS3Client;
import com.byk.imageservice.entity.OriginalImage;
import com.byk.imageservice.entity.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @GetMapping("/image/show/{predefinedTypeName}/{seoName}/")
    @ResponseStatus(HttpStatus.OK)
    public S3Object getImageById(@PathVariable String predefinedTypeName, @PathVariable String seoName,
                                 @RequestParam String reference) {
        final String objectKey = predefinedTypeName + "/" + reference;

        try {
            return this.awsS3Client.getOneObjectFromBucket(bucket.getName(), objectKey);
        } catch (Exception e) {
            final String reason = "Image with objectKey:" + objectKey + " could not be found!";
            log.info(reason);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
        }
    }

    @PostMapping("/image/{predefinedTypeName}/")
    @ResponseStatus(value = HttpStatus.CREATED)
    public void addImage(@PathVariable String predefinedTypeName, @RequestBody OriginalImage originalImage) {
        final File imageFile = new File(originalImage.getPathName());

        String fileExtension = originalImage.getPathName().substring(originalImage.getPathName().lastIndexOf(".") + 1);
        final Type type = Type.valueOf(fileExtension.toUpperCase());

        switch (type) {
            case JPG:
            case PNG:
                try {
                    final File scaledImageFile = optimizeImage(predefinedTypeName, imageFile, type);

                    this.awsS3Client.uploadOneObjectToBucket(bucket.getName(), "original/"
                            + directoryStrategy(originalImage.getPathName())[0], imageFile);
                    this.awsS3Client.uploadOneObjectToBucket(bucket.getName(), predefinedTypeName
                            + directoryStrategy(originalImage.getPathName())[1], scaledImageFile);
                } catch (Exception e) {
                    final String reason = "Image with key:" + originalImage.getPathName() + " could not be uploaded!";
                    log.warn(reason);
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason, e);
                }
                break;
            default:
                final String reason = "Please upload an image with the following extensions; JPG, PNG!";
                log.error(reason);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
        }
    }

    @DeleteMapping("/image/flush/{predefinedImageType}/")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void deleteImageById(@PathVariable String predefinedImageType, @RequestParam String reference) {
        try {
            this.awsS3Client.deleteOneObjectFromBucket(bucket.getName(),
                    predefinedImageType + "/" + reference);
        } catch (Exception e) {
            final String reason = "Image with objectKey:" + reference + " could not be deleted!";
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

    private File optimizeImage(String predefinedTypeName, File imageFile, Type type) throws IOException {
        final BufferedImage bufferedImage = ImageIO.read(imageFile);

        // Resizing/Optimizing the image.
        final Image scaledImage =
                bufferedImage.getScaledInstance(
                        (int) (bufferedImage.getWidth() * 0.25),
                        (int) (bufferedImage.getHeight() * 0.25),
                        Image.SCALE_AREA_AVERAGING
                );

        // Convert Image (scaledImage) to BufferedImage (scaledBufferedImage).
        BufferedImage scaledBufferedImage = new BufferedImage
                (scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics bg = scaledBufferedImage.getGraphics();
        bg.drawImage(scaledImage, 0, 0, null);
        bg.dispose();

        // Convert BufferedImage (scaledBufferedImage) to File (scaledBufferedImageFile).
        String imageFileWithoutExtension = imageFile.getPath().substring(0, imageFile.getPath().lastIndexOf("."));
        final Path scaledBufferedImageFile = Paths.get(imageFileWithoutExtension + "_" + predefinedTypeName + "." + type);
        Files.createFile(scaledBufferedImageFile);
        ImageIO.write(scaledBufferedImage, type.name(), scaledBufferedImageFile.toFile());

        return scaledBufferedImageFile.toFile();
    }

    private String[] directoryStrategy(String filePath) {
        String[] directoryArray = filePath.split("(?<=\\G.{4})");

        final char oldChar = '/';
        final char newChar = '_';

        final List<String> directories = Arrays.stream(directoryArray)
                .limit(2)
                .filter(s -> s.length() == 4 && !s.contains(".")).map(s -> s.replace(oldChar, newChar))
                .collect(Collectors.toList());

        final List<String> fileName = List.of(filePath).stream()
                .map(s -> s.replace(oldChar, newChar))
                .collect(Collectors.toList());

        List<String> combineLists = Stream.of(directories, fileName)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        final String[] directoryStrategy = {""};
        combineLists.forEach(s -> directoryStrategy[0] += "/" + s);

        return new String[]{fileName.get(0), directoryStrategy[0]};
    }
}
