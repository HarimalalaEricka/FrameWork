package com.framework.model;

import java.util.*;

public class ModelView
{
    private String view;
    private Map<String, Object> model = new HashMap<>();

    public ModelView()
    {
        this.view = view;
    }
    public String getView()
    {
        return this.view;
    }
    public void setView( String view)
    {
        this.view = view;
    }
    public Map<String, Object> getModel()
    {
        return this.model;
    }
    public void addAttribute(String name, Object value)
    {
        model.put(name, value);
    }
}