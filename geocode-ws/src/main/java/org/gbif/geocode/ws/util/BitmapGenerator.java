package org.gbif.geocode.ws.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.gbif.geocode.ws.model.SvgShape;
import org.gbif.geocode.ws.model.TileMapper;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import javax.imageio.ImageIO;
import javax.management.MBeanServer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.apache.batik.transcoder.image.ImageTranscoder.KEY_BACKGROUND_COLOR;

/**
 * Generate large bitmap images from geocoder layer data, to use as a local cache when querying that layer.
 *
 * See documentation file MapImageLookup.adoc for background.
 */
public class BitmapGenerator {
  private static final String APP_CONF_FILE = "geocode.properties";

  // It does not seem worth using a templating engine for a single "template".  Welcome to 1995!
  private static final String SVG_HEADER =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' 'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>\n" +
      // geometricPrecision has anti-aliasing, but crispEdges misses out tiny shapes.
      "<svg shape-rendering=\"geometricPrecision\" " +
      // This gives us a 7200×3600 image, with the "units" (pixels) on a scale -180°–180°, -90°–90°.
      // Therefore, 1px = 1°.
      "height=\"3600\" width=\"7200\" viewBox=\"-180 -90 360 180\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
      "\n" +
      "  <style type=\"text/css\">\n" +
      "    path {\n" +
      "      stroke: #000000;\n" +
      // 0.2° outlines, which seem reasonable after the simplification applied in the SQL.
      "      stroke-width: 0.20px;\n" +
      "      stroke-linecap: round;\n" +
      "      stroke-linejoin: round;\n" +
      "      fill: none;\n" +
      "    }\n" +
      "  </style>\n";

  private static final String HOLLOW_PATH_FORMAT = "  <path id='%s' d='%s'/>\n";

  private static final String FILLED_PATH_FORMAT = "  <path id='%s' style='fill: %s' d='%s'/>\n";

  private static final String SVG_FOOTER = "</svg>\n";

  private final SqlSessionFactory sqlSessionFactory;

  /**
   * Generate bitmaps for all known layers.
   */
  public static void main(String... args) throws Exception {
    Preconditions.checkNotNull(args[0], "Argument is target directory");
    Path targetDirectory = Paths.get(args[0]);
    targetDirectory.toFile().mkdirs();

    Properties p = PropertiesUtil.loadProperties(APP_CONF_FILE);
    MyBatisModule m = new InternalMyBatisModule(p);
    Injector injector = Guice.createInjector(m);

    BitmapGenerator bitmapGenerator = new BitmapGenerator(injector.getInstance(SqlSessionFactory.class));
    bitmapGenerator.generateAllBitmaps(targetDirectory.resolve("layers"));

    bitmapGenerator.combineAllBitmaps(targetDirectory, "political", "eez", "gadm", "iho", "seavox", "wgsrpd", "geolocate_centroids");
  }

  /**
   * Generate bitmaps for each layer.
   */
  public void generateAllBitmaps(Path targetDirectory) throws Exception {

    try (SqlSession session = sqlSessionFactory.openSession()) {
      TileMapper tileMapper = session.getMapper(TileMapper.class);

      ImmutableMap<String, Supplier<List<SvgShape>>> svgSuppliers = new ImmutableMap.Builder<String, Supplier<List<SvgShape>>>()
        .put("political", tileMapper::svgPolitical)
        .put("eez", tileMapper::svgEez)
        .put("gadm", tileMapper::svgGadm)
        .put("gadm5", tileMapper::svgGadm5)
        .put("gadm4", tileMapper::svgGadm4)
        .put("gadm3", tileMapper::svgGadm3)
        .put("gadm2", tileMapper::svgGadm2)
        .put("gadm1", tileMapper::svgGadm1)
        .put("gadm0", tileMapper::svgGadm0)
        .put("iho", tileMapper::svgIho)
        .put("seavox", tileMapper::svgSeaVoX)
        .put("wgsrpd", tileMapper::svgWgsrpd)
        .put("geolocate_centroids", tileMapper::svgGeolocateCentroids)
        .build();

      for (Map.Entry<String, Supplier<List<SvgShape>>> x : svgSuppliers.entrySet()) {
        generateBitmap(x.getValue(), targetDirectory, x.getKey());
      }

      // This probably requires too much RAM, but is useful in development.
      //svgSuppliers.entrySet().parallelStream().forEach(
      //  (x) -> {
      //    try {
      //      generateBitmap(x.getValue(), targetDirectory, x.getKey());
      //    } catch (Exception e) {
      //      throw new RuntimeException(e);
      //    }
      //  }
      //);
    }
  }

  /**
   * Generates the bitmaps for each layer.
   */
  public void generateBitmap(Supplier<List<SvgShape>> shapeSupplier, Path targetDirectory, String layerName)
    throws Exception
  {
    Path pngFile = targetDirectory.resolve(layerName + ".png");
    if (pngFile.toFile().exists()) {
      System.err.println("Won't overwrite "+pngFile+", remove it first if you want to regenerate it (slow).");
      return;
    }
    Path filledPngFile = Files.createTempFile(layerName, "-filled.png");
    Path hollowPngFile = Files.createTempFile(layerName, "-hollow.png");

    System.out.println("Generating bitmap for "+layerName);
    Stopwatch sw = Stopwatch.createStarted();
    Path filledSvgFile = Files.createTempFile(layerName, "-filled.svg");
    Path hollowSvgFile = Files.createTempFile(layerName, "-hollow.svg");

    // Read SVG paths from the database, and write them into an SVG file.
    System.out.println("→ Generating SVG for "+layerName+" as "+filledSvgFile+" and "+hollowSvgFile);
    try (
      Writer filledSvg = new OutputStreamWriter(new FileOutputStream(filledSvgFile.toFile()), StandardCharsets.UTF_8);
      Writer hollowSvg = new OutputStreamWriter(new FileOutputStream(hollowSvgFile.toFile()), StandardCharsets.UTF_8);
    ) {
      filledSvg.write(SVG_HEADER);
      hollowSvg.write(SVG_HEADER);
      List<SvgShape> shapes = shapeSupplier.get();

      Stack<String> colours = MakeColours.makeColours(shapes.size());

      for (SvgShape shape : shapes) {
        filledSvg.write(String.format(FILLED_PATH_FORMAT, shape.getId(), colours.pop(), shape.getShape()));
        hollowSvg.write(String.format(HOLLOW_PATH_FORMAT, shape.getId(), shape.getShape()));
      }

      filledSvg.write(SVG_FOOTER);
      hollowSvg.write(SVG_FOOTER);
    }

    // Convert the SVG file to PNG.
    System.out.println("→ Converting SVG to PNG for "+layerName+" as "+hollowPngFile);
    try (OutputStream pngOut = new FileOutputStream(hollowPngFile.toFile())) {
      TranscoderInput svgImage = new TranscoderInput(hollowSvgFile.toString());
      TranscoderOutput pngImage = new TranscoderOutput(pngOut);
      PNGTranscoder pngTranscoder = new PNGTranscoder();
      pngTranscoder.addTranscodingHint(KEY_BACKGROUND_COLOR, Color.white);
      pngTranscoder.transcode(svgImage, pngImage);
    }

    System.out.println("→ Converting SVG to PNG for "+layerName+" as "+filledPngFile);
    try (OutputStream pngOut = new FileOutputStream(filledPngFile.toFile())) {
      TranscoderInput svgImage = new TranscoderInput(filledSvgFile.toString());
      TranscoderOutput pngImage = new TranscoderOutput(pngOut);
      PNGTranscoder pngTranscoder = new PNGTranscoder();
      pngTranscoder.addTranscodingHint(KEY_BACKGROUND_COLOR, Color.white);
      pngTranscoder.transcode(svgImage, pngImage);
    }

    // TODO: Pixels need to get bigger towards the poles.

    System.out.println("→ Combining both PNGs for "+layerName+" as "+pngFile);
    {
      BufferedImage filled = ImageIO.read(filledPngFile.toFile());
      BufferedImage hollow = ImageIO.read(hollowPngFile.toFile());

      int height = filled.getHeight();
      int width = filled.getWidth();

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if ((hollow.getRGB(x, y) | 0xFF000000) < 0xFFFFFFFF) {
            filled.setRGB(x, y, 0xFF000000);
          }
        }
      }

      ImageIO.write(filled, "png", pngFile.toFile());
    }

    System.out.println("Layer "+layerName+" completed in "+sw.elapsed(TimeUnit.SECONDS)+"s");
  }

  /**
   * Map string keys to equally-stepped positions in the colourspace (dumb implementation).
   */
  Map<String, Integer> colourKey = new HashMap<>();
  Set<Integer> usedColours = new HashSet<>();
  int lastColour = 0;
  int inc = 0;
  private synchronized int getColour(String key) {
    if (inc == 0) {
      usedColours.add(0x000000);
      usedColours.add(0xFFFFFF);
      // Parameter will need changing if the number of polygons increases significantly.
      // (The idea is to go through the FFFFFF colours ~three times, so nearby polygons aren't such close colours.)
      for (inc = 2400; inc < 20000; inc++) {
        if (0xFFFFFF % inc == 3) break;
      }
      System.out.println("Colour increment is "+inc);
    }

    if (key.equals("BLACK")) {
      return 0x000000;
    }

    if (!colourKey.containsKey(key)) {
      lastColour = (lastColour + inc) % 0xFFFFFF;
      System.out.println("Colour "+key+" is now "+String.format("#%06x", lastColour));
      assert !usedColours.contains(lastColour);
      colourKey.put(key, lastColour);
      usedColours.add(lastColour);
    }
    return colourKey.get(key);
  }

  /**
   * Combines the bitmaps for every layer into a single bitmap, for use as a client cache.
   */
  public void combineAllBitmaps(Path targetDirectory, String... layerNames) throws Exception {
    Path pngFile = targetDirectory.resolve("resource/cache-bitmap.png");
    if (pngFile.toFile().exists()) {
      System.err.println("Won't overwrite "+pngFile+", remove it first if you want to regenerate it (slow).");
      return;
    }

    System.out.println("Generating combined layer bitmap.");
    Stopwatch sw = Stopwatch.createStarted();

    int height = 3600;
    int width = 7200;
    BufferedImage combined = new BufferedImage(width, height, TYPE_INT_RGB);

    BufferedImage[] images = new BufferedImage[layerNames.length];
    for (int i = 0; i < layerNames.length; i++) {
      images[i] = ImageIO.read(new FileInputStream(targetDirectory.resolve("layers/" + layerNames[i] + ".png").toFile()));
      assert (height == combined.getHeight());
      assert (width == combined.getWidth());
    }

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        String key = "";
        for (int i = 0; i < images.length; i++) {
          int colour = images[i].getRGB(x, y) & 0x00FFFFFF;
          if (colour == 0x000000) {
            key = "BLACK";
            break;
          }
          key += (colour);
        }
        combined.setRGB(x, y, getColour(key));
      }
    }

    ImageIO.write(combined, "PNG", pngFile.toFile());

    System.out.println("Combined bitmap with "+usedColours.size()+" colours completed in "+sw.elapsed(TimeUnit.SECONDS)+"s");
  }

  /**
   * A Guice module for all things mybatis.
   */
  private static class InternalMyBatisModule extends MyBatisModule {

    public InternalMyBatisModule(Properties props) {
      super("geocode", props);
    }

    @Override
    protected void bindMappers() {
      addMapperClass(TileMapper.class);
    }

    @Override
    protected void bindTypeHandlers() {}

    @Override
    protected void bindManagers() {
      bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
    }
  }

  public BitmapGenerator(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }
}
