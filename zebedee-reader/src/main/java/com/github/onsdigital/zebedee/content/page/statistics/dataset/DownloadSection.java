package com.github.onsdigital.zebedee.content.page.statistics.dataset;

import com.github.onsdigital.zebedee.content.base.Content;

import java.util.List;

/**
 * Represents a section in a dataset for downloading
 */
public class DownloadSection extends Content {
    private String title;
    private List<String> cdids;
    private String file;
    private String fileDescription; //Markdown
    /**
     * content of the download section in Character for (i.e. extracted from the original format into clear text)
     */
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getCdids() {
        return cdids;
    }

    public void setCdids(List<String> cdids) {
        this.cdids = cdids;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
