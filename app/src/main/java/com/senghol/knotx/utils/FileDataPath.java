package com.senghol.knotx.utils;

import android.graphics.Bitmap;

public class FileDataPath {
    private String storedFilePath;
    private Bitmap bitmap;
    private String originalFilePath;



    public FileDataPath(String storedFilePath, Bitmap bitmap, String originalFilePath) {
        this.storedFilePath = storedFilePath;
        this.bitmap = bitmap;
        this.originalFilePath = originalFilePath;
    }
    public void setStoredFilePath(String storedFilePath) {
        this.storedFilePath = storedFilePath;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }
    public String getStoredFilePath() {
        return storedFilePath;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

}
