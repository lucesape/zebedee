package com.github.onsdigital.zebedee.content.json;

import java.util.List;
import java.util.Map;

/**
 * Created by thomasridd on 07/10/15.
 */
public class Chart {
    public String type;
    public String title;
    public String subtitle;
    public String unit;
    public String source;
    public String notes;
    public List<Map<String, String>> data;
    public List<String> categories;
    public List<String> headers;
}
