package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.SelfAssessmentItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SelfAssessmentItemMapper extends BaseMapper<SelfAssessmentItem> {
    // 按任务ID查询条目列表（按排序号排序）
    @Select("SELECT * FROM self_assessment_item " +
            "WHERE task_id = #{taskId}")
    List<SelfAssessmentItem> selectByTaskId(@Param("taskId") Long taskId);

    @Insert({
            "<script>",
            "INSERT INTO self_assessment_item (task_id, item_content, sort_num) ",
            "VALUES ",
            "<foreach collection='itemList' item='item' separator=','>",
            "(#{item.taskId}, #{item.itemContent}, #{item.sortNum})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("itemList") List<SelfAssessmentItem> itemList);
}
