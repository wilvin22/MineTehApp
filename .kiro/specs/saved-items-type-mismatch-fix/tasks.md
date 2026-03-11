# Implementation Plan

- [ ] 1. Write bug condition exploration test
  - **Property 1: Fault Condition** - Compilation Error with Type Mismatch
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface the compilation error that demonstrates the type mismatch bug exists
  - **Scoped PBT Approach**: For this deterministic compilation bug, verify the concrete failing case: SavedItemsActivity using ItemAdapter with ArrayList<ItemModel>
  - Test that SavedItemsActivity.kt compiles successfully (will fail on unfixed code due to type mismatch at line 32:35)
  - Verify the compilation error message matches: "Argument type mismatch: actual type is 'java.util.ArrayList<com.example.mineteh.model.ItemModel>', but 'kotlin.collections.List<com.example.mineteh.models.Listing>' was expected"
  - Verify that favItems is of type ArrayList<ItemModel> and ItemAdapter expects this type
  - Verify that the activity uses dummy data instead of FavoritesViewModel
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the compilation bug exists)
  - Document the compilation error to understand the type mismatch root cause
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 2.1, 2.2, 2.3_

- [ ] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing Adapter Usage Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for components outside SavedItemsActivity
  - Verify that other activities using ItemAdapter with ItemModel (if any) compile and run correctly
  - Verify that ListingsAdapter usage in HomeActivity, SearchActivity, or other activities works correctly with List<Listing>
  - Verify that FavoritesViewModel and FavoritesRepository continue to function correctly
  - Verify that API integration (FavoritesRepository.getFavorites()) returns List<Listing> as expected
  - Write tests capturing observed behavior patterns from Preservation Requirements
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 3. Fix for SavedItemsActivity type mismatch compilation error

  - [ ] 3.1 Implement the fix in SavedItemsActivity.kt
    - Remove legacy import: `import com.example.mineteh.model.ItemModel`
    - Add required imports: ViewModelProvider, FavoritesViewModel, Resource, Toast
    - Remove dummy data creation (val favItems = arrayListOf(...))
    - Instantiate FavoritesViewModel: `val viewModel = ViewModelProvider(this)[FavoritesViewModel::class.java]`
    - Replace ItemAdapter with ListingsAdapter that accepts a click listener
    - Add observer for viewModel.favorites to update adapter with submitList()
    - Handle Resource states (Success, Error, Loading) in the observer
    - Call viewModel.loadFavorites() to trigger data fetch
    - _Bug_Condition: isBugCondition(code) where code.adapterType == "ItemAdapter" AND code.dataType == "ArrayList<ItemModel>" AND code.isDummyData == true_
    - _Expected_Behavior: code compiles successfully with ListingsAdapter and List<Listing>, uses FavoritesViewModel for data fetching_
    - _Preservation: Other activities using ItemAdapter or ListingsAdapter remain unchanged, FavoritesViewModel/Repository API unchanged, image loading via Glide continues to work_
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3_

  - [ ] 3.2 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Compilation Success with Correct Types
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - Verify SavedItemsActivity.kt compiles successfully without type mismatch errors
    - Verify the adapter is ListingsAdapter with List<Listing> type
    - Verify FavoritesViewModel is properly integrated
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 3.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing Adapter Usage Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - Verify other activities using adapters continue to work correctly
    - Verify FavoritesViewModel and FavoritesRepository remain unchanged
    - Verify API integration continues to function correctly
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)

- [ ] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
