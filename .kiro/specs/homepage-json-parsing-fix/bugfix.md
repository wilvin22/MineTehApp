# Bugfix Requirements Document

## Introduction

The app crashes with a "data format error" when opening the home page. This indicates a JSON parsing failure in the Gson converter when the HomeViewModel attempts to fetch listings through the ListingsRepository. The error occurs because:
1. The API response structure does not match the expected `ApiResponse<List<Listing>>` format, OR
2. The Listing objects in the response are missing required non-nullable fields, OR
3. The data field contains an incompatible type (object instead of array, null, etc.)

This bug prevents users from viewing the home page and accessing any listings, making the app unusable upon launch.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN the app opens and HomeViewModel.fetchListings() is called in the init block THEN the system crashes with IllegalStateException showing "Expected BEG..." error message

1.2 WHEN the API returns a JSON response where the data field is not a valid array structure (e.g., null, empty object {}, or malformed JSON) THEN the system fails to parse the response and throws IllegalStateException

1.3 WHEN Gson attempts to deserialize the API response into ApiResponse<List<Listing>> with mismatched JSON structure THEN the system crashes before the error can be caught and handled gracefully

### Expected Behavior (Correct)

2.1 WHEN the app opens and HomeViewModel.fetchListings() is called in the init block THEN the system SHALL handle the API response gracefully without crashing, even if the JSON structure is unexpected

2.2 WHEN the API returns a JSON response where the data field is not a valid array structure (e.g., null, empty object {}, or malformed JSON) THEN the system SHALL catch the parsing exception and return Resource.Error with an appropriate error message

2.3 WHEN Gson fails to deserialize the API response THEN the system SHALL catch the exception in the repository layer and display a user-friendly error message in the UI instead of crashing

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the API returns a valid JSON response with data as an array of listings THEN the system SHALL CONTINUE TO parse the response successfully and display the listings in the RecyclerView

3.2 WHEN the API returns an empty array [] in the data field THEN the system SHALL CONTINUE TO handle it correctly and display an empty list without crashing

3.3 WHEN the API returns success: false with an error message THEN the system SHALL CONTINUE TO display the error message to the user via Toast

3.4 WHEN network errors occur (timeout, no connection, etc.) THEN the system SHALL CONTINUE TO catch them and display appropriate error messages
