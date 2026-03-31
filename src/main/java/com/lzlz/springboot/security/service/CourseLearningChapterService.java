package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.CourseLearningChapterDto;
import com.lzlz.springboot.security.entity.ChapterResource;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.entity.CourseLearningChapter;
import com.lzlz.springboot.security.entity.Textbook;
import com.lzlz.springboot.security.mapper.ChapterResourceMapper;
import com.lzlz.springboot.security.mapper.CourseLearningChapterMapper;
import com.lzlz.springboot.security.mapper.CourseMapper;
import com.lzlz.springboot.security.mapper.TextbookMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourseLearningChapterService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d+(\\.\\d+)*$");
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final CourseMapper courseMapper;
    private final CourseLearningChapterMapper courseLearningChapterMapper;
    private final ChapterResourceMapper chapterResourceMapper;
    private final TextbookMapper textbookMapper;
    private final MinIOService minIOService;

    @Transactional(rollbackFor = Exception.class)
    public int replaceByXlsx(Long courseId, MultipartFile chapterFile) {
        if (courseMapper.selectById(courseId) == null) {
            throw new IllegalArgumentException("课程不存在: " + courseId);
        }
        if (chapterFile == null || chapterFile.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String fileName = Optional.ofNullable(chapterFile.getOriginalFilename()).orElse("");
        if (!fileName.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("仅支持xlsx文件");
        }

        List<ParsedRow> rows = parseRows(chapterFile);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("章节文件没有可解析的数据");
        }

        QueryWrapper<ChapterResource> resourceCleanWrapper = new QueryWrapper<>();
        resourceCleanWrapper.eq("course_id", courseId);
        resourceCleanWrapper.isNotNull("learning_chapter_id");
        resourceCleanWrapper.ne("learning_chapter_id", 0L);
        chapterResourceMapper.delete(resourceCleanWrapper);

        LambdaQueryWrapper<CourseLearningChapter> chapterCleanWrapper = new LambdaQueryWrapper<>();
        chapterCleanWrapper.eq(CourseLearningChapter::getCourseId, courseId);
        courseLearningChapterMapper.delete(chapterCleanWrapper);

        String parseBatchNo = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
        Map<String, Long> idByCode = new HashMap<>();
        Map<String, Integer> sortCounterByParentCode = new HashMap<>();

        int inserted = 0;
        for (ParsedRow row : rows) {
            String parentCode = parentCode(row.code);
            Long parentId = null;
            if (parentCode != null) {
                parentId = idByCode.get(parentCode);
                if (parentId == null) {
                    throw new IllegalArgumentException("第 " + row.rowNum + " 行父级章节不存在: " + parentCode);
                }
            }

            String parentKey = parentCode == null ? "ROOT" : parentCode;
            int sortOrder = sortCounterByParentCode.getOrDefault(parentKey, 0) + 1;
            sortCounterByParentCode.put(parentKey, sortOrder);

            CourseLearningChapter entity = new CourseLearningChapter();
            entity.setCourseId(courseId);
            entity.setParentId(parentId);
            entity.setLevel(levelOf(row.code));
            entity.setChapterCode(row.code);
            entity.setChapterName(row.name);
            entity.setSortOrder(sortOrder);
            entity.setContent(row.content);
            entity.setParseBatchNo(parseBatchNo);
            courseLearningChapterMapper.insert(entity);

            idByCode.put(row.code, entity.getId());
            inserted++;
        }
        return inserted;
    }

    public boolean existsInCourse(Long courseId, Long chapterId) {
        if (chapterId == null || chapterId <= 0) {
            return false;
        }
        LambdaQueryWrapper<CourseLearningChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseLearningChapter::getId, chapterId)
                .eq(CourseLearningChapter::getCourseId, courseId);
        return courseLearningChapterMapper.selectCount(wrapper) > 0;
    }

    public CourseLearningChapterDto.ChapterListData getChapterList(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在: " + courseId);
        }

        LambdaQueryWrapper<CourseLearningChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseLearningChapter::getCourseId, courseId)
                .orderByAsc(CourseLearningChapter::getLevel)
                .orderByAsc(CourseLearningChapter::getSortOrder)
                .orderByAsc(CourseLearningChapter::getId);

        List<CourseLearningChapter> all = courseLearningChapterMapper.selectList(wrapper);
        CourseLearningChapterDto.ChapterListData data = new CourseLearningChapterDto.ChapterListData();
        data.setTextbookName(course.getName());
        if (all.isEmpty()) {
            return data;
        }

        Map<Long, List<CourseLearningChapter>> childrenMap = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (CourseLearningChapter c : all) {
            ids.add(c.getId());
            childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }

        Map<Long, List<CourseLearningChapterDto.ResourceNode>> resourcesMap = queryResources(courseId, ids);
        List<CourseLearningChapterDto.ChapterNode> top = new ArrayList<>();

        List<CourseLearningChapter> roots = childrenMap.getOrDefault(null, Collections.emptyList());
        for (CourseLearningChapter root : roots) {
            CourseLearningChapterDto.ChapterNode node = new CourseLearningChapterDto.ChapterNode();
            node.setChapterId(String.valueOf(root.getId()));
            node.setChapterName(root.getChapterName());
            node.setParentId(null);
            node.setSortOrder(root.getSortOrder());
            node.setContent(root.getContent());
            node.setResources(resourcesMap.getOrDefault(root.getId(), Collections.emptyList()));

            List<CourseLearningChapter> children = childrenMap.getOrDefault(root.getId(), Collections.emptyList());
            for (CourseLearningChapter child : children) {
                CourseLearningChapterDto.ChildNode childNode = new CourseLearningChapterDto.ChildNode();
                childNode.setChapterId(String.valueOf(child.getId()));
                childNode.setChapterName(child.getChapterName());
                childNode.setParentId(String.valueOf(root.getId()));
                childNode.setSortOrder(child.getSortOrder());
                childNode.setIsCompleted(false);
                node.getChildren().add(childNode);
            }
            top.add(node);
        }

        data.setChapters(top);
        return data;
    }

    private Map<Long, List<CourseLearningChapterDto.ResourceNode>> queryResources(Long courseId, List<Long> chapterIds) {
        Map<Long, List<CourseLearningChapterDto.ResourceNode>> result = new HashMap<>();
        if (chapterIds.isEmpty()) {
            return result;
        }

        QueryWrapper<ChapterResource> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.in("learning_chapter_id", chapterIds);
        wrapper.orderByDesc("created_at");
        List<ChapterResource> resources = chapterResourceMapper.selectList(wrapper);
        if (resources.isEmpty()) {
            return result;
        }

        Set<Long> fileIds = new HashSet<>();
        for (ChapterResource r : resources) {
            if (r.getFileId() != null) {
                fileIds.add(r.getFileId());
            }
        }
        Map<Long, Textbook> textbookMap = new HashMap<>();
        if (!fileIds.isEmpty()) {
            List<Textbook> textbooks = textbookMapper.selectBatchIds(fileIds);
            for (Textbook textbook : textbooks) {
                textbookMap.put(textbook.getId(), textbook);
            }
        }

        for (ChapterResource r : resources) {
            CourseLearningChapterDto.ResourceNode node = new CourseLearningChapterDto.ResourceNode();
            node.setResourceId(r.getFileId() == null ? null : String.valueOf(r.getFileId()));
            node.setResourceName(r.getResourceName());
            node.setResourceType(r.getMaterialType());
            node.setUrl(buildUrl(textbookMap.get(r.getFileId())));
            Long key = r.getLearningChapterId();
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
        }
        return result;
    }

    private String buildUrl(Textbook textbook) {
        if (textbook == null || textbook.getMinioObjectName() == null) {
            return "";
        }
        try {
            return minIOService.getPresignedUrl(textbook.getMinioObjectName());
        } catch (Exception e) {
            return "";
        }
    }

    private List<ParsedRow> parseRows(MultipartFile file) {
        List<ParsedRow> rows = new ArrayList<>();
        Set<String> seenCode = new HashSet<>();

        try (InputStream is = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String code = normalizeCode(stringCell(row.getCell(0)));
                String name = stringCell(row.getCell(1)).trim();
                String content = stringCell(row.getCell(2)).trim();

                if (code.isEmpty() && name.isEmpty() && content.isEmpty()) {
                    continue;
                }
                if (code.isEmpty() || name.isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 1) + " 行chapterCode/chapterName不能为空");
                }
                if (!CODE_PATTERN.matcher(code).matches()) {
                    throw new IllegalArgumentException("第 " + (i + 1) + " 行chapterCode格式错误: " + code);
                }
                if (!seenCode.add(code)) {
                    throw new IllegalArgumentException("第 " + (i + 1) + " 行chapterCode重复: " + code);
                }

                ParsedRow parsed = new ParsedRow(i + 1, code, name, content);
                rows.add(parsed);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("解析xlsx失败: " + e.getMessage(), e);
        }
        return rows;
    }

    private String stringCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell);
    }

    private String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .replace("。", ".")
                .replace("．", ".")
                .replace(" ", "");
    }

    private String parentCode(String code) {
        int idx = code.lastIndexOf('.');
        if (idx < 0) {
            return null;
        }
        return code.substring(0, idx);
    }

    private int levelOf(String code) {
        return code.split("\\.").length;
    }

    @Getter
    private static class ParsedRow {
        private final int rowNum;
        private final String code;
        private final String name;
        private final String content;

        private ParsedRow(int rowNum, String code, String name, String content) {
            this.rowNum = rowNum;
            this.code = code;
            this.name = name;
            this.content = content;
        }
    }
}
