
package com.workforce.attendance.service;

import com.workforce.attendance.entity.OfficeLocation;

public interface GeofenceService {

    boolean isWithinOffice(
            double latitude,
            double longitude,
            OfficeLocation office);

    double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2);
    
    void validateLocationAccuracy(Double accuracyMeters);
}
