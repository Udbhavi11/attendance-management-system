
package com.workforce.attendance.service.impl;

import org.springframework.stereotype.Service;

import com.workforce.attendance.entity.OfficeLocation;
import com.workforce.attendance.service.GeofenceService;

@Service
public class GeofenceServiceImpl implements GeofenceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final double MAX_LOCATION_ACCURACY_METERS = 50.0;

    @Override
    public boolean isWithinOffice(
            double latitude,
            double longitude,
            OfficeLocation office) {

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180");
        }

        if (office == null || !office.isActive()) {
            throw new IllegalArgumentException(
                    "Employee has no active assigned office");
        }

        double distance = calculateDistance(
                latitude,
                longitude,
                office.getLatitude(),
                office.getLongitude());

        return distance <= office.getAllowedRadiusMeters();
    }

    @Override
    public double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2)
                * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        
        //Haversine formula - great-circle distance between two points on the Earth's surface
        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
    
	@Override
	public void validateLocationAccuracy(Double accuracyMeters) {
	
	    if (accuracyMeters == null || !Double.isFinite(accuracyMeters)
	            || accuracyMeters <= 0) {
	        throw new IllegalArgumentException(
	                "A valid GPS accuracy value is required");
	    }
	
	    if (accuracyMeters > MAX_LOCATION_ACCURACY_METERS) {
	        throw new IllegalArgumentException(
	                "GPS accuracy is too low. Please try again in a location "
	                + "with a clearer GPS signal.");
	    }
}

}
