@echo off
setlocal enabledelayedexpansion

REM Parse command line arguments
set dither_mode=floyd
set dither_param=15
if "%1" neq "" set dither_mode=%1
if "%2" neq "" set dither_param=%2

echo Starting sprite processing...
echo Dither mode: %dither_mode%
echo Dither parameter: %dither_param%
echo.
echo Usage: processSprites.bat [dither_mode] [dither_param]
echo   dither_mode: none, ordered, or floyd (default: ordered)
echo   dither_param: For ordered: matrix like o8x8, o4x4, o2x2 (default: o8x8)
echo                 For floyd: diffusion amount 0-100 (default: 35)
echo   Examples: 
echo     processSprites.bat ordered o4x4
echo     processSprites.bat floyd 35
echo     processSprites.bat none
echo.
echo Usage: processSprites.bat [extra_colors] [dither_method]
echo   extra_colors: Additional colors to add to the palette (default: 0)
echo   dither_method: Ordered dithering pattern (default: o4x4)
echo   Examples: 
echo     processSprites.bat 5
echo     processSprites.bat 5 o8x8
echo     processSprites.bat 0 o2x2
echo.
echo Note: Texture resizing is cached by resolution for performance.
echo Use cleanCache.bat to clear cached files if needed.
echo.

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

REM Call the palette processing stage
echo Starting palette stage...
call paletteStage.bat %dither_mode% %dither_param%
if !errorlevel! neq 0 (
    echo Error in palette stage. Stopping processing.
    exit /b !errorlevel!
)
)

REM Call the effects processing stage
echo Starting effects stage...
call effectsStage.bat
if !errorlevel! neq 0 (
    echo Error in effects stage. Stopping processing.
    exit /b !errorlevel!
)

echo Sprite processing completed successfully!
pause
