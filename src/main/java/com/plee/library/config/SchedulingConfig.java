package com.plee.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;


//@Configuration
//public class SchedulingConfig {
//
//    @Bean
//    public TaskScheduler taskScheduler() {
//        return new ThreadPoolTaskScheduler();
//    }
//}
