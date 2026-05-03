# Carpool Project - Viva Help & Technical Explanation

## 1. Project Goal
Facilitate a safe and efficient ride-sharing ecosystem for students and office workers, allowing drivers to share empty seats and passengers to find affordable commutes to their shared workplace or university.

## 2. Core App Logic

### Authentication & Profiles
- **User Management**: Uses Firebase Authentication for login/signup.
- **Workplace Filtering**: Users set their University/Office in `ProfileFragment`. The app then uses this to filter relevant rides in the Discovery feed.

### Driver Flow (The Provider)
- **Posting**: In `PostRideFragment`, drivers define a route (Home to Work or vice versa), set a schedule, and seats.
- **Managing**: In `MyPostsFragment`, drivers see their rides. Expanding a card shows all passenger requests.
- **Acceptance**: Drivers can "Accept" or "Reject" offers. Upon acceptance, the UI updates in real-time for both parties.
- **Live Trip**: Once a ride starts, the driver's `currentLat` and `currentLng` are updated in Firestore every 5 seconds.

### Passenger Flow (The Consumer)
- **Discovery**: `DiscoverFragment` fetches all active rides. Passengers can filter by destination, time, and days.
- **Negotiation**: Instead of a fixed price, the `BookingOfferBottomSheet` allows passengers to select a specific pickup point and propose a fair price.
- **Tracking**: Once accepted and "Ongoing", the passenger sees the driver's live moving car icon on their map.

---

## 3. Key Technical Files

### Models (Data Structure)
- **`Ride.java`**: Holds trip details (driver info, route coordinates, type, status).
- **`BookingOffer.java`**: Manages the "handshake" between passenger and driver, including the pickup point and negotiated price.
- **`Person.java`**: The user profile model.

### Adapters (UI Bridge)
- **`RideAdapter.java`**: The engine of the UI. It manages:
    - Dynamic map rendering for each card.
    - Combined routing (driver sees all passenger stops on one map).
    - Status-based UI transitions (Pending -> Accepted -> Ongoing).
    - Animations (The moving car icon logic).

### Fragments (Screens)
- **`DiscoverFragment.java`**: Handles the complex Firestore querying and multi-parameter filtering logic.
- **`PostRideFragment.java`**: Integrates Google Maps Geocoding to turn addresses into coordinates.

---

## 4. Viva Q&A - Technical Highlights

**Q: How does the map show the route?**
A: We use `PolylineOptions` to draw a line between the `startLat/Lng` and `endLat/Lng`.

**Q: How is real-time updating achieved?**
A: We use Firestore's `addSnapshotListener`. This creates a persistent connection that "pushes" data to the app instantly whenever a value changes in the database.

**Q: How do you handle map performance in a list?**
A: We use a `MapView` inside the RecyclerView items and carefully manage its lifecycle (`onCreate`, `onResume`) and a `Tag` system to prevent redundant re-initialization.

**Q: How is the fair calculated?**
A: In `BookingOfferBottomSheet`, we use a distance-based formula: `Base Fair + (Distance * Rate) + (Estimated Time * Time Rate)`.
