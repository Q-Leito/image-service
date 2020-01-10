package com.byk.imageservice.entity;

import lombok.Data;

@Data
public class S3Image {
    private int height;
    private int width;
    private int quality;
    private ScaleType scaleType;
    private String fillColor;
    private Type type;
    private String sourceName;
}
