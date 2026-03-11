# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Fault Condition** - Graceful Handling of Malformed JSON
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Scope the property to concrete failing cases - malformed JSON responses where data field is not a valid array
  - Create unit test in ListingsRepositoryTest that mocks API service to return malformed JSON responses
  - Test cases: empty object `{"success": true, "message": null, "data": {}}`, null data `{"success": true, "message": null, "data": null}`, wrapped array `{"success": true, "message": null, "data": {"listings": [...]}}`, single object instead of array
  - Assert that getListings() returns Resource.Error with user-friendly message (not crash)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS with IllegalStateException "Expected BEGIN_ARRAY but was BEGIN_OBJECT" (this is correct - it proves the bug exists)
  - Document counterexamples found to understand root cause
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Valid Response Handling
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for valid API responses (non-buggy inputs)
  - Test case 1: Valid listings array - observe that responses with listing arrays parse correctly and display in RecyclerView
  - Test case 2: Empty array `[]` - observe that empty array responses are handled correctly without crashing
  - Test case 3: Error responses with `success: false` - observe that error messages are displayed via Toast
  - Test case 4: Network errors (timeout, no connection) - observe that they are caught and display appropriate error messages
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements
  - Property-based testing generates many test cases for stronger guarantees
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. Fix for JSON parsing crash on homepage

  - [x] 3.1 Implement custom deserializer for ApiResponse<List<Listing>>
    - Create custom JsonDeserializer in ApiClient.kt that handles type mismatches gracefully
    - Check if data field is a JSON array before attempting to deserialize
    - If data is not an array (object, null, etc.), return empty list or null
    - Handle missing or malformed fields gracefully
    - Register TypeAdapter with Gson using GsonBuilder().registerTypeAdapter()
    - Ensure it only applies to ApiResponse<List<Listing>> type
    - _Bug_Condition: isBugCondition(apiResponse) where parsed.data is not an array OR is null OR contains elements that don't match Listing schema OR gsonDeserialize throws IllegalStateException_
    - _Expected_Behavior: For any API response where isBugCondition returns true, getListings SHALL catch parsing exception and return Resource.Error with user-friendly message, preventing crash_
    - _Preservation: Valid array responses, empty arrays, error responses with success:false, and network errors must continue to work exactly as before_
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4_

  - [x] 3.2 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Graceful Handling of Malformed JSON
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Valid Response Handling
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
