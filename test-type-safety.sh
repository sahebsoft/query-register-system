#!/bin/bash

echo "=========================================="
echo "Type Safety and Conversion Test Suite"
echo "=========================================="

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8080/api/query/employees"

# Counter for tests
PASSED=0
FAILED=0

# Function to run test
run_test() {
    local test_name="$1"
    local url="$2"
    local expected_pattern="$3"
    
    echo -e "\n${YELLOW}Test: ${test_name}${NC}"
    echo "URL: ${url}"
    
    response=$(curl -s "${url}")
    
    if echo "${response}" | grep -q "${expected_pattern}"; then
        echo -e "${GREEN}✓ PASSED${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        echo "Response: ${response:0:200}..."
        ((FAILED++))
        return 1
    fi
}

echo -e "\n${YELLOW}Starting Spring Boot application...${NC}"
cd /srv/projects/query-register-system
mvn spring-boot:run > /tmp/spring-boot.log 2>&1 &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP"; then
        echo -e "${GREEN}Application started successfully${NC}"
        break
    fi
    sleep 2
done

echo -e "\n=========================================="
echo "1. Testing Integer Type Conversion"
echo "=========================================="

run_test "Integer param - departmentId" \
    "${BASE_URL}?param.departmentId=50" \
    "\"departmentId\":50"

run_test "Invalid Integer - should handle gracefully" \
    "${BASE_URL}?param.departmentId=abc" \
    "error"

echo -e "\n=========================================="
echo "2. Testing BigDecimal Type Conversion"
echo "=========================================="

run_test "BigDecimal filter - minSalary" \
    "${BASE_URL}?filter.salary.gte=5000.50" \
    "salary"

run_test "BigDecimal param - minSalary" \
    "${BASE_URL}?param.minSalary=10000.75" \
    "\"minSalary\""

run_test "Invalid BigDecimal" \
    "${BASE_URL}?param.minSalary=not-a-number" \
    "error"

echo -e "\n=========================================="
echo "3. Testing LocalDate Type Conversion"
echo "=========================================="

run_test "LocalDate filter - hireDate" \
    "${BASE_URL}?filter.hireDate.gte=2020-01-01" \
    "hireDate"

run_test "LocalDate param - minHireDate" \
    "${BASE_URL}?param.minHireDate=2020-01-01" \
    "\"minHireDate\""

run_test "Invalid LocalDate format" \
    "${BASE_URL}?param.minHireDate=01/01/2020" \
    "error"

echo -e "\n=========================================="
echo "4. Testing Boolean Type Conversion"
echo "=========================================="

run_test "Boolean param - includeInactive (true)" \
    "${BASE_URL}?param.includeInactive=true" \
    "\"includeInactive\":true"

run_test "Boolean param - includeInactive (false)" \
    "${BASE_URL}?param.includeInactive=false" \
    "\"includeInactive\":false"

run_test "Boolean with string 'yes'" \
    "${BASE_URL}?param.includeInactive=yes" \
    "\"includeInactive\":true"

echo -e "\n=========================================="
echo "5. Testing List Type Conversion (IN clause)"
echo "=========================================="

run_test "List<Integer> - departmentIds" \
    "${BASE_URL}?param.departmentIds=10,20,30" \
    "departmentIds"

run_test "List<String> - jobIds" \
    "${BASE_URL}?param.jobIds=IT_PROG,ST_CLERK,SA_REP" \
    "jobIds"

run_test "Single value list" \
    "${BASE_URL}?param.departmentIds=50" \
    "departmentIds"

echo -e "\n=========================================="
echo "6. Testing String Type (No Conversion)"
echo "=========================================="

run_test "String filter - firstName" \
    "${BASE_URL}?filter.firstName=John" \
    "firstName"

run_test "String with special chars" \
    "${BASE_URL}?filter.firstName=O'Neil" \
    "firstName"

echo -e "\n=========================================="
echo "7. Testing Multiple Type Conversions Together"
echo "=========================================="

run_test "Mixed types in single request" \
    "${BASE_URL}?param.departmentId=50&param.minSalary=5000.00&param.minHireDate=2020-01-01&param.includeInactive=true&param.departmentIds=10,20,30" \
    "data"

echo -e "\n=========================================="
echo "8. Testing Type Mismatch Handling"
echo "=========================================="

run_test "String value for Integer param" \
    "${BASE_URL}?param.departmentId=fifty" \
    "error"

run_test "Invalid date format" \
    "${BASE_URL}?filter.hireDate.gte=2020-13-45" \
    "error"

run_test "Decimal for Integer" \
    "${BASE_URL}?param.departmentId=50.5" \
    "error"

echo -e "\n=========================================="
echo "9. Testing Null and Empty Values"
echo "=========================================="

run_test "Empty param value" \
    "${BASE_URL}?param.departmentId=" \
    "data"

run_test "Empty list" \
    "${BASE_URL}?param.departmentIds=" \
    "data"

echo -e "\n=========================================="
echo "10. Testing Filter Operators with Types"
echo "=========================================="

run_test "BETWEEN with BigDecimal" \
    "${BASE_URL}?filter.salary.between=5000,10000" \
    "salary"

run_test "IN with Integers" \
    "${BASE_URL}?filter.departmentId.in=10,20,30" \
    "departmentId"

run_test "LIKE with String" \
    "${BASE_URL}?filter.firstName.like=J%" \
    "firstName"

# Cleanup
echo -e "\n=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}Passed: ${PASSED}${NC}"
echo -e "${RED}Failed: ${FAILED}${NC}"

if [ ${FAILED} -eq 0 ]; then
    echo -e "\n${GREEN}All type safety tests passed!${NC}"
else
    echo -e "\n${RED}Some tests failed. Please review.${NC}"
fi

echo -e "\n${YELLOW}Stopping application...${NC}"
kill ${APP_PID} 2>/dev/null
wait ${APP_PID} 2>/dev/null

exit ${FAILED}