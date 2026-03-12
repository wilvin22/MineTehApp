# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Fault Condition** - Complete Execution Chain Logging
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate where the execution chain breaks
  - **Manual Testing Approach**: Since this is a diagnostic bug requiring observation of logs across multiple components, manual testing is most appropriate
  - Test that after successful login, logs appear at each step: Login.navigateToHome() -> HomeActivity.onCreate() -> HomeViewModel.init -> HomeViewModel.fetchListings() -> ListingsRepository.getListings()
  - Run test on UNFIXED code (without diagnostic logging)
  - **EXPECTED OUTCOME**: Test FAILS - logs will stop appearing at some point in the chain (this is correct - it proves the bug exists)
  - Document counterexamples found: identify the exact point where logs stop appearing
  - Test cases to run:
    1. Login with valid credentials and observe log chain
    2. Use auto-login and observe log chain
    3. Check if HomeActivity.onCreate() is called
    4. Check if HomeViewModel constructor/init executes
    5. Check if ListingsRepository.getListings() is called
  - Mark task complete when test is run and the break point in execution chain is documented
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Login Flow Behavior
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-HomeActivity navigation flows
  - Write manual test cases capturing observed behavior patterns from Preservation Requirements
  - Manual testing is appropriate for preservation because the login flow has limited variations and requires visual verification
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Test cases to observe and document:
    1. Click signup button -> verify navigation to Signup activity works
    2. Click forgot password -> verify navigation to ForgotPassword activity works
    3. Enter invalid credentials -> verify error handling displays correctly
    4. Toggle Remember Me -> verify email is saved/loaded correctly
    5. Verify auto-login functionality works (may have same bug but should still attempt navigation)
  - Mark task complete when tests are run and passing behavior is documented on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [-] 3. Fix for listings not loading after login

  - [x] 3.1 Add diagnostic logging to Login.kt
    - Add entry log at start of navigateToHome(): "navigateToHome() called"
    - Log intent details: "Starting HomeActivity with intent: $intent"
    - Add result log after startActivity(): "startActivity(HomeActivity) completed"
    - Wrap startActivity() in try-catch to detect silent failures
    - Log any exceptions with full stack trace
    - _Bug_Condition: isBugCondition(input) where input.loginSuccessful == true AND input.navigateToHomeCalled == true AND input.repositoryGetListingsCalled == false_
    - _Expected_Behavior: Complete execution chain with logs at each step from design_
    - _Preservation: Login flow, error handling, auto-login, Remember Me, navigation to other activities must remain unchanged_
    - _Requirements: 2.1, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 3.2 Add diagnostic logging to HomeActivity.kt
    - Add entry log at very start of onCreate(): "onCreate() called"
    - Log intent extras: "Intent extras: ${intent.extras}"
    - Add checkpoint logs after each major step:
      - After setContentView: "setContentView completed"
      - After findViewById calls: "Views initialized"
      - Before viewModels() delegate: "About to initialize ViewModel"
      - After viewModels() delegate: "ViewModel initialized: $viewModel"
      - After setupRecyclerView: "RecyclerView setup completed"
      - After observeViewModel: "ViewModel observation setup completed"
    - Wrap onCreate() body in try-catch to detect crashes
    - Log any exceptions with full stack trace
    - _Bug_Condition: isBugCondition(input) where input.homeActivityCreated == UNKNOWN_
    - _Expected_Behavior: HomeActivity.onCreate() logs appear in execution chain_
    - _Preservation: Existing HomeActivity initialization must remain unchanged_
    - _Requirements: 2.2, 3.1_

  - [x] 3.3 Add diagnostic logging to HomeViewModel.kt
    - Add constructor log: "Constructor called with application: $application"
    - Log repository creation: "Creating ListingsRepository"
    - After repository creation: "ListingsRepository created: $repository"
    - Add init block log: "init block executing"
    - Before fetchListings: "About to call fetchListings()"
    - After fetchListings: "fetchListings() call completed"
    - Wrap init block in try-catch
    - Log any exceptions that might prevent fetchListings from being called
    - _Bug_Condition: isBugCondition(input) where input.homeViewModelInitialized == UNKNOWN_
    - _Expected_Behavior: HomeViewModel.init logs appear in execution chain_
    - _Preservation: Existing ViewModel initialization must remain unchanged_
    - _Requirements: 2.3, 3.1_

  - [x] 3.4 Add diagnostic logging to ListingsRepository.kt
    - Add constructor log: "Constructor called with context: $context"
    - After apiService init: "ApiService initialized"
    - After tokenManager init: "TokenManager initialized"
    - Move existing "Fetching listings from Supabase..." to very first line of getListings()
    - Add log before withContext: "getListings() called with category=$category, type=$type, search=$search"
    - Verify all exceptions are logged with full stack traces
    - _Bug_Condition: isBugCondition(input) where input.repositoryGetListingsCalled == false_
    - _Expected_Behavior: ListingsRepository.getListings() logs appear in execution chain_
    - _Preservation: Existing repository logic must remain unchanged_
    - _Requirements: 2.4, 3.1_

  - [x] 3.5 Run diagnostic tests and identify break point
    - Run the app with all diagnostic logging added
    - Perform login with valid credentials
    - Observe logs in Logcat to identify where execution chain breaks
    - Document the exact point of failure (which component's logs are missing)
    - Analyze root cause based on diagnostic findings
    - _Expected_Behavior: Logs reveal the exact break point in execution chain_
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 3.6 Implement targeted fix based on diagnostic findings
    - Based on where the chain breaks, implement the appropriate fix:
      - If HomeActivity not starting: Fix manifest configuration or intent creation
      - If ViewModel not initializing: Fix ViewModel factory or dependency injection
      - If crash before repository: Fix null pointer or coroutine issue
      - If repository failing: Fix Context, ApiClient, or SupabaseClient initialization
    - Document the specific fix applied
    - _Bug_Condition: isBugCondition(input) from design_
    - _Expected_Behavior: expectedBehavior(result) from design - complete execution chain_
    - _Preservation: Preservation Requirements from design_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 3.7 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Complete Execution Chain Logging
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - Verify logs appear at ALL steps: Login.navigateToHome() -> HomeActivity.onCreate() -> HomeViewModel.init -> HomeViewModel.fetchListings() -> ListingsRepository.getListings()
    - Verify listings are displayed in the RecyclerView OR appropriate error message is shown
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 3.8 Verify preservation tests still pass
    - **Property 2: Preservation** - Login Flow Behavior
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - Verify all non-HomeActivity navigation flows work exactly as before:
      1. Signup navigation works
      2. Forgot password navigation works
      3. Error handling displays correctly
      4. Remember Me saves/loads email correctly
      5. Auto-login attempts navigation (should now work if it was affected by same bug)
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Verify complete execution chain logs appear from Login through ListingsRepository
  - Verify listings are displayed correctly in HomeActivity
  - Verify all preservation tests pass (no regressions in login flow)
  - If any issues arise, document them and ask the user for guidance
