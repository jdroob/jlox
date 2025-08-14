#!/bin/bash

# JLox Test Runner Script
# Runs all .lox test files in the test directory and logs results

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JLOX_DIR="$SCRIPT_DIR"
TEST_DIR="$JLOX_DIR/test"
LOG_FILE="$JLOX_DIR/test_results.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Initialize counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Clear previous log
> "$LOG_FILE"

echo "=======================================" | tee -a "$LOG_FILE"
echo "JLox Test Runner - $(date)" | tee -a "$LOG_FILE"
echo "=======================================" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

# Check if jlox can be run
echo "Checking if jlox is built..." | tee -a "$LOG_FILE"
if [[ ! -f "$JLOX_DIR/bin/com/craftinginterpreters/lox/Lox.class" ]]; then
    echo -e "${YELLOW}Building jlox...${NC}" | tee -a "$LOG_FILE"
    cd "$JLOX_DIR"
    if ! make 2>&1 | tee -a "$LOG_FILE"; then
        echo -e "${RED}Failed to build jlox. Exiting.${NC}" | tee -a "$LOG_FILE"
        exit 1
    fi
fi

echo "" | tee -a "$LOG_FILE"
echo "Running tests from: $TEST_DIR" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

# Run each test file
for test_file in "$TEST_DIR"/*.lox; do
    if [[ -f "$test_file" ]]; then
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        test_name=$(basename "$test_file")
        
        echo -n "Running $test_name... " | tee -a "$LOG_FILE"
        
        # Run the test and capture output and exit code
        cd "$JLOX_DIR"
        output=$(java -cp bin com.craftinginterpreters.lox.Lox "$test_file" 2>&1)
        exit_code=$?
        
        # Log detailed results
        echo "" >> "$LOG_FILE"
        echo "=== Test: $test_name ===" >> "$LOG_FILE"
        echo "Exit code: $exit_code" >> "$LOG_FILE"
        echo "Output:" >> "$LOG_FILE"
        echo "$output" >> "$LOG_FILE"
        echo "" >> "$LOG_FILE"
        
        # Determine if test passed or failed
        # Consider exit code 0 as pass, non-zero as fail
        # You might want to adjust this logic based on your test expectations
        if [[ $exit_code -eq 0 ]]; then
            echo -e "${GREEN}PASSED${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            echo "Result: PASSED" >> "$LOG_FILE"
        else
            echo -e "${RED}FAILED${NC}"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            echo "Result: FAILED" >> "$LOG_FILE"
            
            # Show error output in terminal for failed tests
            if [[ -n "$output" ]]; then
                echo -e "${RED}Error output:${NC}"
                echo "$output" | head -5  # Show first 5 lines of error
            fi
        fi
        echo "" | tee -a "$LOG_FILE"
    fi
done

# Summary
echo "=======================================" | tee -a "$LOG_FILE"
echo "Test Summary:" | tee -a "$LOG_FILE"
echo "Total tests: $TOTAL_TESTS" | tee -a "$LOG_FILE"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}" | tee -a "$LOG_FILE"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}" | tee -a "$LOG_FILE"

if [[ $FAILED_TESTS -eq 0 ]]; then
    echo -e "${GREEN}All tests passed!${NC}" | tee -a "$LOG_FILE"
    exit 0
else
    echo -e "${RED}$FAILED_TESTS test(s) failed.${NC}" | tee -a "$LOG_FILE"
    echo "Check $LOG_FILE for detailed results." | tee -a "$LOG_FILE"
    exit 1
fi
