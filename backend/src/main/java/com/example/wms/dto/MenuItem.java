package com.example.wms.dto;

import java.util.List;

public class MenuItem {
    private String title;
    private String path;
    private String icon;
    private List<MenuItem> children;

    public MenuItem() {
    }

    public MenuItem(String title, String path, String icon) {
        this.title = title;
        this.path = path;
        this.icon = icon;
    }

    public MenuItem(String title, String icon, List<MenuItem> children) {
        this.title = title;
        this.icon = icon;
        this.children = children;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<MenuItem> getChildren() {
        return children;
    }

    public void setChildren(List<MenuItem> children) {
        this.children = children;
    }
}
