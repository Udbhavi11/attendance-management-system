package com.workforce.attendance;
import java.time.LocalDate;
import java.time.LocalTime;

import com.workforce.attendance.entity.Shift;
import com.workforce.attendance.util.ShiftTimeCalculator;

public class ShiftCalculatorDemo {

    public static void main(String[] args) {

        Shift shift = new Shift();

        shift.setName("Night");
        shift.setStartTime(LocalTime.of(22, 0));
        shift.setEndTime(LocalTime.of(6, 0));

        LocalDate rosterDate =
                LocalDate.of(2026, 11, 3);

        System.out.println(
            "Overnight: " +
            ShiftTimeCalculator.isOvernightShift(shift)
        );

        System.out.println(
            "Start: " +
            ShiftTimeCalculator.getShiftStartDateTime(
                rosterDate, shift
            )
        );

        System.out.println(
            "End: " +
            ShiftTimeCalculator.getShiftEndDateTime(
                rosterDate, shift
            )
        );
    }
}