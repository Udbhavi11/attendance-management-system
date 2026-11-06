
package com.workforce.attendance.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.workforce.attendance.entity.Shift;

public final class ShiftTimeCalculator {

    private ShiftTimeCalculator() {
        // Utility class; prevent object creation.
    }

    // Get scheduled shift start date-time.
    public static LocalDateTime getShiftStartDateTime(
            LocalDate rosterDate,
            Shift shift) {

        validateInputs(rosterDate, shift);

        return LocalDateTime.of(
                rosterDate,
                shift.getStartTime()
        );
    }

    // Get scheduled shift end date-time.
    // Overnight shifts end on the following date.
    public static LocalDateTime getShiftEndDateTime(
            LocalDate rosterDate,
            Shift shift) {

        validateInputs(rosterDate, shift);

        LocalDate endDate = rosterDate;

        if (isOvernightShift(shift)) {
            endDate = rosterDate.plusDays(1);
        }

        return LocalDateTime.of(
                endDate,
                shift.getEndTime()
        );
    }

    // Check whether the shift crosses midnight.
    public static boolean isOvernightShift(Shift shift) {

        if (shift == null
                || shift.getStartTime() == null
                || shift.getEndTime() == null) {

            throw new IllegalArgumentException(
                    "Shift and shift times are required"
            );
        }

        return shift.getEndTime()
                .isBefore(shift.getStartTime());
    }

    // Calculate late arrival in complete minutes.
    public static long calculateLateArrivalMinutes(
            LocalDate rosterDate,
            Shift shift,
            Instant actualCheckIn,
            ZoneId zoneId) {

        validateInputs(rosterDate, shift);

        if (actualCheckIn == null || zoneId == null) {
            throw new IllegalArgumentException(
                    "Check-in time and time zone are required"
            );
        }

        LocalDateTime scheduledStart =
                getShiftStartDateTime(rosterDate, shift);

        Instant graceDeadline = scheduledStart
                .atZone(zoneId)
                .plusMinutes(shift.getLateArrivalGraceMinutes())
                .toInstant();

        if (!actualCheckIn.isAfter(graceDeadline)) {
            return 0;
        }

        return Duration.between(
                graceDeadline,
                actualCheckIn
        ).toMinutes();
    }

 // Calculate early departure in complete minutes.
    public static long calculateEarlyDepartureMinutes(
            LocalDate rosterDate,
            Shift shift,
            Instant actualCheckOut,
            ZoneId zoneId) {

        validateInputs(rosterDate, shift);

        if (actualCheckOut == null || zoneId == null) {
            throw new IllegalArgumentException(
                    "Check-out time and time zone are required"
            );
        }

        LocalDateTime scheduledStart =
                getShiftStartDateTime(rosterDate, shift);

        LocalDateTime scheduledEnd =
                getShiftEndDateTime(rosterDate, shift);

        Instant scheduledStartInstant = scheduledStart
                .atZone(zoneId)
                .toInstant();

        Instant scheduledEndInstant = scheduledEnd
                .atZone(zoneId)
                .toInstant();

        if (actualCheckOut.isBefore(scheduledStartInstant)) {
            throw new IllegalArgumentException(
                    "Checkout cannot occur before the scheduled shift starts"
            );
        }

        if (!actualCheckOut.isBefore(scheduledEndInstant)) {
            return 0;
        }

        return Duration.between(
                actualCheckOut,
                scheduledEndInstant
        ).toMinutes();
    }

    // Validate roster date and shift details.
    private static void validateInputs(
            LocalDate rosterDate,
            Shift shift) {

        if (rosterDate == null) {
            throw new IllegalArgumentException(
                    "Roster date is required"
            );
        }

        if (shift == null
                || shift.getStartTime() == null
                || shift.getEndTime() == null) {

            throw new IllegalArgumentException(
                    "Shift and shift times are required"
            );
        }

        if (shift.getStartTime().equals(shift.getEndTime())) {
            throw new IllegalArgumentException(
                    "Shift start time and end time cannot be the same"
            );
        }

        if (shift.getLateArrivalGraceMinutes() < 0) {
            throw new IllegalArgumentException(
                    "Grace period cannot be negative"
            );
        }
    }
}
