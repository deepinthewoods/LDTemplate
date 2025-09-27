@echo off
setlocal enabledelayedexpansion

echo Starting sprite processing...

REM Create necessary directories
if not exist "processedSprites" mkdir "processedSprites"
if not exist "temp" mkdir "temp"

echo Created directories: processedSprites, temp

REM Call the blur processing stage
echo Starting blur stage...
call blurStage.bat
if !errorlevel! neq 0 (
    echo Error in blur stage. Stopping processing.
    exit /b !errorlevel!
)

echo Sprite processing completed successfully!
pause
