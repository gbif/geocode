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

import javax.management.MBeanServer;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.apache.batik.transcoder.image.ImageTranscoder.KEY_BACKGROUND_COLOR;

/**
 * Generate large bitmap images from geocoder layer data, to use as a local cache when querying that layer.
 */
public class BitmapGenerator {
  private static final String APP_CONF_FILE = "geocode.properties";

  // It does not seem worth using a templating engine for a single "template".  Welcome to 1995!
  private static final String SVG_HEADER =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN' 'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>\n" +
      // crispEdges removes anti-aliasing on the default rendering.
      "<svg shape-rendering=\"crispEdges\" fill=\"black\" fill-opacity=\"1\" stroke=\"none\" " +
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
      "      fill: #000000;\n" +
      "    }\n" +
      "  </style>\n";

  private static final String PATH_FORMAT = "  <path id='%s' style='fill: %s' d='%s'/>\n";

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
    bitmapGenerator.generateAllBitmaps(targetDirectory);
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
        .put("iho", tileMapper::svgIho)
        .put("seavox", tileMapper::svgSeaVoX)
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
    System.out.println("Generating bitmap for "+layerName);
    Stopwatch sw = Stopwatch.createStarted();
    Path svgFile = Files.createTempFile(layerName, ".svg");
    Path pngFile = targetDirectory.resolve(layerName + ".png");

    // TODO: Pixels need to get bigger towards the poles.

    // Read SVG paths from the database, and write them into an SVG file.
    System.out.println("→ Generating SVG for "+layerName+" as "+svgFile.toString());
    try (Writer svgOut = new OutputStreamWriter(new FileOutputStream(svgFile.toFile()), StandardCharsets.UTF_8)) {
      svgOut.write(SVG_HEADER);
      List<SvgShape> shapes = shapeSupplier.get();

      Stack<String> colours = MakeColours.makeColours(shapes.size());

      for (SvgShape shape : shapes) {
        svgOut.write(String.format(PATH_FORMAT, shape.getId(), colours.pop(), shape.getShape()));
      }

      svgOut.write(SVG_FOOTER);
    }

    // Convert the SVG file to PNG.
    System.out.println("→ Converting SVG to PNG for "+layerName+" as "+pngFile.toString());
    try (OutputStream pngOut = new FileOutputStream(pngFile.toFile())) {
      TranscoderInput svgImage = new TranscoderInput(svgFile.toString());
      TranscoderOutput pngImage = new TranscoderOutput(pngOut);
      PNGTranscoder pngTranscoder = new PNGTranscoder();
      pngTranscoder.addTranscodingHint(KEY_BACKGROUND_COLOR, Color.white);
      pngTranscoder.transcode(svgImage, pngImage);
    }

    System.out.println("Layer "+layerName+" completed in "+sw.elapsed(TimeUnit.SECONDS)+"s");
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
