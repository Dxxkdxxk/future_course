package com.lzlz.springboot.security.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lzlz.springboot.security.entity.SignTask;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.lzlz.springboot.security.mapper.SignTaskMapper;
import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling // 启动类添加该注解开启定时任务
public class SignTaskSchedule {

    private final SignTaskMapper signTaskMapper;

    public SignTaskSchedule(SignTaskMapper signTaskMapper) {
        this.signTaskMapper = signTaskMapper;
    }

    /**
     * 每1分钟执行一次，更新过期签到任务状态
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void updateExpiredSignTask() {
        // 查询所有有效且已过期的签到任务
        List<SignTask> expiredTaskList = signTaskMapper.selectList(
                new LambdaQueryWrapper<SignTask>()
                        .eq(SignTask::getStatus, "valid")
                        .lt(SignTask::getEndTime, LocalDateTime.now())
        );

        // 批量更新状态为invalid
        for (SignTask signTask : expiredTaskList) {
            signTask.setStatus("invalid");
            signTaskMapper.updateById(signTask);
        }
    }
}
