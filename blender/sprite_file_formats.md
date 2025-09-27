# Sprite Rendering System File Formats

This document explains the file formats used by the sprite rendering system in this Blender project.

## Overview

The render script processes objects with underscore prefixes (`_`) and generates two types of output:
1. **Sprite Images** - PNG sequences for visual sprites  
2. **Guide Data** - Binary files with coordinate information

## Directory Structure

```
project/
├── sprites/           # Rendered sprite images
├── guides/            # Binary guide data files  
├── assets/            # Final packed textures (TexturePacker output)
└── sprites.blend      # This Blender file
```

## Sprite Images

### File Format
- **Format**: PNG image sequence
- **Location**: `sprites/` directory
- **Naming**: `{objectname}_{actionname}_{frame}.png`

### Examples
```
sprites/
├── player_walk_0001.png
├── player_walk_0002.png
├── player_idle_0001.png
├── enemy_attack_0001.png
└── ...
```

### How to Create
1. Name your sprite object with `_` prefix (e.g., `_player`)
2. Create animation actions in Blender
3. Run the render script
4. Script automatically renders all actions for all `_` objects

## Guide Data Files

Guide files contain coordinate information for attachment points, collision detection, or UI positioning.

### Types of Guide Objects

#### 1. Traditional Guide Objects
- **Objects**: Any object with "guide" in the name
- **Output**: Single coordinate pair per frame

#### 2. Armature Bone Guides  
- **Objects**: Armatures with `_` prefix (e.g., `_player_rig`)
- **Output**: Bone endpoint coordinates per frame

### File Formats

#### Traditional Guide Format
- **File Extension**: None (binary file)
- **Location**: `guides/` directory  
- **Naming**: `{objectname}_guide`
- **Data Structure**:
  ```
  For each frame:
    - X coordinate (4 bytes, big-endian signed int)
    - Y coordinate (4 bytes, big-endian signed int)
  ```

#### Armature Bone Guide Format
- **File Extension**: None (binary file)
- **Location**: `guides/` directory
- **Naming**: `{objectname}_{bonename}_{actionname}`
- **Data Structure**:
  ```
  For each frame:
    - Head X coordinate (4 bytes, big-endian signed int)
    - Head Y coordinate (4 bytes, big-endian signed int)  
    - Tail X coordinate (4 bytes, big-endian signed int)
    - Tail Y coordinate (4 bytes, big-endian signed int)
  ```

### Examples

#### Traditional Guide Files
```
guides/
├── player_weaponpoint_guide    # Single point tracking
├── enemy_hitbox_guide          # Collision detection point
└── ui_healthbar_guide          # UI attachment point
```

#### Armature Bone Guide Files
```
guides/
├── player_hand.L_walk          # Left hand bone during walk animation
├── player_hand.L_idle          # Left hand bone during idle animation
├── player_hand.R_attack        # Right hand bone during attack animation
├── player_head_walk            # Head bone during walk animation
└── player_weapon_point_idle    # Custom weapon attachment bone
```

## Coordinate System

All coordinates are in **screen space pixels** relative to the render camera:
- **Origin**: Top-left corner (0, 0)
- **X-axis**: Left to right (positive = right)
- **Y-axis**: Top to bottom (positive = down)
- **Resolution**: Matches render settings (e.g., 1920×1080)

## Reading Guide Files

### Python Example
```python
import struct

def read_traditional_guide(filepath, frame_count):
    """Read traditional guide file (single point per frame)"""
    coords = []
    with open(filepath, 'rb') as f:
        for frame in range(frame_count):
            x = struct.unpack('>i', f.read(4))[0]
            y = struct.unpack('>i', f.read(4))[0]
            coords.append((x, y))
    return coords

def read_bone_guide(filepath, frame_count):
    """Read bone guide file (head + tail per frame)"""
    bone_data = []
    with open(filepath, 'rb') as f:
        for frame in range(frame_count):
            head_x = struct.unpack('>i', f.read(4))[0]
            head_y = struct.unpack('>i', f.read(4))[0]
            tail_x = struct.unpack('>i', f.read(4))[0]
            tail_y = struct.unpack('>i', f.read(4))[0]
            bone_data.append({
                'head': (head_x, head_y),
                'tail': (tail_x, tail_y)
            })
    return bone_data
```

### C# Example (Unity)
```csharp
using System.IO;

public class GuideReader 
{
    public static Vector2[] ReadTraditionalGuide(string filepath, int frameCount) 
    {
        var coords = new Vector2[frameCount];
        using (var reader = new BinaryReader(File.Open(filepath, FileMode.Open))) 
        {
            for (int i = 0; i < frameCount; i++) 
            {
                int x = System.Net.IPAddress.NetworkToHostOrder(reader.ReadInt32());
                int y = System.Net.IPAddress.NetworkToHostOrder(reader.ReadInt32());
                coords[i] = new Vector2(x, y);
            }
        }
        return coords;
    }
    
    public static BoneFrame[] ReadBoneGuide(string filepath, int frameCount) 
    {
        var frames = new BoneFrame[frameCount];
        using (var reader = new BinaryReader(File.Open(filepath, FileMode.Open))) 
        {
            for (int i = 0; i < frameCount; i++) 
            {
                int headX = System.Net.IPAddress.NetworkToHostOrder(reader.ReadInt32());
                int headY = System.Net.IPAddress.NetworkToHostOrder(reader.ReadInt32());
                int tailX = System.Net.IPAddress.NetworkToHostOrder(reader.ReadInt32());
                int tailY = System.Net.IPAddress.NetworkToHostOrder(reader.ReadInt32());
                
                frames[i] = new BoneFrame {
                    head = new Vector2(headX, headY),
                    tail = new Vector2(tailX, tailY)
                };
            }
        }
        return frames;
    }
}

public struct BoneFrame 
{
    public Vector2 head;
    public Vector2 tail;
}
```

## Usage Examples

### Weapon Attachment
1. Create bone named `weapon_point` in your armature
2. Name armature `_player`  
3. Render generates: `guides/player_weapon_point_attack`
4. In game: Read coordinates to position weapon sprite on player's hand

### Collision Detection
1. Create guide object named `_enemy_hitbox_guide`
2. Position it at enemy's vulnerable spot
3. Render generates: `guides/enemy_hitbox_guide`
4. In game: Use coordinates for hit detection

### UI Elements
1. Create bone named `health_ui` above character's head
2. Render generates: `guides/player_health_ui_walk`
3. In game: Position health bar at these coordinates

## Script Configuration

### Key Variables in Render Script
```python
basePrefix = ""              # Added to all output filenames
basePath = "//sprites/"      # Sprite output directory  
guidePath = "/guides/"       # Guide data output directory
```

### Render Settings
- Resolution affects coordinate values
- Camera position/angle affects coordinate mapping
- Animation frame range determines file sizes

## TexturePacker Integration

After rendering, the script automatically runs TexturePacker:
```bash
java -cp runnable-texturepacker.jar com.badlogic.gdx.tools.texturepacker.TexturePacker sprites ../assets
```

This creates optimized sprite atlases in the `assets/` directory for use in game engines.

---

*Generated by the sprite rendering system documentation generator*
