# Carpool Project - Comprehensive Viva Guide

## 1. Project Objective
The project aims to provide a secure and efficient ride-sharing platform for communities like universities and corporate offices. By matching commuters going to the same workplace, it reduces transportation costs, decreases traffic congestion, and lowers carbon emissions.

## 2. Technical Architecture
- **Language**: Java
- **Database**: Firebase Firestore (NoSQL)
- **Authentication**: Firebase Auth
- **Maps**: Google Maps SDK for Android
- **Location Services**: FusedLocationProviderClient (Google Play Services)

## 3. Core App Logic & Flow

### User Onboarding
- **Signup**: Users register and specify their role (Student/Staff). Profiles are stored in the `users` collection.
- **Profile**: Users set their **University/Workplace name and address**. This is used to suggested relevant rides.

### The Driver lifecycle
1. **Posting**: A driver creates a ride in `PostRideFragment`. They define the route, time, and seats. The app uses Google Geocoding to save precise coordinates.
2. **Management**: In `MyPostsFragment`, drivers see their rides. Expanding a ride shows all incoming requests.
3. **Approval**: The driver can accept a passenger. This updates the status in real-time and decrements available seats.
4. **The Trip**: When the driver starts the ride, the app begins sending GPS updates to Firestore.

### The Passenger lifecycle
1. **Searching**: In `DiscoverFragment`, passengers see rides filtered by their workplace. They can use search and time filters to find the best match.
2. **Negotiation**: Passengers select their specific pickup point on the map and propose a fair price.
3. **Real-time Tracking**: Once accepted, the passenger can see the driver's live position moving on their map in the "Ongoing" phase.

## 4. Key Files Explanation

### Models (Data Structures)
- **`Ride.java`**: Stores trip data: origin/destination coordinates, driver UID, and seat counts.
- **`BookingOffer.java`**: Represents a request from a passenger, including their specific stop and the offered price.
- **`Person.java`**: The user model containing profile and workplace information.

### UI Adapters
- **`RideAdapter.java`**: The core of the app's UI. It handles the dynamic map rendering for every card, the car animations, and the complex combined map view for drivers that shows every passenger stop.

### Key Fragments
- **`DiscoverFragment.java`**: Manages the ride feed and implements real-time filtering logic.
- **`PostRideFragment.java`**: Uses the Geocoder to convert text addresses into map coordinates.
- **`ProfileFragment.java`**: Handles user preferences and workplace settings.

## 5. Technical Viva Questions
- **Q: How does the map show all stops?** 
  - A: We iterate through all accepted `BookingOffer` documents and add a Marker for each stop to the main map using `googleMap.addMarker()`.
- **Q: How is real-time chat achieved?**
  - A: We use Firestore's `addSnapshotListener` on the `messages` collection to detect new documents instantly.
- **Q: How do you handle the car animation?**
  - A: We use a linear `translate` animation defined in XML and apply it to the car `ImageView` in the adapter.
