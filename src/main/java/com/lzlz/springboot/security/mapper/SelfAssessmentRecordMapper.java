package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.SelfAssessmentRecord;
import com.lzlz.springboot.security.entity.StudentAssessmentItemVO;
import com.lzlz.springboot.security.entity.StudentRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SelfAssessmentRecordMapper extends BaseMapper<SelfAssessmentRecord> {
    // 判断学生是否已提交该任务的自评
    @Select("SELECT COUNT(*) FROM self_assessment_record " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId}")
    Integer countByTaskAndStudent(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    @Select("SELECT DISTINCT student_id FROM self_assessment_record WHERE task_id = #{taskId}")
    List<Long> selectDistinctStudentIdsByTaskId(@Param("taskId") Long taskId);

    @Select({
            "SELECT ",
            "    r.id AS recordId, ",
            "    r.task_id AS taskId, ",
            "    t.task_title AS taskTitle, ",
            "    r.item_id AS itemId, ",
            "    i.item_content AS itemContent, ",
            "    r.master_level AS masterLevel, ",
            "    r.submit_time AS submitTime ",
            "FROM self_assessment_record r ",
            "LEFT JOIN self_assessment_task t ON r.task_id = t.id ",
            "LEFT JOIN self_assessment_item i ON r.item_id = i.id ",
            "WHERE r.student_id = #{studentId} ",
            "ORDER BY r.submit_time DESC"
    })
    List<StudentRecordVO> selectStudentRecordList(@Param("studentId") Long studentId);

    // 必须确保SQL中有#{taskId}和#{studentId}占位符，且表连接逻辑正确
    @Select("SELECT " +
            "  record.item_id AS itemId, " +
            "  record.master_level AS masterLevel, " +
            "  item.item_content AS itemContent " +
            "FROM self_assessment_record record " +
            "LEFT JOIN self_assessment_item item ON record.task_id = item.task_id " +
            "WHERE record.task_id = #{taskId} AND record.student_id = #{studentId}")
    List<StudentAssessmentItemVO> selectByTaskAndStudent(
            @Param("taskId") Long taskId,  // @Param名必须和SQL中的#{taskId}一致
            @Param("studentId") Long studentId  // @Param名必须和SQL中的#{studentId}一致
    );
}
