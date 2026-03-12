# Bugfix Requirements Document

## Introduction

After migrating from a PHP API to Supabase, listings are not displaying on the homepage. The RecyclerView shows only the "Welcome to MineTeh" text with no listings visible. The app does not show any loading indicators, error messages, or visual feedback. This indicates that either:
1. The Supabase query is failing silently without returning data
2. The JSON parsing in parseListingsResponse() is failing and returning an empty list
3. The data is being fetched but not properly propagated to the UI

This bug prevents users from viewing any marketplace listings, making the core functionality of the app unusable. The previous "homepage-json-parsing-fix" spec addressed JSON parsing for the old PHP API, but the new Supabase integration uses a different response format that may not be handled correctly.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN the app opens and HomeViewModel.fetchListings() is called THEN the system displays an empty RecyclerView with no listings visible

1.2 WHEN the Supabase query executes in ListingsRepository.getListings() THEN the system fails to parse the response data into List<Listing> objects, resulting in an empty list

1.3 WHEN parseListingsResponse() receives the Supabase JSON response THEN the system fails to extract listing data correctly, returning zero listings even when data exists in the database

1.4 WHEN the UI observes the listings LiveData THEN the system receives Resource.Success with an empty list instead of populated listings, causing the RecyclerView to remain empty

### Expected Behavior (Correct)

2.1 WHEN the app opens and HomeViewModel.fetchListings() is called THEN the system SHALL fetch listings from Supabase and display them in the RecyclerView

2.2 WHEN the Supabase query executes in ListingsRepository.getListings() THEN the system SHALL successfully parse the response data into List<Listing> objects with all fields populated correctly

2.3 WHEN parseListingsResponse() receives the Supabase JSON response THEN the system SHALL extract all listing data correctly, including images and seller information, returning a populated list

2.4 WHEN the UI observes the listings LiveData THEN the system SHALL receive Resource.Success with a populated list and display the listings in the RecyclerView

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the Supabase database contains no listings THEN the system SHALL CONTINUE TO display an empty RecyclerView without crashing

3.2 WHEN a network error occurs during the Supabase query THEN the system SHALL CONTINUE TO catch the exception and return Resource.Error with an appropriate error message

3.3 WHEN category, type, or search filters are applied THEN the system SHALL CONTINUE TO filter the listings correctly in Kotlin after fetching from Supabase

3.4 WHEN the user navigates away from HomeActivity and returns THEN the system SHALL CONTINUE TO fetch and display listings correctly

3.5 WHEN parseListingImages() and parseAccounts() are called THEN the system SHALL CONTINUE TO parse related data correctly without affecting the main listings query
