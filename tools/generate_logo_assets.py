import os
from PIL import Image, ImageDraw, ImageFilter
import numpy as np
from scipy.ndimage import label, binary_dilation, binary_erosion

def generate_logo_image(shape1, shape2, bbox, base_color, base_border, upright_color, upright_border, canvas_size=512):
    out = np.zeros((500, 500, 4), dtype=np.uint8)

    s2_border = binary_dilation(shape2, iterations=2) & ~binary_erosion(shape2, iterations=2)
    out[shape2] = base_color
    out[s2_border] = base_border

    s1_border = binary_dilation(shape1, iterations=2) & ~binary_erosion(shape1, iterations=2)
    out[shape1] = upright_color
    out[s1_border] = upright_border

    img = Image.fromarray(out)
    cropped = img.crop(bbox)

    final_img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    w, h = cropped.size
    scale = (canvas_size * 0.82) / max(w, h)
    nw, nh = int(w * scale), int(h * scale)
    scaled = cropped.resize((nw, nh), Image.Resampling.LANCZOS)
    final_img.paste(scaled, ((canvas_size - nw) // 2, (canvas_size - nh) // 2), scaled)
    return final_img, cropped

def main():
    src_path = r"C:/Users/admin/.gemini/antigravity/brain/18a79ba9-86b0-46cd-9292-a995035b84ae/.user_uploaded/media_1794144952896_8f913622.png"
    if not os.path.exists(src_path):
        print(f"Source not found: {src_path}")
        return

    orig = Image.open(src_path).convert("RGBA")
    arr = np.array(orig)
    alpha = arr[:, :, 3] > 80

    labeled, num_features = label(alpha)
    if num_features < 2:
        print("Could not isolate shapes")
        return

    shape1 = (labeled == 1) # vertical upright card
    shape2 = (labeled == 2) # horizontal base card

    # Calculate tight bounding box of both shapes
    combined = shape1 | shape2
    dummy = Image.fromarray((combined * 255).astype(np.uint8))
    bbox = dummy.getbbox()

    res_dir = r"d:\Project_A\Lokap\app\src\main\res"
    drawable_dir = os.path.join(res_dir, "drawable")
    os.makedirs(drawable_dir, exist_ok=True)

    # 1. Dark theme logo (White base card)
    # Base: Crisp pure white [255, 255, 255, 255] with border [228, 228, 231, 255]
    # Upright: Soft Pink [244, 114, 182, 255] with highlight [251, 113, 133, 255]
    logo_dark, cropped_dark = generate_logo_image(
        shape1, shape2, bbox,
        base_color=[255, 255, 255, 255],
        base_border=[228, 228, 231, 255],
        upright_color=[244, 114, 182, 255],
        upright_border=[251, 113, 133, 255]
    )
    logo_dark_path = os.path.join(drawable_dir, "ic_lokap_logo_dark.png")
    logo_dark.save(logo_dark_path, "PNG")
    print(f"Saved logo dark: {logo_dark_path}")

    # Also save as default ic_lokap_logo.png (used by launcher foreground)
    default_logo_path = os.path.join(drawable_dir, "ic_lokap_logo.png")
    logo_dark.save(default_logo_path, "PNG")
    print(f"Saved default logo: {default_logo_path}")

    # 2. Light theme logo (Black base card)
    # Base: Deep black [24, 24, 27, 255] with border [63, 63, 70, 255]
    # Upright: Soft Pink [244, 114, 182, 255] with crisp border [225, 29, 72, 255]
    logo_light, _ = generate_logo_image(
        shape1, shape2, bbox,
        base_color=[24, 24, 27, 255],
        base_border=[63, 63, 70, 255],
        upright_color=[244, 114, 182, 255],
        upright_border=[225, 29, 72, 255]
    )
    logo_light_path = os.path.join(drawable_dir, "ic_lokap_logo_light.png")
    logo_light.save(logo_light_path, "PNG")
    print(f"Saved logo light: {logo_light_path}")

    # 3. Generate Mipmaps for Launcher Icons with White base card
    w, h = cropped_dark.size
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    dark_bg = (18, 18, 20, 255) # #121214 Dark monochrome background

    for folder, size in densities.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)

        sym_size = int(size * 0.65)
        sym_scale = sym_size / max(w, h)
        snw, snh = int(w * sym_scale), int(h * sym_scale)
        scaled_sym = cropped_dark.resize((snw, snh), Image.Resampling.LANCZOS)

        # A. Square / Rounded launcher
        icon_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(icon_img)
        corner_r = int(size * 0.22)
        draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=corner_r, fill=dark_bg)
        ox = (size - snw) // 2
        oy = (size - snh) // 2
        icon_img.paste(scaled_sym, (ox, oy), scaled_sym)
        icon_path = os.path.join(folder_path, "ic_launcher.png")
        icon_img.save(icon_path, "PNG")

        # B. Round launcher
        round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw_round = ImageDraw.Draw(round_img)
        draw_round.ellipse([0, 0, size - 1, size - 1], fill=dark_bg)
        round_img.paste(scaled_sym, (ox, oy), scaled_sym)
        round_path = os.path.join(folder_path, "ic_launcher_round.png")
        round_img.save(round_path, "PNG")

        print(f"Generated launcher icons for {folder}")

if __name__ == "__main__":
    main()
