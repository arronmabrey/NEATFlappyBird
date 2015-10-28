package fr.neatmonster.labs;

import static fr.neatmonster.labs.neat.Pool.INPUTS;
import static fr.neatmonster.labs.neat.Pool.OUTPUTS;
import static fr.neatmonster.labs.neat.Pool.POPULATION;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import fr.neatmonster.labs.neat.Genome;
import fr.neatmonster.labs.neat.Neuron;
import fr.neatmonster.labs.neat.Pool;
import fr.neatmonster.labs.neat.Species;
import fr.neatmonster.labs.neat.Synapse;

@SuppressWarnings("serial")
public class NEATFlappyBird extends JPanel implements Runnable {
    private static class Bird {
        private static Map<Species, BufferedImage[]> cache = new WeakHashMap<Species, BufferedImage[]>();

        private static BufferedImage colorBird(final BufferedImage refImage,
                final Color color) {
            final BufferedImage image = new BufferedImage(BIRD_WIDTH,
                    BIRD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            final Color bright = color.brighter().brighter();
            final Color dark = color.darker().darker();
            for (int y = 0; y < BIRD_HEIGHT; ++y)
                for (int x = 0; x < BIRD_WIDTH; ++x) {
                    int argb = refImage.getRGB(x, y);
                    if (argb == 0xffe0802c)
                        argb = dark.getRGB();
                    else if (argb == 0xfffad78c)
                        argb = bright.getRGB();
                    else if (argb == 0xfff8b733)
                        argb = color.getRGB();
                    image.setRGB(x, y, argb);
                }
            return image;
        }

        private BufferedImage[] images;

        private final Genome genome;
        private double       height;
        private double       velocity;
        private double       angle;
        private boolean      flap;
        private int          flaps;
        private boolean      dead;

        private Bird(final Species species, final Genome genome) {
            if (cache.containsKey(species))
                images = cache.get(species);
            else {
                final Color color = new Color(rnd.nextInt(0x1000000));
                images = new BufferedImage[3];
                for (int i = 0; i < 3; ++i)
                    images[i] = colorBird(BIRD_IMAGES[i], color);
                cache.put(species, images);
            }

            this.genome = genome;
            height = HEIGHT / 2.0;
        }
    }

    private static class Cell {
        private int          x;
        private int          y;
        private final double value;

        public Cell(final int x, final int y, final double value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }
    }

    private static class Tube {
        private final double height;
        private double       position;
        private boolean      passed;

        private Tube(final int height) {
            this.height = height;
            position = WIDTH;
            passed = false;
        }
    }

    public static final Random rnd = new Random();

    private static final int WIDTH         = 576;
    private static final int HEIGHT        = 768;
    private static final int BIRD_WIDTH    = 72;
    private static final int BIRD_HEIGHT   = 52;
    private static final int FLOOR_WIDTH   = 672;
    private static final int FLOOR_HEIGHT  = 224;
    private static final int FLOOR_OFFSET  = 96;
    private static final int FLOOR_SPEED   = 5;
    private static final int TUBE_WIDTH    = 104;
    private static final int TUBE_HEIGHT   = 640;
    private static final int TUBE_APERTURE = 200;

    private static BufferedImage   BACK_IMAGE;
    private static BufferedImage[] BIRD_IMAGES;
    private static BufferedImage   GROUND_IMAGE;
    private static BufferedImage   TUBE1_IMAGE;
    private static BufferedImage   TUBE2_IMAGE;
    private static Font            FONT;

    private static final int[]   XS     = new int[] { 2, 6, 14, 18, 26, 50, 54,
            58, 62, 66, 70, 70, 66, 62, 42, 22, 14, 10, 6, 2 };
    private static final int[]   YS     = new int[] { -34, -38, -42, -46, -50,
            -50, -46, -42, -38, -26, -22, -18, -10, -6, -2, -2, -6, -10, -18,
            -22 };
    private static final Polygon BOUNDS = new Polygon(XS, YS, XS.length);

    static {
        try {
            BACK_IMAGE = upscale(ImageIO.read(new File("bg.png")));
            GROUND_IMAGE = upscale(ImageIO.read(new File("ground.png")));
            final BufferedImage birdImage = ImageIO.read(new File("bird.png"));
            BIRD_IMAGES = new BufferedImage[] {
                    upscale(birdImage.getSubimage(0, 0, 36, 26)),
                    upscale(birdImage.getSubimage(36, 0, 36, 26)),
                    upscale(birdImage.getSubimage(72, 0, 36, 26)) };
            TUBE1_IMAGE = upscale(ImageIO.read(new File("tube1.png")));
            TUBE2_IMAGE = upscale(ImageIO.read(new File("tube2.png")));
            FONT = Font.createFont(Font.TRUETYPE_FONT,
                    new File("04B_19__.TTF"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static Dimension getBounds(final Graphics2D g, final Font font,
            final String text) {
        final int width = (int) font
                .getStringBounds(text, g.getFontRenderContext()).getWidth();
        final int height = (int) font
                .createGlyphVector(g.getFontRenderContext(), text)
                .getVisualBounds().getHeight();
        return new Dimension(width, height);
    }

    public static void main(final String[] args) {
        final JFrame frame = new JFrame();
        frame.setResizable(false);
        frame.setTitle("NEATFlappyBird");
        frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final NEATFlappyBird neat = new NEATFlappyBird();
        frame.add(neat);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        neat.run();
    }

    private static BufferedImage toBufferedImage(final Image image) {
        final BufferedImage buffered = new BufferedImage(image.getWidth(null),
                image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        buffered.getGraphics().drawImage(image, 0, 0, null);
        return buffered;
    }

    private static BufferedImage upscale(final Image image) {
        return toBufferedImage(image.getScaledInstance(image.getWidth(null) * 2,
                image.getHeight(null) * 2, Image.SCALE_FAST));
    }

    private int speed;
    private int ticks;
    private int ticksTubes;

    private final List<Bird> birds = new ArrayList<Bird>();
    private final List<Tube> tubes = new ArrayList<Tube>();

    private Bird best;
    private int  score;

    public void eval() {
        Tube nextTube = null;
        for (final Tube tube : tubes)
            if (tube.position + TUBE_WIDTH > WIDTH / 3 - BIRD_WIDTH / 2
                    && (nextTube == null || tube.position < nextTube.position))
                nextTube = tube;
        for (final Bird bird : birds) {
            if (bird.dead)
                continue;

            final double[] input = new double[4];
            input[0] = bird.height / HEIGHT;
            if (nextTube == null) {
                input[1] = 0.5;
                input[2] = 1.0;
            } else {
                input[1] = nextTube.height / HEIGHT;
                input[2] = nextTube.position / WIDTH;
            }
            input[3] = 1.0;

            final double[] output = bird.genome.evaluateNetwork(input);
            if (output[0] > 0.5)
                bird.flap = true;
        }
    }

    public void initializeGame() {
        speed = 75;
        ticks = 0;
        ticksTubes = 0;

        best = null;
        score = 0;

        birds.clear();
        for (final Species species : Pool.species)
            for (final Genome genome : species.genomes) {
                genome.generateNetwork();
                birds.add(new Bird(species, genome));
            }
        tubes.clear();
    }

    public void learn() {
        best = birds.get(0);
        boolean allDead = true;
        for (final Bird bird : birds) {
            if (bird.dead)
                continue;
            allDead = false;

            double fitness = ticks - bird.flaps * 1.5;
            fitness = fitness == 0.0 ? -1.0 : fitness;

            bird.genome.fitness = fitness;
            if (fitness > Pool.maxFitness)
                Pool.maxFitness = fitness;

            if (fitness > best.genome.fitness)
                best = bird;
        }

        if (allDead) {
            Pool.newGeneration();
            initializeGame();
        }
    }

    @Override
    public void paint(final Graphics g_) {
        final Graphics2D g2d = (Graphics2D) g_;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(BACK_IMAGE, 0, 0, WIDTH, HEIGHT, null);

        for (final Tube tube : tubes) {
            g2d.drawImage(TUBE1_IMAGE, (int) tube.position,
                    HEIGHT - (int) tube.height - TUBE_APERTURE - TUBE_HEIGHT,
                    TUBE_WIDTH, TUBE_HEIGHT, null);
            g2d.drawImage(TUBE2_IMAGE, (int) tube.position,
                    HEIGHT - (int) tube.height, TUBE_WIDTH, TUBE_HEIGHT, null);
        }

        g2d.drawImage(GROUND_IMAGE,
                -(FLOOR_SPEED * ticks % (WIDTH - FLOOR_WIDTH)),
                HEIGHT - FLOOR_OFFSET, FLOOR_WIDTH, FLOOR_HEIGHT, null);

        int alive = 0;
        final int anim = ticks / 3 % 3;
        for (final Bird bird : birds) {
            if (bird.dead)
                continue;
            ++alive;
            final AffineTransform at = new AffineTransform();
            at.translate(WIDTH / 3 - BIRD_HEIGHT / 3, HEIGHT - bird.height);
            at.rotate(-bird.angle / 180.0 * Math.PI, BIRD_WIDTH / 2,
                    BIRD_HEIGHT / 2);
            g2d.drawImage(bird.images[anim], at, null);
        }

        final Font scoreFont = FONT.deriveFont(50f);
        g2d.setFont(scoreFont);
        final String scoreText = Integer.toString(score);
        final GlyphVector glyphsVector = scoreFont
                .createGlyphVector(g2d.getFontRenderContext(), scoreText);
        final Rectangle2D scoreBounds = glyphsVector.getVisualBounds();
        final int scoreX = WIDTH / 2 - (int) scoreBounds.getWidth() / 2;
        final int scoreY = HEIGHT / 6 + (int) scoreBounds.getHeight() / 2;
        final Shape outline = glyphsVector.getOutline(scoreX, scoreY);
        g2d.setStroke(new BasicStroke(8f));
        g2d.setColor(Color.BLACK);
        g2d.draw(outline);
        g2d.setColor(Color.WHITE);
        g2d.drawString(scoreText, scoreX, scoreY);
        g2d.setStroke(new BasicStroke(1f));

        g2d.setColor(new Color(0x80ffffff, true));
        g2d.fillRoundRect(10, 10, WIDTH - 20, 185, 6, 6);

        final int minX = 30;
        final int maxX = WIDTH - 42;

        final Map<Integer, Cell> graph = new HashMap<Integer, Cell>();
        for (final Entry<Integer, Neuron> entry : best.genome.network
                .entrySet()) {
            final int i = entry.getKey();
            final Neuron neuron = entry.getValue();
            final int x;
            final int y;
            if (i < Pool.INPUTS) {
                x = 15;
                y = 15 + 47 * i;
            } else if (entry.getKey() < INPUTS + OUTPUTS) {
                x = WIDTH - 47;
                y = 80;
                int opacity = 0x80000000;
                if (neuron.value < 0.5)
                    opacity = 0x30000000;
                g2d.setColor(new Color(opacity, true));
                g2d.setFont(FONT.deriveFont(9f));
                g2d.drawString("FLAP", 541, 88);
            } else {
                x = (minX + maxX) / 2;
                y = 80;
            }
            graph.put(i, new Cell(x, y, neuron.value));
        }

        for (int n = 0; n < 4; ++n)
            for (final Synapse gene : best.genome.genes)
                if (gene.enabled) {
                    final Cell c1 = graph.get(gene.input);
                    final Cell c2 = graph.get(gene.output);
                    if (gene.input >= INPUTS + OUTPUTS) {
                        c1.x = (int) (0.75 * c1.x + 0.25 * c2.x);
                        if (c1.x >= c2.x)
                            c1.x = c1.x - 60;
                        if (c1.x < minX)
                            c1.x = minX;
                        if (c1.x > maxX)
                            c1.x = maxX;
                        c1.y = (int) (0.75 * c1.y + 0.25 * c2.y);
                    }
                    if (gene.output >= INPUTS + OUTPUTS) {
                        c2.x = (int) (0.25 * c1.x + 0.75 * c2.x);
                        if (c1.x >= c2.x)
                            c2.x = c2.x + 60;
                        if (c2.x < minX)
                            c2.x = minX;
                        if (c2.x > maxX)
                            c2.x = maxX;
                        c2.y = (int) (0.25 * c1.y + 0.75 * c2.y);
                    }
                }

        for (final Synapse gene : best.genome.genes)
            if (gene.enabled) {
                final Cell c1 = graph.get(gene.input);
                final Cell c2 = graph.get(gene.output);
                final float value = (float) Math
                        .abs(Neuron.sigmoid(gene.weight));
                final Color color;
                if (Neuron.sigmoid(gene.weight) > 0.0)
                    color = Color.getHSBColor(2f / 3f, 1f, value);
                else
                    color = Color.getHSBColor(0f, 1f, value);
                g2d.setColor(new Color(color.getRed(), color.getGreen(),
                        color.getBlue(), 0x80));
                g2d.drawLine(c1.x + 8, c1.y + 5, c2.x + 2, c2.y + 5);
            }

        for (final Cell cell : graph.values())
            paintCell(g2d, cell);

        g2d.setColor(new Color(0x80000000, true));
        g2d.setFont(FONT.deriveFont(14f));
        g2d.drawString("GENERATION " + Pool.generation, 15, 190);
        Dimension d = getBounds(g2d, g2d.getFont(),
                "ALIVE " + alive + "/" + POPULATION);
        g2d.drawString("ALIVE " + alive + "/" + POPULATION, (576 - d.width) / 2,
                190);
        d = getBounds(g2d, g2d.getFont(),
                "FITNESS " + best.genome.fitness + "/" + Pool.maxFitness);
        g2d.drawString("FITNESS " + best.genome.fitness + "/" + Pool.maxFitness,
                561 - d.width, 190);
    }

    public void paintCell(final Graphics2D g2d, final Cell cell) {
        final float value = (float) Math.abs(cell.value);
        final Color color;
        if (cell.value > 0.0)
            color = Color.getHSBColor(2f / 3f, 1f, value);
        else
            color = Color.getHSBColor(0f, 1f, value);
        g2d.setColor(new Color(color.getRed(), color.getGreen(),
                color.getBlue(), 0x80));
        g2d.fillRect(cell.x, cell.y, 10, 10);
        g2d.setColor(g2d.getColor().darker().darker());
        g2d.drawRect(cell.x, cell.y, 10, 10);
    }

    @Override
    public void run() {
        Pool.initializePool();

        initializeGame();
        while (true) {
            eval();
            update();
            learn();

            repaint();
            try {
                Thread.sleep(25L);
            } catch (final InterruptedException e) {
            }
        }
    }

    public void update() {
        ++ticks;
        ++ticksTubes;

        if (ticksTubes == speed) {
            final int height = FLOOR_OFFSET + 100
                    + rnd.nextInt(HEIGHT - 200 - TUBE_APERTURE - FLOOR_OFFSET);
            tubes.add(new Tube(height));
            ticksTubes = 0;
        }

        final Iterator<Tube> it = tubes.iterator();
        while (it.hasNext()) {
            final Tube tube = it.next();
            tube.position -= FLOOR_SPEED;
            if (tube.position + TUBE_WIDTH < 0.0)
                it.remove();
            if (!tube.passed && tube.position + TUBE_WIDTH < WIDTH / 3
                    - BIRD_WIDTH / 2) {
                ++score;
                if (score % 10 == 0) {
                    speed -= 5;
                    speed = Math.max(speed, 20);
                }
                tube.passed = true;
            }
        }

        for (final Bird bird : birds) {
            if (bird.dead)
                continue;

            if (bird.flap) {
                bird.velocity = 10;
                bird.flap = false;
                ++bird.flaps;
            }

            bird.height += bird.velocity;
            bird.velocity -= 0.98;
            bird.angle = 3.0 * bird.velocity;
            bird.angle = Math.max(-90.0, Math.min(90.0, bird.angle));

            if (bird.height > HEIGHT) {
                bird.height = HEIGHT;
                bird.velocity = 0.0;
                bird.angle = -bird.angle;
            }

            if (bird.height < FLOOR_OFFSET + BIRD_HEIGHT / 2)
                bird.dead = true;

            final AffineTransform at = new AffineTransform();
            at.translate(WIDTH / 3 - BIRD_HEIGHT / 2, HEIGHT - bird.height);
            at.rotate(-bird.angle / 180.0 * Math.PI, BIRD_WIDTH / 2,
                    BIRD_HEIGHT / 2);
            at.translate(0, 52);
            final Shape bounds = new GeneralPath(BOUNDS)
                    .createTransformedShape(at);
            for (final Tube tube : tubes) {
                final Rectangle2D ceilTube = new Rectangle2D.Double(
                        tube.position,
                        HEIGHT - tube.height - TUBE_APERTURE - TUBE_HEIGHT,
                        TUBE_WIDTH, TUBE_HEIGHT);
                final Rectangle2D floorTube = new Rectangle2D.Double(
                        tube.position, HEIGHT - tube.height, TUBE_WIDTH,
                        TUBE_HEIGHT);
                if (bounds.intersects(ceilTube)
                        || bounds.intersects(floorTube)) {
                    bird.dead = true;
                    break;
                }
            }
        }
    }
}
