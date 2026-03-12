# Bugfix Requirements Document

## Introduction

All listings on the homepage were displaying placeholder images instead of their actual images because the Android app was attempting to load images from a local development IP address (http://192.168.18.4/MineTeh) that is not accessible from Android devices. This affected all users viewing the homepage, preventing them from seeing listing images. The fix updates the image URL construction in ItemAdapter.kt and ListingsAdapter.kt to use the public website URL (https://mineteh.infinityfree.me) where images are actually hosted.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN the app loads listing images on the homepage THEN the system uses the local development IP address (http://192.168.18.4/MineTeh) which is not accessible from Android devices

1.2 WHEN image loading fails due to inaccessible URL THEN the system displays placeholder images for all listings instead of actual images

1.3 WHEN ItemAdapter constructs image URLs THEN the system uses the hardcoded local IP address instead of the public URL

1.4 WHEN ListingsAdapter constructs image URLs THEN the system uses the hardcoded local IP address instead of the public URL

### Expected Behavior (Correct)

2.1 WHEN the app loads listing images on the homepage THEN the system SHALL use the public website URL (https://mineteh.infinityfree.me) which is accessible from Android devices

2.2 WHEN image URLs are constructed correctly THEN the system SHALL load and display actual listing images instead of placeholders

2.3 WHEN ItemAdapter constructs image URLs THEN the system SHALL use the public website URL as the base path

2.4 WHEN ListingsAdapter constructs image URLs THEN the system SHALL use the public website URL as the base path

2.5 WHEN images are loaded THEN the system SHALL log diagnostic information to track load success/failure

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the app displays listings on the homepage THEN the system SHALL CONTINUE TO show the same layout and UI structure

3.2 WHEN image loading is in progress THEN the system SHALL CONTINUE TO show placeholder images temporarily

3.3 WHEN listing data is displayed THEN the system SHALL CONTINUE TO show all other listing information (title, price, etc.) correctly

3.4 WHEN users interact with listings THEN the system SHALL CONTINUE TO respond to clicks and navigation as before
