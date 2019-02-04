

## Slate Task : Create and Monitor Geofence

The goal of this assignment is to create an Android application that will detect if the device is located inside of a geofence area.

Geofence area is defined as a combination of some geographic point, radius, and specific Wifi network name. A device is considered to be inside of the geofence area if the device is connected to the specified WiFi network or remains geographically inside the defined circle.

Note that if device coordinates are reported outside of the zone, but the device still connected to the specific Wifi network, then the device is treated as being inside the geofence area.

Application activity should provide controls to configure the geofence area and display current status: inside OR outside.

The latitude, longitude, and radius define a geofence. To mark a Geofence area specify its latitude and longitude. To adjust the proximity for the location add a radius. You can add Wi-Fi SSID for detect connection.

To run this sample, location must be enabled. Prerequisites

### Prerequisites
Android API Level >=v21

### Getting Started

This sample uses the Gradle build system. To build this project, use the "gradlew build" command or use "Import Project" in Android Studio.

