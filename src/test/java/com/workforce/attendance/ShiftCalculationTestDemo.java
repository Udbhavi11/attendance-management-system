
package com.workforce.attendance;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.workforce.attendance.entity.Shift;
import com.workforce.attendance.util.ShiftTimeCalculator;

public class ShiftCalculationTestDemo {

    public static void main(String[] args) {

        Shift shift = new Shift();

        shift.setName("General");
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(18, 0));
        shift.setLateArrivalGraceMinutes(10);

        LocalDate rosterDate =
                LocalDate.of(2026, 11, 3);

        ZoneId zoneId = ZoneId.of("Asia/Kolkata");

        // Actual check-in: 09:15 AM
        Instant checkIn = Instant.parse(
                "2026-11-03T03:45:00Z"
        );

        // Actual check-out: 05:30 PM
        Instant checkOut = Instant.parse(
                "2026-11-03T12:00:00Z"
        );

        long lateMinutes =
                ShiftTimeCalculator.calculateLateArrivalMinutes(
                        rosterDate,
                        shift,
                        checkIn,
                        zoneId
                );

        long earlyMinutes =
                ShiftTimeCalculator.calculateEarlyDepartureMinutes(
                        rosterDate,
                        shift,
                        checkOut,
                        zoneId
                );

        System.out.println("Late arrival: "
                + lateMinutes + " minutes");

        System.out.println("Early departure: "
                + earlyMinutes + " minutes");
    }
}
