@echo off
setlocal enabledelayedexpansion

REM ===============================================================
REM Palette remap + animation-friendly dithering
REM Usage:
REM   script.bat [dither_mode] [param]
REM Examples:
REM   script.bat none
REM   script.bat ordered o8x8
REM   script.bat floyd 35
REM ===============================================================

REM ---- Params (with defaults)
set "dither_mode=ordered"
set "ordered_matrix=o8x8"
set "diffusion_amount=35"

if not "%~1"=="" set "dither_mode=%~1"
if not "%~2"=="" (
    if /I "%dither_mode%"=="ordered" set "ordered_matrix=%~2"
    if /I "%dither_mode%"=="floyd"   set "diffusion_amount=%~2"
)

echo Starting palette remap...
echo Dither mode: %dither_mode%
if /I "%dither_mode%"=="ordered" echo Ordered matrix: %ordered_matrix%
if /I "%dither_mode%"=="floyd"   echo Diffusion amount: %diffusion_amount%%%

REM ---- Check palette
if not exist "pal.png" (
    echo ERROR: pal.png not found. Cannot remap to palette.
    exit /b 1
)

REM ---- Process PNG frames
if not exist "processedSprites" (
    echo ERROR: processedSprites folder not found.
    exit /b 1
)

if not exist "temp" mkdir "temp"

cd processedSprites

for %%f in (*.png) do (
    set "filename=%%f"
    if /I not "!filename:~0,7!"=="effect_" (
        echo Processing: %%f

        REM Get sprite dimensions (optional, but useful if you later need it)
        for /f "tokens=1,2" %%a in ('magick identify -format "%%w %%h" "%%f"') do (
            set "sprite_width=%%a"
            set "sprite_height=%%b"
        )

        REM Extract alpha
        magick "%%f" -alpha extract "..\temp\alpha_%%f"

        REM Prepare RGB (no alpha) in sRGB
        magick "%%f" -alpha off -colorspace sRGB "..\temp\sprite_%%f"

        REM ---- Remap + Dither (apply LAST to avoid temporal drift)
        if /I "%dither_mode%"=="none" (
            magick "..\temp\sprite_%%f" +dither -remap "..\pal.png" "..\temp\final_noalpha_%%f"
        ) else if /I "%dither_mode%"=="ordered" (
            magick "..\temp\sprite_%%f" -remap "..\pal.png" -ordered-dither %ordered_matrix% "..\temp\final_noalpha_%%f"
        ) else if /I "%dither_mode%"=="floyd" (
            magick "..\temp\sprite_%%f" -define dither:diffusion-amount=%diffusion_amount%%% -dither FloydSteinberg -remap "..\pal.png" "..\temp\final_noalpha_%%f"
        ) else (
            echo WARNING: Unknown dither_mode "%dither_mode%". Falling back to ordered %ordered_matrix%.
            magick "..\temp\sprite_%%f" -remap "..\pal.png" -ordered-dither %ordered_matrix% "..\temp\final_noalpha_%%f"
        )

        REM Restore alpha
        magick "..\temp\final_noalpha_%%f" "..\temp\alpha_%%f" -compose Copy_Alpha -composite "%%f"

        echo Completed: %%f
    )
)

cd ..

echo Palette remap completed!
