#!/bin/bash

# =============================================================================
# Picture Book E2E Test Runner
# =============================================================================
# This script runs comprehensive end-to-end tests for the Android Picture Book
# Management System and generates detailed reports with artifacts.
#
# Usage:
#   ./run_e2e_tests.sh [options]
#
# Options:
#   --all              Run all E2E tests (default)
#   --quick            Run quick sanity tests only
#   --flaky            Run flaky test detection (repeat each test 10 times)
#   --headed           Run with browser UI visible (debug mode)
#   --test=NAME        Run specific test class
#   --help             Show this help message
#
# Test Flows Covered:
#   1. Book Creation Flow
#   2. Image Import Flow
#   3. Search and Filter Flow
#   4. Book Deletion Flow
#   5. Data Persistence Flow
# =============================================================================

set -e

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_REPORT_DIR="$PROJECT_DIR/build/reports/e2e-tests"
TEST_ARTIFACTS_DIR="$PROJECT_DIR/build/test-artifacts"
TEST_RESULTS_XML="$PROJECT_DIR/build/test-results/test/TEST-results.xml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# =============================================================================
# Helper Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

show_header() {
    echo "============================================================"
    echo "  Picture Book E2E Test Runner"
    echo "  $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================================"
    echo ""
}

show_help() {
    head -25 "$0" | tail -20
    exit 0
}

setup_directories() {
    log_info "Setting up test directories..."
    mkdir -p "$TEST_REPORT_DIR"
    mkdir -p "$TEST_ARTIFACTS"
    mkdir -p "$PROJECT_DIR/build/test-results/test"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if Gradle wrapper exists
    if [ ! -f "$PROJECT_DIR/gradlew" ]; then
        log_error "Gradle wrapper not found. Make sure you're in the project root."
        exit 1
    fi

    # Check if Android SDK is configured
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        log_warning "ANDROID_HOME/ANDROID_SDK_ROOT not set. Using project SDK."
    fi

    log_success "Prerequisites check passed"
}

# =============================================================================
# Test Execution Functions
# =============================================================================

run_all_tests() {
    log_info "Running all E2E tests..."
    echo ""

    # Run Android E2E tests
    ./gradlew connectedAndroidTest \
        --tests "com.maomaochongapp.picturebook.e2e.*" \
        --info \
        --stacktrace

    if [ $? -eq 0 ]; then
        log_success "All E2E tests passed!"
        ((TESTS_PASSED++))
    else
        log_error "Some E2E tests failed!"
        ((TESTS_FAILED++))
    fi
}

run_quick_tests() {
    log_info "Running quick sanity tests..."
    echo ""

    # Run only the book creation test as a sanity check
    ./gradlew connectedAndroidTest \
        --tests "com.maomaochongapp.picturebook.e2e.PictureBookCreationE2ETest" \
        --info

    if [ $? -eq 0 ]; then
        log_success "Quick sanity tests passed!"
        ((TESTS_PASSED++))
    else
        log_error "Quick sanity tests failed!"
        ((TESTS_FAILED++))
    fi
}

run_specific_test() {
    local test_class="$1"
    log_info "Running specific test: $test_class..."
    echo ""

    ./gradlew connectedAndroidTest \
        --tests "$test_class" \
        --info

    if [ $? -eq 0 ]; then
        log_success "Test $test_class passed!"
        ((TESTS_PASSED++))
    else
        log_error "Test $test_class failed!"
        ((TESTS_FAILED++))
    fi
}

run_flaky_detection() {
    log_info "Running flaky test detection (10 iterations)..."
    echo ""

    local test_classes=(
        "com.maomaochongapp.picturebook.e2e.PictureBookCreationE2ETest"
        "com.maomaochongapp.picturebook.e2e.ImageImportDisplayE2ETest"
        "com.maomaochongapp.picturebook.e2e.SearchFunctionalityE2ETest"
        "com.maomaochongapp.picturebook.e2e.BookDeletionE2ETest"
        "com.maomaochongapp.picturebook.e2e.DataPersistenceE2ETest"
    )

    for test_class in "${test_classes[@]}"; do
        log_info "Testing $test_class for flakiness..."

        local pass_count=0
        local fail_count=0

        for i in {1..10}; do
            echo -n "  Iteration $i/10: "

            if ./gradlew connectedAndroidTest \
                --tests "$test_class" \
                --quiet 2>/dev/null; then
                echo -e "${GREEN}PASS${NC}"
                ((pass_count++))
            else
                echo -e "${RED}FAIL${NC}"
                ((fail_count++))
            fi
        done

        if [ $fail_count -gt 0 ]; then
            log_warning "$test_class: $pass_count/10 passed, $fail_count/10 failed (FLAKY)"
        else
            log_success "$test_class: 10/10 passed (STABLE)"
        fi

        echo ""
    done
}

# =============================================================================
# Report Generation
# =============================================================================

generate_html_report() {
    log_info "Generating HTML test report..."

    local report_file="$TEST_REPORT_DIR/e2e-test-report.html"

    cat > "$report_file" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>E2E Test Report - Picture Book Management</title>
    <style>
        :root {
            --pass-color: #28a745;
            --fail-color: #dc3545;
            --warn-color: #ffc107;
            --bg-color: #f8f9fa;
            --card-bg: #ffffff;
            --text-color: #333333;
            --border-color: #dee2e6;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            background-color: var(--bg-color);
            color: var(--text-color);
            line-height: 1.6;
            padding: 20px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
        }

        header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        header h1 {
            font-size: 2rem;
            margin-bottom: 10px;
        }

        header p {
            opacity: 0.9;
        }

        .summary-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .card {
            background: var(--card-bg);
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            border-left: 4px solid var(--border-color);
        }

        .card.pass { border-left-color: var(--pass-color); }
        .card.fail { border-left-color: var(--fail-color); }
        .card.warn { border-left-color: var(--warn-color); }

        .card h3 {
            font-size: 0.875rem;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: 10px;
        }

        .card .value {
            font-size: 2.5rem;
            font-weight: bold;
        }

        .card.pass .value { color: var(--pass-color); }
        .card.fail .value { color: var(--fail-color); }
        .card.warn .value { color: var(--warn-color); }

        .test-section {
            background: var(--card-bg);
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            margin-bottom: 20px;
        }

        .test-section h2 {
            font-size: 1.25rem;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid var(--border-color);
        }

        .test-list {
            list-style: none;
        }

        .test-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 15px;
            border-bottom: 1px solid var(--border-color);
        }

        .test-item:last-child {
            border-bottom: none;
        }

        .test-item:hover {
            background-color: var(--bg-color);
        }

        .test-name {
            font-weight: 500;
        }

        .test-status {
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 0.75rem;
            font-weight: bold;
            text-transform: uppercase;
        }

        .test-status.pass {
            background-color: rgba(40, 167, 69, 0.1);
            color: var(--pass-color);
        }

        .test-status.fail {
            background-color: rgba(220, 53, 69, 0.1);
            color: var(--fail-color);
        }

        .test-status.flaky {
            background-color: rgba(255, 193, 7, 0.1);
            color: #b78900;
        }

        .progress-bar {
            height: 8px;
            background-color: var(--border-color);
            border-radius: 4px;
            overflow: hidden;
            margin-top: 10px;
        }

        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, var(--pass-color), var(--warn-color), var(--fail-color));
            transition: width 0.5s ease;
        }

        .artifacts-section {
            margin-top: 30px;
        }

        .artifact-list {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 15px;
        }

        .artifact-item {
            background: var(--card-bg);
            padding: 15px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            text-decoration: none;
            color: var(--text-color);
            transition: transform 0.2s;
        }

        .artifact-item:hover {
            transform: translateY(-2px);
        }

        .artifact-icon {
            font-size: 1.5rem;
            margin-bottom: 10px;
        }

        footer {
            text-align: center;
            padding: 30px;
            color: #666;
            font-size: 0.875rem;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>E2E Test Report</h1>
            <p>Picture Book Management System - Android</p>
            <p id="timestamp">Generated: </p>
        </header>

        <div class="summary-cards">
            <div class="card pass">
                <h3>Tests Passed</h3>
                <div class="value" id="passed-count">0</div>
            </div>
            <div class="card fail">
                <h3>Tests Failed</h3>
                <div class="value" id="failed-count">0</div>
            </div>
            <div class="card warn">
                <h3>Flaky Tests</h3>
                <div class="value" id="flaky-count">0</div>
            </div>
            <div class="card">
                <h3>Total Tests</h3>
                <div class="value" id="total-count">0</div>
            </div>
        </div>

        <div class="test-section">
            <h2>Test Coverage</h2>
            <div class="progress-bar">
                <div class="progress-fill" id="coverage-bar" style="width: 0%"></div>
            </div>
            <p style="margin-top: 10px; font-size: 0.875rem;">
                <span id="coverage-percent">0</span>% coverage across all user flows
            </p>
        </div>

        <div class="test-section">
            <h2>User Flows Tested</h2>
            <ul class="test-list">
                <li class="test-item">
                    <span class="test-name">1. Book Creation Flow - Create books with title, description, and tags</span>
                    <span class="test-status pass">Covered</span>
                </li>
                <li class="test-item">
                    <span class="test-name">2. Image Import Flow - Import images from device storage</span>
                    <span class="test-status pass">Covered</span>
                </li>
                <li class="test-item">
                    <span class="test-name">3. Search and Filter Flow - Search by title/description, filter by tags</span>
                    <span class="test-status pass">Covered</span>
                </li>
                <li class="test-item">
                    <span class="test-name">4. Book Deletion Flow - Delete books and verify image cleanup</span>
                    <span class="test-status pass">Covered</span>
                </li>
                <li class="test-item">
                    <span class="test-name">5. Data Persistence Flow - Verify data persists across operations</span>
                    <span class="test-status pass">Covered</span>
                </li>
            </ul>
        </div>

        <div class="test-section">
            <h2>E2E Test Classes</h2>
            <ul class="test-list">
                <li class="test-item">
                    <span class="test-name">PictureBookCreationE2ETest</span>
                    <span class="test-status pass">Ready</span>
                </li>
                <li class="test-item">
                    <span class="test-name">ImageImportDisplayE2ETest</span>
                    <span class="test-status pass">Ready</span>
                </li>
                <li class="test-item">
                    <span class="test-name">SearchFunctionalityE2ETest</span>
                    <span class="test-status pass">Ready</span>
                </li>
                <li class="test-item">
                    <span class="test-name">BookDeletionE2ETest</span>
                    <span class="test-status pass">Ready</span>
                </li>
                <li class="test-item">
                    <span class="test-name">DataPersistenceE2ETest</span>
                    <span class="test-status pass">Ready</span>
                </li>
            </ul>
        </div>

        <div class="test-section artifacts-section">
            <h2>Test Artifacts</h2>
            <div class="artifact-list">
                <a href="../test-results/test/TEST-results.xml" class="artifact-item">
                    <div class="artifact-icon">📊</div>
                    <strong>JUnit XML Results</strong>
                    <p style="font-size: 0.875rem; color: #666;">Machine-readable test results</p>
                </a>
                <a href="screenshots/" class="artifact-item">
                    <div class="artifact-icon">📸</div>
                    <strong>Screenshots</strong>
                    <p style="font-size: 0.875rem; color: #666;">Visual test evidence</p>
                </a>
                <a href="traces/" class="artifact-item">
                    <div class="artifact-icon">🎬</div>
                    <strong>Execution Traces</strong>
                    <p style="font-size: 0.875rem; color: #666;">Detailed execution logs</p>
                </a>
                <a href="../reports/androidTests/connected/index.html" class="artifact-item">
                    <div class="artifact-icon">📑</div>
                    <strong>Android Test Report</strong>
                    <p style="font-size: 0.875rem; color: #666;">Full Android test report</p>
                </a>
            </div>
        </div>

        <footer>
            <p>Picture Book Management System E2E Tests</p>
            <p>Run with: <code>./run_e2e_tests.sh</code></p>
        </footer>
    </div>

    <script>
        // Update timestamp
        document.getElementById('timestamp').textContent = 'Generated: ' + new Date().toLocaleString();

        // Update counts (these would be populated by the test runner)
        const passed = parseInt(localStorage.getItem('tests_passed') || '0');
        const failed = parseInt(localStorage.getItem('tests_failed') || '0');
        const total = passed + failed;
        const coverage = total > 0 ? Math.round((passed / total) * 100) : 0;

        document.getElementById('passed-count').textContent = passed || '-';
        document.getElementById('failed-count').textContent = failed || '-';
        document.getElementById('total-count').textContent = total || '-';
        document.getElementById('coverage-percent').textContent = coverage;
        document.getElementById('coverage-bar').style.width = coverage + '%';
    </script>
</body>
</html>
EOF

    log_success "HTML report generated: $report_file"
}

generate_junit_xml() {
    log_info "JUnit XML will be generated at: $TEST_RESULTS_XML"
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    show_header

    # Parse arguments
    case "${1:-}" in
        --all|"")
            run_all_tests
            ;;
        --quick)
            run_quick_tests
            ;;
        --flaky)
            run_flaky_detection
            ;;
        --test=*)
            test_name="${1#*=}"
            run_specific_test "$test_name"
            ;;
        --help|-h)
            show_help
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            ;;
    esac

    # Generate reports
    setup_directories
    generate_html_report
    generate_junit_xml

    # Summary
    echo ""
    echo "============================================================"
    echo "  Test Run Summary"
    echo "============================================================"
    echo -e "  ${GREEN}Passed:${NC}  $TESTS_PASSED"
    echo -e "  ${RED}Failed:${NC}  $TESTS_FAILED"
    echo "============================================================"

    # Exit with error if tests failed
    if [ $TESTS_FAILED -gt 0 ]; then
        exit 1
    fi
}

# Run main function
main "$@"
