package com.atomicswe.videostreaming.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class Video {
    private String name;
    private Date created = new Date();
    private long contentLength;
    private String contentMimeType = "video/mp4";
}