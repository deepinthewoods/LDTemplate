@echo off
echo Cleaning up cached texture files...

REM Remove cached palette files
if exist "temp\pal_*.png" (
    echo Removing cached palette files...
    del "temp\pal_*.png"
)

REM Remove cached noise texture files
if exist "temp\noise_*.png" (
    echo Removing cached noise texture files...
    del "temp\noise_*.png"
)

REM Remove cached clouds texture files
if exist "temp\clouds_*.png" (
    echo Removing cached clouds texture files...
    del "temp\clouds_*.png"
)

echo Cache cleanup completed!
pause
