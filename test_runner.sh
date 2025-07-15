#!/bin/bash

echo "=========================================="
echo "JLox Test Runner"
echo "$(date)"
echo "=========================================="
echo ""

cd /home/johnrubi/training/crafting_interpreters/jlox

# Initialize counters
total=0
passed=0
failed=0

# Create log file
{
    echo "=========================================="
    echo "JLox Test Runner"
    echo "$(date)"
    echo "=========================================="
    echo ""
} > test_results.log

echo "Running all tests in test/ directory..."
echo ""

# Run each test
for test_file in test/*.lox; do
    if [ -f "$test_file" ]; then
        total=$((total + 1))
        test_name=$(basename "$test_file")
        
        printf "%-30s" "$test_name"
        
        # Run test and capture output
        output=$(java -cp bin com.craftinginterpreters.lox.Lox "$test_file" 2>&1)
        exit_code=$?
        
        # Log detailed results
        {
            echo "=== Test: $test_name ==="
            echo "Exit code: $exit_code"
            echo "Output:"
            echo "$output"
            echo ""
        } >> test_results.log
        
        # Check result
        if [ $exit_code -eq 0 ]; then
            echo " [PASSED]"
            passed=$((passed + 1))
        else
            echo " [FAILED]"
            failed=$((failed + 1))
            echo "  Error: $output" | head -1
        fi
    fi
done

echo ""
echo "=========================================="
echo "Results Summary:"
echo "Total tests: $total"
echo "Passed: $passed"
echo "Failed: $failed"
echo "=========================================="

# Log summary
{
    echo "=========================================="
    echo "Results Summary:"
    echo "Total tests: $total"
    echo "Passed: $passed"
    echo "Failed: $failed"
    echo "=========================================="
} >> test_results.log

if [ $failed -eq 0 ]; then
    echo "All tests passed!"
    exit 0
else
    echo "$failed test(s) failed. Check test_results.log for details."
    exit 1
fi
