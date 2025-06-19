#!/bin/bash
# filepath: d:\Progetti\Hazelcast In-Depth\run.sh

RUN_JAVA=true
RUN_PYTHON=true
PROJECT_ROOT=$(dirname "$(realpath "$0")")

# Check command line arguments
if [ "$1" = "java" ]; then
    RUN_PYTHON=false
    echo "Running Java tests only..."
elif [ "$1" = "python" ]; then
    RUN_JAVA=false
    echo "Running Java tests only..."
elif [ -z "$1" ]; then
    echo "Running all tests..."
else
    echo "Invalid argument: $1"
    echo "Usage: run.sh [java|python]"
    echo "  No argument: Run both Java and Python tests"
    echo "  java: Run only Java tests"
    echo "  python: Run only Python tests"
    exit 1
fi

# Run Java tests if specified
if [ "$RUN_JAVA" = "true" ]; then
    echo
    echo "=== Running Java Tests ==="
    echo
    cd "$PROJECT_ROOT/tests/java"
    ./gradlew app:run
    ./gradlew app:test
    cd "$PROJECT_ROOT"
fi

# Run Python tests if specified
if [ "$RUN_PYTHON" = "true" ]; then
    echo
    echo "=== Running Python Tests ==="
    echo
    cd "$PROJECT_ROOT/tests/python"
    
    # Check if virtual environment exists, create if not
    if [ ! -d ".venv" ]; then
        echo "Creating Python virtual environment..."
        python3 -m venv .venv
        
        # Activate venv and install requirements if present
        source .venv/bin/activate
        if [ -f "requirements.txt" ]; then
            echo "Installing requirements..."
            pip install -r requirements.txt
        fi
        deactivate
    fi
    
    # Run the tests with the virtual environment
    source .venv/bin/activate
    python main.py
    deactivate
    
    cd "$PROJECT_ROOT"
fi

echo
echo "Test execution complete."
exit 0
