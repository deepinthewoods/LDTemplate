@echo off
setlocal enabledelayedexpansion

echo Starting effects sprite processing...

REM Check if base textures exist
if not exist "temp\noise_texture.png" (
    echo Error: noise_texture.png not found. Please run generateTextures.bat first.
    exit /b 1
)
if not exist "temp\clouds_texture.png" (
    echo Error: clouds_texture.png not found. Please run generateTextures.bat first.
    exit /b 1
)

REM Process each PNG file in the sprites folder to create effects versions
for %%f in ("sprites\*.png") do (
    echo Processing effects version: %%f
    
    REM Step 1: Create distance map from transparent pixels (distance transform)
    REM This creates a grayscale image where edge pixels are white (255) and center pixels are black (0)
    magick "%%f" ^
        -alpha extract ^
        -morphology Distance Euclidean ^
        -auto-level ^
        -negate "temp\distance_%%~nxf"
    
    if !errorlevel! neq 0 (
        echo Error creating distance map for %%f
        exit /b !errorlevel!
    )
    
    REM Step 2: Get the sprite dimensions for cropping textures
    for /f "tokens=1,2" %%a in ('magick identify -format "%%w %%h" "%%f"') do (
        set width=%%a
        set height=%%b
    )
    
    REM Step 3: Get cached noise and cloud textures for this sprite size
    set noise_cache_file=temp\noise_!width!x!height!.png
    set clouds_cache_file=temp\clouds_!width!x!height!.png
    
    if not exist "!noise_cache_file!" (
        echo Creating cached noise texture for resolution !width!x!height!
        magick "temp\noise_texture.png" -crop !width!x!height!+0+0 "!noise_cache_file!"
        if !errorlevel! neq 0 (
            echo Error creating cached noise texture for %%f
            exit /b !errorlevel!
        )
    ) else (
        echo Using cached noise texture for resolution !width!x!height!
    )
    
    if not exist "!clouds_cache_file!" (
        echo Creating cached clouds texture for resolution !width!x!height!
        magick "temp\clouds_texture.png" -crop !width!x!height!+0+0 "!clouds_cache_file!"
        if !errorlevel! neq 0 (
            echo Error creating cached clouds texture for %%f
            exit /b !errorlevel!
        )
    ) else (
        echo Using cached clouds texture for resolution !width!x!height!
    )
    
    REM Step 4: Combine into RGB channels using cached textures
    magick "!noise_cache_file!" ^
        "!clouds_cache_file!" ^
        "temp\distance_%%~nxf" ^
        -channel RGB -combine "temp\rgb_combined_%%~nxf"
    
    if !errorlevel! neq 0 (
        echo Error combining RGB channels for %%f
        exit /b !errorlevel!
    )
    
    REM Step 5: Extract alpha channel from original sprite
    magick "%%f" -alpha extract "temp\alpha_%%~nxf"
    
    if !errorlevel! neq 0 (
        echo Error extracting alpha channel for %%f
        exit /b !errorlevel!
    )
    
    REM Step 6: Combine RGB with extracted alpha channel
    magick "temp\rgb_combined_%%~nxf" "temp\alpha_%%~nxf" ^
        -alpha off ^
        -compose Copy_Alpha -composite ^
        "processedSprites\effect_%%~nxf"
    
    if !errorlevel! neq 0 (
        echo Error combining with alpha channel for %%f
        exit /b !errorlevel!
    )
    
    echo Completed effects version: effect_%%~nxf
)

echo Effects processing completed successfully!
