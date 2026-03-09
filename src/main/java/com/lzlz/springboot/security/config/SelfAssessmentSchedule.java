package com.lzlz.springboot.security.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lzlz.springboot.security.entity.SelfAssessmentTask;
import com.lzlz.springboot.security.mapper.SelfAssessmentTaskMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SelfAssessmentSchedule {

    private final SelfAssessmentTaskMapper selfAssessmentTaskMapper;

    public SelfAssessmentSchedule(SelfAssessmentTaskMapper selfAssessmentTaskMapper) {
        this.selfAssessmentTaskMapper = selfAssessmentTaskMapper;
    }

    /**
     * 每天凌晨执行，更新过期自评任务状态
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateExpiredTask() {
        List<SelfAssessmentTask> expiredTaskList = selfAssessmentTaskMapper.selectList(
                new LambdaQueryWrapper<SelfAssessmentTask>()
                        .eq(SelfAssessmentTask::getStatus, "valid")
                        .lt(SelfAssessmentTask::getEndTime, LocalDateTime.now())
        );

        for (SelfAssessmentTask task : expiredTaskList) {
            task.setStatus("invalid");
            selfAssessmentTaskMapper.updateById(task);
        }
    }
}
