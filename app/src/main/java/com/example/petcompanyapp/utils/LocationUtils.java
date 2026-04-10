package com.example.petcompanyapp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class LocationUtils {

    private LocationUtils() {
        // Utilitario para rotulos e coordenadas de localizacao.
    }

    public static String buildCoordinateLabel(double latitude, double longitude) {
        return String.format(Locale.US, "Lat: %.5f | Lon: %.5f", latitude, longitude);
    }

    public static String resolveLocationLabel(Context context, double latitude, double longitude) {
        if (!Geocoder.isPresent()) {
            return buildCoordinateLabel(latitude, longitude);
        }

        try {
            Geocoder geocoder = new Geocoder(context, new Locale("pt", "BR"));
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressLine = address.getAddressLine(0);
                if (addressLine != null && !addressLine.trim().isEmpty()) {
                    return addressLine;
                }

                String locality = address.getSubAdminArea();
                if (locality == null || locality.trim().isEmpty()) {
                    locality = address.getLocality();
                }
                if (locality != null && !locality.trim().isEmpty()) {
                    return locality;
                }
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // Fallback para coordenadas quando o geocoder nao responder.
        }

        return buildCoordinateLabel(latitude, longitude);
    }
}
