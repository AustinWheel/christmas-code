package dev.austin.christmas.scene;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Rotating solid geometry rasterized into an upright, fixed-resolution ASCII grid. */
final class AsciiTree {
    private record GlyphKey(char character, int rgb, boolean glowing) {}
    private final Map<GlyphKey, BufferedImage> glyphs = new HashMap<>();
    private int cachedSize;
    private static final String FONT_FAMILY = chooseFont();
    private static final ConvolveOp GLOW = createGlow();

    private static String chooseFont() {
        var families = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : new String[]{"Menlo", "Consolas", "DejaVu Sans Mono", "Courier New"})
            if (families.contains(name)) return name;
        return Font.MONOSPACED;
    }

    private static ConvolveOp createGlow() {
        float[] kernel = new float[49];
        float total = 0;
        for (int y = -3; y <= 3; y++) for (int x = -3; x <= 3; x++) {
            float value = (float) Math.exp(-(x * x + y * y) / 4.5);
            kernel[(y + 3) * 7 + x + 3] = value;
            total += value;
        }
        for (int i = 0; i < kernel.length; i++) kernel[i] /= total;
        return new ConvolveOp(new Kernel(7, 7, kernel), ConvolveOp.EDGE_ZERO_FILL, null);
    }

    // Rasterize and soften each glyph once, rather than blurring the whole scene every frame.
    private void drawGlyph(Graphics2D g, char character, double x, double baseline, boolean glowing) {
        GlyphKey key = new GlyphKey(character, g.getColor().getRGB(), glowing);
        BufferedImage sprite = glyphs.computeIfAbsent(key, k -> {
            int size = cachedSize * 2 + 12;
            BufferedImage core = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ink = core.createGraphics();
            ink.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ink.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            ink.setFont(new Font(FONT_FAMILY, Font.PLAIN, cachedSize));
            ink.setColor(new Color(k.rgb, true));
            String text = String.valueOf(k.character);
            float glyphWidth = (float) ink.getFontMetrics().getStringBounds(text, ink).getWidth();
            ink.drawString(text, (size - glyphWidth) / 2, cachedSize + 6);
            ink.dispose();
            if (!k.glowing) return core;
            BufferedImage soft = GLOW.filter(core, null);
            Graphics2D finish = soft.createGraphics();
            finish.setComposite(AlphaComposite.SrcOver.derive(0.88f));
            finish.drawImage(core, 0, 0, null);
            finish.dispose();
            return soft;
        });
        g.drawImage(sprite, AffineTransform.getTranslateInstance(x - sprite.getWidth() / 2.0,
                baseline - cachedSize - 6), null);
    }
    private record Vertex(double x, double y, double z, double nx, double ny, double nz, double u, double v) {}
    private record Face(Vertex a, Vertex b, Vertex c, int material) {}
    private record Screen(double x, double y, double depth, double light, double u, double v) {}
    private final java.util.List<Face> mesh = new java.util.ArrayList<>();
    private final java.util.List<Face> starMesh = new java.util.ArrayList<>();
    private static final String SHADOW_GLYPHS = "..--::^^^v";
    private static final String MID_GLYPHS = "^^-vxvxwWoo+";
    private static final String LIGHT_GLYPHS = "W%&#@Oo*";
    private static final Color[] GREENS = {new Color(16,64,43), new Color(35,133,85),
            new Color(79,193,104), new Color(94,202,105), new Color(106,203,115)};
    private static final Color[] GOLDS = {new Color(115,76,25),new Color(194,143,51),
            new Color(239,204,94),new Color(255,232,150),new Color(255,252,221)};
    private static final Color[] COLORS = {new Color(70, 183, 98), new Color(255, 231, 128),
            new Color(255, 83, 91), new Color(72, 116, 246), new Color(158, 102, 56)};
    private static final Color[] ORNAMENTS = {new Color(246,55,60),new Color(225,48,70),
            new Color(255,88,55),new Color(250,119,57),new Color(255,153,64),new Color(240,72,42)};
    private double[] depth, bloom;
    private int[] rgb, materials, bloomColor;
    private char[] characters;
    private int columns, rows, cellWidth, cellHeight, frameWidth = -1, frameHeight;
    private double frameTime = Double.NaN;
    private Area silhouette = new Area();

    AsciiTree() {
        // Overlapping flared branch skirts, with gently scalloped edges.
        for (int tier = 0; tier < 6; tier++) {
            double bottom = 0.12 + tier * 0.265;
            double radius = 0.82 * (1 - tier / 6.4);
            for (int ring = 0; ring < 10; ring++) for (int sector = 0; sector < 96; sector++) {
                double a = sector * Math.PI * 2 / 96, b = (sector + 1) * Math.PI * 2 / 96;
                double v = ring / 10.0, w = (ring + 1) / 10.0;
                quad(branch(bottom, radius, a, v), branch(bottom, radius, b, v),
                        branch(bottom, radius, b, w), branch(bottom, radius, a, w), 0);
            }
        }
        box(0, -0.06, 0, .12, .25, .12, 4);
        // A real helical tube: the depth buffer hides the rear half of each turn.
        for (int i = 0; i < 400; i++) {
            double t = i / 400.0, next = (i + 1) / 400.0;
            double y = .19 + t * 1.55, yn = .19 + next * 1.55;
            double angle = t * Math.PI * 2 * 4.4;
            double an = next * Math.PI * 2 * 4.4;
            for (int k = 0; k < 6; k++) {
                double p = k * Math.PI / 3, q = (k + 1) * Math.PI / 3;
                quad(garland(y, angle, p), garland(yn, an, p), garland(yn, an, q), garland(y, angle, q), 1);
            }
        }
        for (int i = 0; i < 54; i++) {
            double y = .24 + (i % 12) * .125;
            double angle = i * 2.39996;
            double size = .042 + (i % 3) * .006;
            double radius = treeRadius(y, angle) + size * .60;
            int material = 40 + i % ORNAMENTS.length;
            sphere(Math.cos(angle) * radius, y, Math.sin(angle) * radius, size, material);
        }
        for (int i = 0; i < 5; i++) {
            double angle = i * Math.PI * 2 / 5 + .3;
            double x = Math.cos(angle), z = Math.sin(angle);
            double w = .29 + i % 2 * .035, h = .25 + i % 3 * .025, d = .27;
            int color = i % 2 == 0 ? 3 : 2;
            box(x, -.02, z, w, h, d, color);
            box(x, h-.02, z, w+.025, .045, d+.025, color);
        }
        star();
    }

    private static Vertex branch(double bottom, double radius, double angle, double v) {
        double scallop = 1 + .028 * Math.sin(angle * 9 + bottom * 5) * (1 - v);
        double r = radius * Math.pow(1 - v, 0.85) * scallop;
        double y = bottom + v * .47 + .012 * Math.cos(angle * 9) * (1 - v);
        double slope = radius / .47;
        double length = Math.sqrt(1 + slope * slope);
        return new Vertex(r * Math.cos(angle), y, r * Math.sin(angle),
                Math.cos(angle) / length, slope / length, Math.sin(angle) / length, angle, y);
    }

    private static double treeRadius(double y, double angle) {
        double radius = 0;
        for (int tier = 0; tier < 6; tier++) {
            double bottom = .12 + tier * .265;
            double v = (y - bottom) / .47;
            if (v >= 0 && v <= 1) radius = Math.max(radius,
                    .82 * (1 - tier / 6.4) * Math.pow(1 - v, .85)
                            * (1 + .028 * Math.sin(angle * 9 + bottom * 5) * (1 - v)));
        }
        return radius;
    }

    private static Vertex garland(double y, double angle, double cross) {
        double envelope = Math.max(treeRadius(y, angle), Math.max(treeRadius(y-.04, angle), treeRadius(y+.04, angle)));
        double r = envelope + .055 + Math.cos(cross) * .040;
        return new Vertex(r * Math.cos(angle), y + Math.sin(cross) * .040, r * Math.sin(angle),
                Math.cos(angle) * Math.cos(cross), Math.sin(cross), Math.sin(angle) * Math.cos(cross), angle, y);
    }

    private void sphere(double x, double y, double z, double radius, int material) {
        for (int lat = 0; lat < 8; lat++) for (int lon = 0; lon < 14; lon++) {
            double a = lat * Math.PI / 8, b = (lat + 1) * Math.PI / 8;
            double c = lon * Math.PI * 2 / 14, d = (lon + 1) * Math.PI * 2 / 14;
            quad(ball(x,y,z,radius,a,c), ball(x,y,z,radius,a,d), ball(x,y,z,radius,b,d), ball(x,y,z,radius,b,c), material);
        }
    }

    private static Vertex ball(double x, double y, double z, double radius, double a, double b) {
        double nx = Math.sin(a) * Math.cos(b), ny = Math.cos(a), nz = Math.sin(a) * Math.sin(b);
        return new Vertex(x + radius * nx, y + radius * ny, z + radius * nz, nx, ny, nz, b, a);
    }

    private void box(double x, double y, double z, double w, double h, double d, int material) {
        double[][] corners = {{x-w/2,y,z-d/2},{x+w/2,y,z-d/2},{x+w/2,y+h,z-d/2},{x-w/2,y+h,z-d/2},
                {x-w/2,y,z+d/2},{x+w/2,y,z+d/2},{x+w/2,y+h,z+d/2},{x-w/2,y+h,z+d/2}};
        int[][] sides = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
        double[][] normals = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        for (int i = 0; i < 6; i++) {
            Vertex[] v = new Vertex[4];
            for (int j = 0; j < 4; j++) {
                double[] p = corners[sides[i][j]], n = normals[i];
                v[j] = new Vertex(p[0],p[1],p[2],n[0],n[1],n[2],j == 1 || j == 2 ? 1 : 0,j >= 2 ? 1 : 0);
            }
            // Gift materials have ribbon UVs; trunk uses the ordinary brown material.
            quad(v[0],v[1],v[2],v[3],material == 4 ? 4 : material + 5);
        }
    }

    private void star() {
        Vertex[] outline = new Vertex[10];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? .14 : .064;
            double x = Math.cos(angle) * r, y = 2.005 + Math.sin(angle) * r;
            outline[i] = new Vertex(x,y,.035,0,0,1,x+1,y);
        }
        Vertex center = new Vertex(0,2.005,.035,0,0,1,1,2.005);
        for (int i = 0; i < 10; i++)
            starMesh.add(new Face(center,outline[i],outline[(i+1)%10],1));
    }

    // A camera-facing star: no tree rotation or camera foreshortening of its shape.
    private Screen projectStar(Vertex v, double scale, double cx, double base) {
        return new Screen((cx+v.x*scale)/cellWidth,
                (base-(2.005*.985+v.y-2.005)*scale)/cellHeight,
                2.005*.174+.035,1,v.u,v.v);
    }

    private void quad(Vertex a, Vertex b, Vertex c, Vertex d, int material) {
        mesh.add(new Face(a,b,c,material)); mesh.add(new Face(a,c,d,material));
    }

    /** The grid is fixed in screen space; geometry, lighting and depth change inside it. */
    private void prepare(int width, int height, double time) {
        if (width == frameWidth && height == frameHeight && time == frameTime) return;
        frameWidth = width; frameHeight = height; frameTime = time;
        double scale = Math.min(height * .38, width * .36);
        int font = Math.max(8, Math.min(17, (int) Math.round(scale / 19)));
        if (font != cachedSize) { glyphs.clear(); cachedSize = font; }
        cellWidth = Math.max(4, (int) (font * .66)); cellHeight = Math.max(6, (int) (font * .92));
        columns = Math.max(1, (width + cellWidth - 1) / cellWidth);
        rows = Math.max(1, (height + cellHeight - 1) / cellHeight);
        int count = columns * rows;
        if (depth == null || depth.length != count) {
            depth = new double[count]; bloom = new double[count]; rgb = new int[count]; materials = new int[count]; bloomColor = new int[count]; characters = new char[count];
        }
        Arrays.fill(depth, Double.NEGATIVE_INFINITY);
        Arrays.fill(bloom, 0);
        double angle = time * Math.PI * 2 / 32 + .15;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double cx = Math.min(width * .66, width - scale * 1.28);
        double base = height * .87;
        ground(scale, cx, base, cos, sin);
        for (Face face : mesh) raster(project(face.a,cos,sin,scale,cx,base),
                project(face.b,cos,sin,scale,cx,base), project(face.c,cos,sin,scale,cx,base), face.material);
        for (Face face : starMesh) raster(projectStar(face.a,scale,cx,base),
                projectStar(face.b,scale,cx,base),projectStar(face.c,scale,cx,base),face.material);
        pinLights(cos, sin, scale, cx, base);
        // Coalesce adjacent occupied cells into horizontal runs for snow clipping.
        java.awt.geom.Path2D mask = new java.awt.geom.Path2D.Double();
        for (int row = 0; row < rows; row++) {
            int start = -1;
            for (int col = 0; col <= columns; col++) {
                boolean filled = col < columns && Double.isFinite(depth[row * columns + col]);
                if (filled && start < 0) start = col;
                if (!filled && start >= 0) {
                    mask.append(new java.awt.geom.Rectangle2D.Double(start * cellWidth,row * cellHeight,
                            (col-start)*cellWidth,cellHeight),false); start = -1;
                }
            }
        }
        silhouette = new Area(mask);
    }

    /** Full-width snow bank; rotating objects draw in front of its texture. */
    private void ground(double scale, double cx, double base, double cos, double sin) {
        double centerY = base + scale * .055;
        for (int row = 0; row < rows; row++) for (int col = 0; col < columns; col++) {
            double x = (col + .5) * cellWidth, y = (row + .5) * cellHeight;
            double surface = SnowGround.surface(x, frameWidth, frameHeight);
            if (y < surface) continue;
            double distanceDown = (y - surface) / Math.max(1, frameHeight - surface);
            double u = (x - cx) / (scale * 1.18), v = (y - centerY) / (scale * .20);
            int hash = (col * 73856093) ^ (row * 19349663);
            hash ^= hash >>> 13;
            double grain = (hash & 1023) / 1023.0;
            double density = .88 - .55 * distanceDown;
            double shadow = .45 * Math.exp(-Math.pow(u / .19, 2) - Math.pow(v / .48, 2));
            for (int i = 0; i < 5; i++) {
                double angle = i * Math.PI * 2 / 5 + .3;
                double gx = Math.cos(angle), gz = Math.sin(angle);
                double sx = cx + (gx*cos + gz*sin) * scale;
                double sy = base + (.02 + (-gx*sin + gz*cos)*.174) * scale;
                double distance = Math.pow((x-sx)/(scale*.15),2) + Math.pow((y-sy)/(scale*.055),2);
                shadow += .35 * Math.exp(-distance);
            }
            double light = Math.max(.25, 1 - shadow) * (.90 - .40 * distanceDown);
            // Quantized colors keep the glow atlas bounded during the rotation.
            light = Math.round(light * 12) / 12.0;
            int index = row * columns + col;
            depth[index] = -100; // Also masks rear snow between mound characters.
            rgb[index] = new Color((int)(174*light),(int)(204*light),(int)(228*light)).getRGB();
            characters[index] = grain > density ? ' ' : distanceDown > .65 ? '.' : ".,:+".charAt((hash & 255) % 4);
            materials[index] = -1;
        }
    }

    private Screen project(Vertex v, double cos, double sin, double scale, double cx, double base) {
        double x = v.x * cos + v.z * sin, z = -v.x * sin + v.z * cos;
        double nx = v.nx * cos + v.nz * sin, nz = -v.nx * sin + v.nz * cos;
        double light = .45 + .55 * Math.max(0, nx * -.40 + v.ny * .65 + nz * .64);
        return new Screen((cx+x*scale)/cellWidth,(base-(v.y*.985-z*.174)*scale)/cellHeight,
                z*.985+v.y*.174,light,v.u,v.v);
    }

    /** Small surface-attached bulbs occupy exactly one visible foliage cell each. */
    private void pinLights(double cos, double sin, double scale, double cx, double base) {
        for (int i = 0; i < 12; i++) {
            double y = .32 + (i % 6) * .23;
            double angle = i * 2.39996 + .9;
            double radius = treeRadius(y, angle) + .025;
            Screen p = project(new Vertex(radius*Math.cos(angle), y, radius*Math.sin(angle),
                    0, 0, 1, 0, 0), cos, sin, scale, cx, base);
            int col = (int)Math.floor(p.x), row = (int)Math.floor(p.y);
            if (col < 0 || col >= columns || row < 0 || row >= rows) continue;
            int index = row*columns+col;
            // Never shine through the rear branches, ornaments, presents, or garlands.
            if (!Double.isFinite(depth[index]) || materials[index] != 0 || p.depth < depth[index]) continue;
            Color color = i % 2 == 0 ? new Color(255,226,156) : new Color(255,166,87);
            depth[index] = p.depth;
            characters[index] = '*';
            rgb[index] = color.getRGB();
            bloomColor[index] = color.getRGB();
            bloom[index] = 1;
            materials[index] = 1;
        }
    }

    private void raster(Screen a, Screen b, Screen c, int material) {
        double denominator = (b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y);
        if (Math.abs(denominator) < 1e-8) return;
        int left = Math.max(0,(int)Math.floor(Math.min(a.x,Math.min(b.x,c.x))));
        int right = Math.min(columns-1,(int)Math.ceil(Math.max(a.x,Math.max(b.x,c.x))));
        int top = Math.max(0,(int)Math.floor(Math.min(a.y,Math.min(b.y,c.y))));
        int bottom = Math.min(rows-1,(int)Math.ceil(Math.max(a.y,Math.max(b.y,c.y))));
        for (int row=top;row<=bottom;row++) for(int col=left;col<=right;col++) {
            double x=col+.5,y=row+.5;
            double wa=((b.y-c.y)*(x-c.x)+(c.x-b.x)*(y-c.y))/denominator;
            double wb=((c.y-a.y)*(x-c.x)+(a.x-c.x)*(y-c.y))/denominator;
            double wc=1-wa-wb;
            if(wa < -1e-7 || wb < -1e-7 || wc < -1e-7) continue;
            double z=wa*a.depth+wb*b.depth+wc*c.depth;
            int index=row*columns+col;
            if(z <= depth[index]) continue;
            depth[index]=z;
            double u=wa*a.u+wb*b.u+wc*c.u,v=wa*a.v+wb*b.v+wc*c.v;
            boolean ornament = material >= 20;
            boolean glowingOrnament = material >= 40;
            int ornamentIndex = ornament ? (material % 20) : 0;
            boolean gift = material == 7 || material == 8;
            int surface = ornament ? 2 : gift ? material-5 : material;
            if(gift && (Math.abs(u-.5)<.13 || Math.abs(v-.65)<.10)) surface=1;
            boolean star = material == 1 && v > 1.86;
            boolean garland = material == 1 && !star;
            double light=wa*a.light+wb*b.light+wc*c.light;
            if (surface == 0) {
                // Expand the directional contrast: left-facing branches catch the light,
                // while right-facing branches retain a dark emerald ambient tone.
                double directional = Math.max(0, Math.min(1, (light - .45) / .55));
                light = .22 + .56 * Math.pow(directional, 1.2);
            }
            // Surface-anchored bulb segments keep their color as the tree rotates.
            int bulb = textureHash((int)Math.floor(u*10), 17);
            boolean dimBulb = garland && bulb % 100 < 6;
            int warmth = textureHash((int)Math.floor(u*6), 29) % 10;
            if (garland || star) light = .90;
            if (dimBulb) light = .55;
            if (glowingOrnament) light = .82 + .18 * light;
            light = Math.round(Math.max(0,Math.min(1,light))*20)/20.0;
            bloom[index] = garland ? (dimBulb ? .24 : .65) : glowingOrnament ? .65 : star ? .40 : 0;
            bloomColor[index] = glowingOrnament ? ORNAMENTS[ornamentIndex].getRGB() : new Color(255,225,110).getRGB();
            Color color;
            if(surface==0) color=palette(GREENS,light);
            else if(garland) {
                Color bulbColor = warmth < 2 ? new Color(255,199,108)
                        : warmth < 5 ? new Color(255,215,130) : new Color(255,232,165);
                double brightness = dimBulb ? .62 : 1;
                color = new Color((int)(bulbColor.getRed()*brightness),
                        (int)(bulbColor.getGreen()*brightness),(int)(bulbColor.getBlue()*brightness));
                bloomColor[index] = bulbColor.getRGB();
            }
            else if(surface==1) color=palette(GOLDS,light);
            else {
                Color baseColor=ornament ? ORNAMENTS[ornamentIndex] : COLORS[surface];
                double exposure=.35+.65*light;
                int highlight=(int)(Math.max(0,light-.78)*125);
                color=new Color(Math.min(255,(int)(baseColor.getRed()*exposure)+highlight),
                        Math.min(255,(int)(baseColor.getGreen()*exposure)+highlight),
                        Math.min(255,(int)(baseColor.getBlue()*exposure)+highlight));
            }
            rgb[index]=color.getRGB();
            int texture=textureHash((int)Math.floor(u*27),(int)Math.floor(v*83));
            if (star) {
                int epoch = (int)Math.floor(frameTime * .8);
                if (texture % 5 == Math.floorMod(epoch,5)) texture = textureHash(texture,epoch);
            }
            String alphabet;
            if(star || garland) alphabet="#@%=*";
            else if(ornament) alphabet=glowingOrnament ? "O@*o" : "oO%&";
            else if(surface==0) {
                // Shade primarily with ink color, keeping full glyphs on the dark side too.
                // Only a few extra sparse marks follow the lighting (8–12% overall).
                int mix = textureHash(texture, 53) % 100;
                double sparsePercent = 8 + 4 * (1-light);
                alphabet = mix < sparsePercent ? SHADOW_GLYPHS : mix < 78 ? MID_GLYPHS : LIGHT_GLYPHS;
            } else if(surface==1) alphabet=light<.60 ? "--=+^" : light<.82 ? "+*=#@" : "#@%*";
            else if(surface==4) alphabet="H|:-";
            else alphabet=light<.58 ? "-:xv" : light<.80 ? "xXo%+" : "W#%&";
            characters[index]=alphabet.charAt(texture%alphabet.length());
            materials[index]=surface;
        }
    }

    private static int textureHash(int u, int v) {
        int hash=u*0x1f123bb5 ^ v*0x5f356495;
        hash^=hash>>>16; hash*=0x7feb352d; hash^=hash>>>15;
        return hash & 0x7fffffff;
    }

    private static Color palette(Color[] stops, double value) {
        double location=Math.max(0,Math.min(1,value))*(stops.length-1);
        int first=Math.min(stops.length-2,(int)location);
        double blend=location-first;
        Color a=stops[first],b=stops[first+1];
        return new Color((int)Math.round(a.getRed()+(b.getRed()-a.getRed())*blend),
                (int)Math.round(a.getGreen()+(b.getGreen()-a.getGreen())*blend),
                (int)Math.round(a.getBlue()+(b.getBlue()-a.getBlue())*blend));
    }

    Area occlusion(int width, int height, double time) {
        prepare(width,height,time);
        return new Area(silhouette);
    }

    private static final Map<Integer, BufferedImage> BLOOMS = new HashMap<>();

    private static BufferedImage createBloom(int rgb) {
        Color color = new Color(rgb);
        BufferedImage image = new BufferedImage(65,65,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(new RadialGradientPaint(32,32,32,new float[]{0,.4f,1},
                new Color[]{new Color(color.getRed(),color.getGreen(),color.getBlue(),160),
                        new Color(color.getRed(),color.getGreen(),color.getBlue(),60),
                        new Color(color.getRed(),color.getGreen(),color.getBlue(),0)}));
        g.fillRect(0,0,65,65);
        g.dispose();
        return image;
    }

    void paint(Graphics2D graphics,int width,int height,double time) {
        prepare(width,height,time);
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            Composite original = g.getComposite();
            float opacity = original instanceof AlphaComposite a ? a.getAlpha() : 1;
            int glowSize = cachedSize * 4;
            for(int row=0;row<rows;row++) for(int col=0;col<columns;col++) {
                int index = row*columns+col;
                if(bloom[index] <= .01 || !Double.isFinite(depth[index])) continue;
                g.setComposite(AlphaComposite.SrcOver.derive(opacity * (float)Math.min(.17,bloom[index]*.13)));
                g.drawImage(BLOOMS.computeIfAbsent(bloomColor[index], AsciiTree::createBloom),(int)((col+.5)*cellWidth-glowSize/2.0),
                        (int)((row+.5)*cellHeight-glowSize/2.0),glowSize,glowSize,null);
            }
            g.setComposite(original);
            for(int row=0;row<rows;row++) for(int col=0;col<columns;col++) {
                int index=row*columns+col;
                if(!Double.isFinite(depth[index])) continue;
                g.setColor(new Color(rgb[index],true));
                drawGlyph(g,characters[index],(col+.5)*cellWidth,(row+1)*cellHeight-1,bloom[index]>0);
            }
        } finally {g.dispose();}
    }
}
