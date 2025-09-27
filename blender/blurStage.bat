@echo off
setlocal enabledelayedexpansion

echo Starting blur stage processing...

REM Process each PNG file in the sprites folder
for %%f in ("sprites\*.png") do (
    echo Processing: %%f
    
    REM Use ImageMagick to blur the whole image then composite original on top
    REM Step 1: Create blurred version in temp folder
    magick "%%f" -blur 0x3 "temp\%%~nxf"
    if !errorlevel! neq 0 (
        echo Error creating blurred version of %%f
        exit /b !errorlevel!
    )
    
    REM Step 2: Composite original on top of blurred version
    magick "temp\%%~nxf" "%%f" -compose Over -composite "processedSprites\%%~nxf"
    if !errorlevel! neq 0 (
        echo Error compositing %%f
        exit /b !errorlevel!
    )
    
    echo Completed: %%~nxf
)

echo Blur stage completed successfully!
