package com.lzlz.springboot.security.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class CourseLearningChapterDto {

    @Data
    public static class ChapterListData {
        private String textbookName;
        private List<ChapterNode> chapters = new ArrayList<>();
    }

    @Data
    public static class ChapterNode {
        private String chapterId;
        private String chapterName;
        private String parentId;
        private Integer sortOrder;
        private String content;
        private List<ResourceNode> resources = new ArrayList<>();
        private List<ChildNode> children = new ArrayList<>();
    }

    @Data
    public static class ChildNode {
        private String chapterId;
        private String chapterName;
        private String parentId;
        private Integer sortOrder;
        private Boolean isCompleted;
    }

    @Data
    public static class ResourceNode {
        private String resourceId;
        private String resourceName;
        private String resourceType;
        private String url;
    }
}
