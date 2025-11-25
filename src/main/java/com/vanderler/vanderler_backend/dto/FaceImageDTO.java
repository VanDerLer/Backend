package com.vanderler.vanderler_backend.dto;

public class FaceImageDTO {

    private String imageBase64;

    public FaceImageDTO() {
    }

    public FaceImageDTO(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
