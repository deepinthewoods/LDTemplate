@echo off
echo Generating 1024x1024 base textures...

REM Create temp directory if it doesn't exist
if not exist "temp" mkdir "temp"

REM Generate RGB noise texture first
echo Generating RGB noise texture...
magick -size 1024x1024 xc: +noise Random "temp/noise_rgb.png"
if %errorlevel% neq 0 (
    echo Error generating RGB noise texture
    exit /b %errorlevel%
)

REM Convert RGB noise to grayscale
echo Converting RGB noise to grayscale...
magick "temp/noise_rgb.png" -colorspace Gray "temp/noise.png"
if %errorlevel% neq 0 (
    echo Error converting to grayscale
    exit /b %errorlevel%
)



echo Base textures generated successfully!
echo - noise_rgb.png (RGB noise)
echo - noise.png (grayscale noise)

pause
