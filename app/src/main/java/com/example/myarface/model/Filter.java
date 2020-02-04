package com.example.myarface.model;

import android.content.Intent;

public class Filter{

    private Integer resource;
    private String name;
    private String photo;

    public Filter(Integer resource, String name, String photo) {
        this.resource = resource;
        this.name = name;
        this.photo = photo;
    }

    public Integer getResource() {
        return resource;
    }

    public void setResource(Integer resource) {
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
