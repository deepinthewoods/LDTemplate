# Dithering Options for Sprite Processing

## Available Ordered Dithering Patterns

ImageMagick supports several ordered dithering patterns that work well for pixel art and animations:

### Bayer Patterns (Most Common)
- **o2x2** - 2x2 Bayer matrix (smallest, least dithering)
- **o4x4** - 4x4 Bayer matrix (default, good balance)
- **o8x8** - 8x8 Bayer matrix (larger pattern, more dithering)

### Other Ordered Patterns
- **o3x3** - 3x3 ordered pattern
- **o6x6** - 6x6 ordered pattern
- **checks** - Checkerboard pattern
- **h4x4a** - Halftone 4x4 pattern A
- **h6x6a** - Halftone 6x6 pattern A
- **h8x8a** - Halftone 8x8 pattern A

## Recommendations

**For pixel art animations:**
- **o2x2** or **o4x4** - These provide subtle dithering without being too noticeable
- Smaller patterns are less likely to create visual artifacts in small sprites

**For larger sprites:**
- **o4x4** or **o8x8** - These can provide better color gradients
- Larger patterns work better with more pixels to work with

**For retro/vintage look:**
- **checks** - Classic checkerboard dithering
- **h4x4a** or **h6x6a** - Halftone patterns for a newspaper print effect

## Usage Examples

```batch
REM No dithering (default behavior without this feature)
processSprites.bat 0 none

REM Subtle 2x2 Bayer dithering
processSprites.bat 0 o2x2

REM Default 4x4 Bayer dithering with 5 extra colors
processSprites.bat 5 o4x4

REM Strong 8x8 Bayer dithering
processSprites.bat 0 o8x8

REM Checkerboard dithering
processSprites.bat 0 checks
```

## Notes

- Ordered dithering is **consistent between frames**, making it perfect for animations
- The pattern repeats in a predictable way, so moving sprites won't flicker
- Smaller matrices (o2x2, o4x4) are generally better for small pixel art sprites
- All dithering is applied only to RGB values; alpha channels are preserved exactly
