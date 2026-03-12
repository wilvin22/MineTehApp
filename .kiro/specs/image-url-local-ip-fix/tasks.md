# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Fault Condition** - Image URLs Use Local IP Address
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Scope the property to concrete failing cases with valid image paths
  - Test that image URLs are constructed with local IP "http://192.168.18.4/MineTeh" on unfixed code
  - Test that image load requests fail from Android devices due to inaccessible local IP
  - Test with various image paths: "uploads/laptop.jpg", "uploads/phone.jpg", "uploads/tablet.jpg"
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples: URLs constructed with local IP instead of public domain, network requests fail with connection errors
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Image Functionality Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-image operations
  - Observe: Listing titles, prices, descriptions display correctly
  - Observe: Click handlers navigate to detail screens correctly
  - Observe: Placeholder images display during loading states
  - Observe: RecyclerView scrolling and item recycling work correctly
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements
  - Property-based testing generates many test cases for stronger guarantees
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. Fix for image URL local IP address issue

  - [x] 3.1 Update ItemAdapter.kt to use public website URL
    - Replace hardcoded base URL in ItemAdapter.kt
    - Change FROM: "http://192.168.18.4/MineTeh"
    - Change TO: "https://mineteh.infinityfree.me"
    - Verify URL construction logic remains unchanged except for base URL string
    - _Bug_Condition: isBugCondition(input) where input.baseUrl == "http://192.168.18.4/MineTeh" AND input.isAndroidDevice == true AND NOT isAccessible(input.baseUrl, input.device)_
    - _Expected_Behavior: Image URLs SHALL use "https://mineteh.infinityfree.me" as base URL, enabling successful image loading from Android devices_
    - _Preservation: All non-image functionality (listing data display, user interactions, layout rendering, placeholder logic) SHALL remain unchanged_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4_

  - [x] 3.2 Update ListingsAdapter.kt to use public website URL
    - Replace hardcoded base URL in ListingsAdapter.kt
    - Change FROM: "http://192.168.18.4/MineTeh"
    - Change TO: "https://mineteh.infinityfree.me"
    - Verify URL construction logic remains unchanged except for base URL string
    - _Bug_Condition: isBugCondition(input) where input.baseUrl == "http://192.168.18.4/MineTeh" AND input.isAndroidDevice == true AND NOT isAccessible(input.baseUrl, input.device)_
    - _Expected_Behavior: Image URLs SHALL use "https://mineteh.infinityfree.me" as base URL, enabling successful image loading from Android devices_
    - _Preservation: All non-image functionality (listing data display, user interactions, layout rendering, placeholder logic) SHALL remain unchanged_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4_

  - [x] 3.3 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Image URLs Use Public Domain
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - Verify image URLs are constructed with "https://mineteh.infinityfree.me"
    - Verify image load requests succeed from Android devices
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 3.4 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Image Functionality Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - Verify listing data display (titles, prices, descriptions) unchanged
    - Verify click handlers and navigation unchanged
    - Verify placeholder display logic unchanged
    - Verify RecyclerView behavior unchanged
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)

- [x] 4. Checkpoint - Ensure all tests pass
  - All exploration tests pass (image URLs use public domain)
  - All preservation tests pass (non-image functionality unchanged)
  - Images load successfully on Android devices
  - No regressions in listing display or user interactions
