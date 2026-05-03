# Carpool Project Technical Documentation

This guide provides a comprehensive overview of the application's architecture, flow, and technical implementation to assist in project demonstrations and technical reviews.

## 1. Project Overview
The Carpool Project is an Android application designed for university students and office workers to share rides. It facilitates a real-time ecosystem where drivers can post their commutes and passengers can negotiate stops and fairs.

## 2. Core Functionalities & Flow

### User Authentication & Profile
- **Entry**: `SplashActivity` checks login status. New users go through `OnboardingActivity`.
- **Registration**: `SignupActivity` collects user roles (Student/Teacher) and basic info.
- **Profile Management**: `ProfileFragment` allows users to set their University or Office name and address. This is a critical data point for the discovery algorithm.

### Driver Lifecycle (Provider)
1. **Posting a Ride**: In `PostRideFragment`, drivers define a route (Home ↔ Workplace), select recurring days, time, and seat capacity. Locations are resolved using Google Maps Geocoding.
2. **Managing Requests**: In `MyPostsFragment`, drivers see their active rides. Expanding a ride reveals the `OfferAdapter` which lists all incoming passenger requests.
3. **Accepting Offers**: Drivers can accept or reject requests. Acceptance triggers a real-time UI update for the passenger and adds a stop marker to the driver's combined map.
4. **The Trip**: Drivers can "Start" and "End" rides. During the trip, their live GPS coordinates are updated in Firestore every few seconds.

### Passenger Lifecycle (Consumer)
1. **Discovery**: `DiscoverFragment` fetches all active rides. It automatically filters rides based on the university/office name stored in the user's profile.
2. **Booking & Negotiation**: Passengers use the `BookingOfferBottomSheet` to select a specific pickup point on the map. The fair is calculated based on the distance from the pickup to the destination.
3. **Tracking**: Once accepted, the passenger can monitor the ride status. In the "Ongoing" phase, a live car animation shows the driver's moving position on the map.

### Real-Time Communication
- **Chat**: `ChatActivity` provides instant messaging between drivers and passengers using Firestore's real-time listeners.
- **Notifications**: System alerts for offer status changes and ride start events.

## 3. Technical Implementation Details

### Data Persistence (Firebase Firestore)
The app uses a NoSQL structure with the following primary collections:
- `users`: Profile data and workplace info.
- `rides`: trip details, route coordinates, and current driver location.
- `offers`: Handshake documents connecting passengers to specific rides with negotiated prices and stops.
- `messages`: Real-time chat history.

### Map Integration (Google Maps SDK)
- **Dynamic Routing**: Uses `PolylineOptions` for the main route.
- **Stop Visualization**: Uses `MarkerOptions` with custom icons (Green for Pickup, Azure for Drop-off).
- **Camera Management**: Uses `LatLngBounds` to ensure all stops and the driver are visible on the screen simultaneously.

### UI & UX Features
- **Custom Adapters**: `RideAdapter` is the core UI component, managing multiple states (Discovery, My Posts, My Rides) and complex map lifecycles.
- **Animations**: Linear translate animations are used for the car icons to provide a modern, "live" feel.
- **Interactive Controls**: Custom zoom and external navigation buttons integrated directly into the map cards.
