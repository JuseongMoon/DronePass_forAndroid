package com.ScienceFiction.DronePassAndroid.feature.vworld

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.NotamStatus

@get:StringRes
internal val FlightZoneLayer.displayNameRes: Int
    get() = when (this) {
        FlightZoneLayer.PROHIBITED -> R.string.flight_zone_layer_prohibited_zone
        FlightZoneLayer.TEMPORARY_PROHIBITED -> R.string.flight_zone_layer_temporary_prohibited
        FlightZoneLayer.CONTROL_ZONE -> R.string.flight_zone_layer_control_zone
        FlightZoneLayer.RESTRICTED -> R.string.flight_zone_layer_restricted_zone
        FlightZoneLayer.DANGER -> R.string.flight_zone_layer_danger_zone
        FlightZoneLayer.ALERT -> R.string.flight_zone_layer_boundary_zone
        FlightZoneLayer.ATZ -> R.string.flight_zone_layer_traffic_zone
        FlightZoneLayer.ULTRALIGHT -> R.string.flight_zone_layer_ultra_light_zone
        FlightZoneLayer.LANDING_FIELD -> R.string.flight_zone_layer_light_aircraft_zone
        FlightZoneLayer.OBSTACLE -> R.string.flight_zone_layer_obstacle_zone
        FlightZoneLayer.PRIOR_CONSULTATION -> R.string.flight_zone_layer_consultation_zone
        FlightZoneLayer.CULTURAL_HERITAGE -> R.string.flight_zone_layer_cultural_heritage
        FlightZoneLayer.NATIONAL_PARK -> R.string.flight_zone_layer_national_park
    }

@get:StringRes
internal val NotamStatus.displayNameRes: Int
    get() = when (this) {
        NotamStatus.SCHEDULED -> R.string.zone_detail_notam_status_scheduled
        NotamStatus.ACTIVE -> R.string.zone_detail_notam_status_active
        NotamStatus.EXPIRED -> R.string.zone_detail_notam_status_expired
        NotamStatus.UNKNOWN -> R.string.zone_detail_notam_status_unknown
    }
