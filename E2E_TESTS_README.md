# E2E Test Suite - Picture Book Management System

Comprehensive end-to-end tests for the Android Picture Book Management System.

## Test Coverage

### User Flows Tested

| Flow | Test Class | Status |
|------|------------|--------|
| 1. Book Creation | `PictureBookCreationE2ETest` | Implemented |
| 2. Image Import | `ImageImportDisplayE2ETest` | Implemented |
| 3. Search & Filter | `SearchFunctionalityE2ETest` | Implemented |
| 4. Book Deletion | `BookDeletionE2ETest` | Implemented |
| 5. Data Persistence | `DataPersistenceE2ETest` | Implemented |

## Quick Start

### Windows

```batch
REM Run all E2E tests
run_e2e_tests.bat --all

REM Run quick sanity tests
run_e2e_tests.bat --quick

REM Run specific test class
run_e2e_tests.bat --test=com.maomaochongapp.picturebook.e2e.PictureBookCreationE2ETest

REM Check for flaky tests
run_e2e_tests.bat --flaky
```

### Linux/Mac/WSL

```bash
# Make script executable (first time only)
chmod +x run_e2e_tests.sh

# Run all E2E tests
./run_e2e_tests.sh --all

# Run quick sanity tests
./run_e2e_tests.sh --quick

# Run specific test class
./run_e2e_tests.sh --test=com.maomaochongapp.picturebook.e2e.PictureBookCreationE2ETest

# Check for flaky tests
./run_e2e_tests.sh --flaky
```

### Direct Gradle Commands

```bash
# Run all E2E tests
./gradlew connectedAndroidTest --tests "com.maomaochongapp.picturebook.e2e.*"

# Run specific test class
./gradlew connectedAndroidTest --tests "com.maomaochongapp.picturebook.e2e.PictureBookCreationE2ETest"

# Run with coverage
./gradlew connectedAndroidTest --tests "com.maomaochongapp.picturebook.e2e.*" -Pcoverage

# Run with debug logging
./gradlew connectedAndroidTest --tests "com.maomaochongapp.picturebook.e2e.*" --info
```

## Test Files

### E2E Tests (androidTest)

Located in `app/src/androidTest/java/com/maomaochongapp/picturebook/e2e/`:

1. **PictureBookCreationE2ETest.kt**
   - Complete book creation flow
   - Multiple books with tag filtering
   - Empty title validation
   - Special characters and unicode support
   - Rapid creation/deletion stress test

2. **ImageImportDisplayE2ETest.kt**
   - Image import flow
   - Cover image auto-assignment
   - Image removal
   - Page number sequencing
   - Metadata preservation

3. **SearchFunctionalityE2ETest.kt**
   - Title search (exact and partial)
   - Description search
   - Case insensitivity
   - Tag filtering
   - Combined search and filter
   - Unicode search

4. **BookDeletionE2ETest.kt**
   - Single book deletion
   - Cascading image deletion
   - Multiple book deletion
   - UI state updates
   - Error handling

5. **DataPersistenceE2ETest.kt**
   - Data persistence across operations
   - Book with images persistence
   - Search state persistence
   - Tag filter state persistence
   - Update persistence
   - Large dataset integrity
   - Timestamp ordering
   - Special character persistence

### Unit Tests (test)

Located in `app/src/test/java/com/maomaochongapp/picturebook/`:

- `PictureBookViewModelTest.kt` - ViewModel logic tests
- `BookRepositoryImplTest.kt` - Repository layer tests
- `ImageUtilsTest.kt` - Image utility tests
- `data/mapper/BookMappersTest.kt` - Entity-domain mapping tests

### Integration Tests (androidTest)

Located in `app/src/androidTest/java/com/maomaochongapp/picturebook/data/local/`:

- `BookDaoTest.kt` - DAO operation tests
- `BookDatabaseTest.kt` - Database configuration tests

## Test Architecture

### Testing Pyramid

```
           /\
          /  \       E2E Tests (5 classes)
         /----\      - Full user flows
        /      \     - UI integration
       /--------\
      /          \   Integration Tests (2 classes)
     /            \  - Database operations
    /--------------\ - DAO tests
   /                \
  /                  \ Unit Tests (4 classes)
 /--------------------\ - ViewModel logic
                        - Repository logic
                        - Utility functions
```

### Test Dependencies

```kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito:mockito-core:5.12.0")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("app.cash.turbine:turbine:1.1.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("org.robolectric:robolectric:4.11.1")

// Android Testing
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("io.mockk:mockk-android:1.13.10")
```

## Security Testing

The test suite validates security improvements:

### URI Validation
```kotlin
// Tests verify trusted authorities only
private val trustedAuthorities = setOf(
    "com.android.externalstorage.documents",
    "com.android.providers.downloads.documents",
    "com.android.providers.media.documents"
)
```

### Input Sanitization
```kotlin
// Tests verify title/description trimming
// Tests verify empty input rejection
// Tests verify special character handling
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run E2E Tests
  run: ./run_e2e_tests.sh --all

- name: Upload Test Results
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: build/test-results/

- name: Upload Test Report
  uses: actions/upload-artifact@v4
  with:
    name: test-report
    path: build/reports/androidTests/connected/
```

### Jenkins

```groovy
stage('E2E Tests') {
    steps {
        sh './run_e2e_tests.sh --all'
    }
    post {
        always {
            junit 'build/test-results/test/*.xml'
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAllHistory: true,
                reportDir: 'build/reports/androidTests/connected',
                reportFiles: 'index.html',
                reportName: 'E2E Test Report'
            ])
        }
    }
}
```

## Troubleshooting

### Common Issues

**Issue: No tests found**
```
Solution: Ensure emulator is running or device is connected
$ adb devices
```

**Issue: Tests timeout**
```
Solution: Increase test timeout in gradle.properties
android.testInstrumentationRunnerArguments.timeout=300000
```

**Issue: Flaky tests**
```
Solution: Run flaky detection
$ ./run_e2e_tests.sh --flaky

Quarantine flaky tests:
@Ignore("Flaky - Issue #123")
@Test
fun flakyTest() { ... }
```

### Test Logs

View detailed logs:
```bash
# Real-time logcat
adb logcat -s PictureBookTest:*

# After test run
adb pull /sdcard/Download/test-logs/ ./build/test-logs/
```

## Test Reports

### HTML Report

Open in browser:
```
build/reports/androidTests/connected/index.html
```

### JUnit XML

For CI integration:
```
build/test-results/test/TEST-*.xml
```

### Coverage Report

```
build/reports/coverage/index.html
```

## Performance Benchmarks

| Test Class | Avg Duration | Target |
|------------|--------------|--------|
| PictureBookCreationE2ETest | ~30s | < 60s |
| ImageImportDisplayE2ETest | ~45s | < 60s |
| SearchFunctionalityE2ETest | ~25s | < 30s |
| BookDeletionE2ETest | ~35s | < 60s |
| DataPersistenceE2ETest | ~50s | < 90s |

## Maintenance

### Adding New Tests

1. Create test class in appropriate directory
2. Follow naming convention: `*E2ETest.kt` for E2E, `*Test.kt` for unit tests
3. Add to test runner scripts if needed
4. Update this README

### Updating Test Data

```kotlin
// Use meaningful test data
viewModel.createBook(
    title = "Test Book Title",  // Descriptive name
    description = "Test Description",
    tags = listOf("test", "integration")
)

// Avoid magic strings
private const val TEST_BOOK_ID = "test-book-123"
```

### Test Best Practices

1. **Independent**: Each test should run independently
2. **Repeatable**: Tests should produce same result every time
3. **Fast**: Keep tests under 60 seconds
4. **Isolated**: Use in-memory database, mock external dependencies
5. **Readable**: Clear test names and assertions

## License

Internal use only - Picture Book Management System
