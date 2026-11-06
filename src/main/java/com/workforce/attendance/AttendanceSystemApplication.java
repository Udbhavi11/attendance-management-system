package com.workforce.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//Enables Spring's scheduling mechanism.
//Allows methods annotated with @Scheduled to execute automatically
//at configured intervals, such as checking for forgotten employee breaks
//and creating anomaly flags when break policy limits are exceeded.
@EnableScheduling
public class AttendanceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendanceSystemApplication.class, args);
    }
}