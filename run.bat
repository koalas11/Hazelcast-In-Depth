@echo off
setlocal

set RUN_JAVA=true
set RUN_PYTHON=true
set PROJECT_ROOT=%~dp0

:: Check command line arguments
if "%1"=="java" (
    set RUN_PYTHON=false
    echo Running Java tests only...
) else if "%1"=="python" (
    set RUN_JAVA=false
    echo Running Python tests only...
) else if "%1"=="" (
    echo Running all tests...
) else (
    echo Invalid argument: %1
    echo Usage: run.bat [java^|python]
    echo   No argument: Run both Java and Python tests
    echo   java: Run only Java tests
    echo   python: Run only Python tests
    exit /b 1
)

:: Run Java tests if specified
if "%RUN_JAVA%"=="true" (
    echo.
    echo === Running Java Tests ===
    echo.
    cd %PROJECT_ROOT%\tests\java
    call .\gradlew.bat app:run
    call .\gradlew.bat app:test
    cd %PROJECT_ROOT%
)

:: Run Python tests if specified
if "%RUN_PYTHON%"=="true" (
    echo.
    echo === Running Python Tests ===
    echo.
    cd %PROJECT_ROOT%\tests\python
    
    :: Check if virtual environment exists, create if not
    if not exist ".venv" (
        echo Creating Python virtual environment...
        py -m venv .venv
        
        :: Activate venv and install requirements if present
        call .venv\Scripts\activate
        if exist "requirements.txt" (
            echo Installing requirements...
            pip install -r requirements.txt
        )
        deactivate
    )
    
    :: Run the tests with the virtual environment
    call .venv\Scripts\activate
    py main.py
    deactivate
    
    cd %PROJECT_ROOT%
)

echo.
echo Test execution complete.
exit /b 0
