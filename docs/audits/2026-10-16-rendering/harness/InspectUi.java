import com.arbergashi.charts.api.*;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.domain.legend.*;
import com.arbergashi.charts.domain.render.AxisRole;
import com.arbergashi.charts.model.*;
import com.arbergashi.charts.platform.export.ChartExportService;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.legend.InteractiveLegendOverlay;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Bounded headless probes for the dated UI audit. Results describe observed
 * behavior; this diagnostic program is not a regression-test acceptance suite.
 */
public class InspectUi {
  private static Path outputDirectory;
  static DefaultChartModel model(String name) { var m = new DefaultChartModel(name); m.setXY(0,0); m.setXY(1,1); m.setXY(2,0); return m; }
  static ArberChartPanel chart() { var p = new ArberChartPanel(model("First series"),new LineRenderer()); p.setAnimationsEnabled(false); p.setSize(800,400); p.doLayout(); return p; }
  static BufferedImage paint(ArberChartPanel p) { var b=new BufferedImage(Math.max(1,p.getWidth()),Math.max(1,p.getHeight()),BufferedImage.TYPE_INT_ARGB); var g=b.createGraphics(); p.paint(g); g.dispose(); return b; }
  static Object field(Object o,String name) throws Exception { var f=o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o); }
  static long countOpaque(BufferedImage b) { long n=0; for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if((b.getRGB(x,y)>>>24)!=0)n++;return n; }
  static long differences(BufferedImage a,BufferedImage b) { long n=0;for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)if(a.getRGB(x,y)!=b.getRGB(x,y))n++;return n; }
  static void layout(Container c) { c.doLayout();for(Component child:c.getComponents())if(child instanceof Container cc)layout(cc); }
  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("Pass an output directory for generated PNGs");
    outputDirectory = Files.createDirectories(Path.of(args[0]));
    Locale.setDefault(Locale.US);ChartScale.setScaleFactor(1f);SwingUtilities.invokeAndWait(()->{try{run();}catch(Exception e){throw new RuntimeException(e);}});System.exit(0); }
  static void run() throws Exception {
    var p=chart();p.setLegend(false);p.setXAxisConfig(new AxisConfig().setMedicalScale(25));p.setYAxisConfig(new AxisConfig().setMedicalScale(10));paint(p);
    var c=p.getDebugContext();double ppm=p.getPhysicalPixelsPerMillimeter();System.out.printf(Locale.US,"MEDICAL pixelsPerMm=%.8f xPxPerUnit=%.8f expectedXPxPerUnit=%.8f yPxPerUnit=%.8f expectedYPxPerUnit=%.8f%n",ppm,c.getPlotBounds().width()/(c.getMaxX()-c.getMinX()),ppm*25,c.getPlotBounds().height()/(c.getMaxY()-c.getMinY()),ppm*10);
    for(var side:LegendDockSide.values()){var d=chart();d.setDockedLegend(side);layout(d);paint(d);System.out.println("DOCK "+side+" canvas="+d.getOverlayCanvasForTesting().getBounds()+" plot="+d.getDebugContext().getPlotBounds());}
    var d=chart();d.setDockedLegend(LegendDockSide.RIGHT);layout(d);paint(d);var legend=(InteractiveLegendOverlay)field(d,"interactiveLegendOverlay");System.out.println("DOCK_THEME before="+legend.isVisible());d.setTheme(ChartThemes.getLightTheme());System.out.println("DOCK_THEME after="+legend.isVisible());
    var e=new ArberChartPanel(model("Export"),new LineRenderer());e.setPreferredSize(new Dimension(500,400));var f=outputDirectory.resolve("preferred-export.png").toFile();ChartExportService.exportPng(e,f);var bi=ImageIO.read(f);System.out.println("EXPORT preferred="+e.getPreferredSize()+" actual="+e.getSize()+" image="+bi.getWidth()+"x"+bi.getHeight()+" nontransparentPixels="+countOpaque(bi));
    var axis=new AxisConfig();var ns=new NiceScale(0,0.004).setMaxTicks(6);double[] ticks=ns.getTicks();var labels=new ArrayList<String>();for(int i=0;i<ns.getTickCount();i++)labels.add(axis.getFormattedValue(ticks[i]));System.out.println("PRECISION ticks="+Arrays.toString(Arrays.copyOf(ticks,ns.getTickCount()))+" labels="+labels);
    var g=chart();g.setLegend(false);var all=paint(g);g.setXAxisConfig(new AxisConfig().setShowGrid(false));var xOff=paint(g);System.out.println("GRID xDisabledPixelDifference="+differences(all,xOff));
    var l=chart();paint(l);var ol=(InteractiveLegendOverlay)field(l,"interactiveLegendOverlay");var rows=(java.util.List<?>)field(ol,"rowHits");var row=rows.getFirst();Rectangle name=(Rectangle)field(row,"nameBounds");Point q=new Point(name.x+Math.max(1,name.width/2),name.y+name.height/2);System.out.println("LEGEND actions="+field(ol,"actions")+" deepestAtName="+SwingUtilities.getDeepestComponentAt(l,q.x,q.y).getClass().getSimpleName());boolean before=l.getLayerVisibilityModel().isVisible("line");ol.dispatchEvent(new MouseEvent(ol,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,q.x,q.y,1,false,MouseEvent.BUTTON1));System.out.println("LEGEND directClick before="+before+" after="+l.getLayerVisibilityModel().isVisible("line"));
    var frozen=chart();frozen.setLegend(false);frozen.setFreeze(true);paint(frozen);var oc=frozen.getOverlayCanvasForTesting();oc.dispatchEvent(new MouseEvent(oc,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,200,160,1,false,MouseEvent.BUTTON1));oc.dispatchEvent(new MouseEvent(oc,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,400,180,1,false,MouseEvent.BUTTON1));System.out.println("FREEZE label="+frozen.getFreezeMeasurementLabel().getText()+" parent="+frozen.getFreezeMeasurementLabel().getParent()+" bounds="+frozen.getFreezeMeasurementLabel().getBounds());
    var translated=chart();translated.setLegend(false);final int[] calls={0};translated.setCoordinateTransformer(new CoordinateTransformer(){public void mapToPixel(PlotContext ctx,double x,double y,double[]out){calls[0]++;out[0]=100;out[1]=100;}public void mapToData(PlotContext ctx,double x,double y,double[]out){calls[0]++;out[0]=100;out[1]=100;}});paint(translated);double[] out=new double[2];translated.getDebugContext().mapToPixel(1,1,out);System.out.println("TRANSFORM calls="+calls[0]+" map="+Arrays.toString(out));
    var inverted=chart();inverted.setLegend(false);inverted.setXAxisConfig(new AxisConfig().setInverted(true));paint(inverted);var ic=inverted.getOverlayCanvasForTesting();var pre=inverted.getDebugContext();double[] old=new double[2];pre.mapToData(130,100,old);ic.dispatchEvent(new MouseWheelEvent(ic,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,130,100,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,-1));var post=inverted.getDebugContext();double[] now=new double[2];post.mapToData(130,100,now);System.out.printf(Locale.US,"INVERTED_ZOOM focusBefore=%.9f focusAfter=%.9f%n",old[0],now[0]);
    var loc=chart();loc.setLocale(Locale.GERMANY);var xaxis=(AxisConfig)field(loc,"xAxisConfig");System.out.println("LOCALE panel="+loc.getLocale()+" axis="+xaxis.getLocale()+" label="+xaxis.getFormattedValue(1.5));
    var secondary=chart();secondary.setLegend(false);secondary.setYAxisConfig(new AxisConfig().setFixedRange(-1,1));secondary.setYAxisSecondaryConfig(new AxisConfig().setFixedRange(-1000,1000));var rec=new BaseRenderer("recording"){protected void drawData(ArberCanvas canvas,ChartModel model,PlotContext context){System.out.println("SECONDARY context="+context.getMinY()+".."+context.getMaxY());}};rec.setYAxisRole(AxisRole.SECONDARY);secondary.setLayer(model("Second series"),rec);paint(secondary);
    var save=chart();save.setLegend(false);save.setXAxisConfig(new AxisConfig().setFixedRange(.1,1.9));save.setYAxisConfig(new AxisConfig().setFixedRange(.1,1.9));ImageIO.write(paint(save),"png",outputDirectory.resolve("axis-bounds.png").toFile());var sc=save.getDebugContext();var sn=new NiceScale(sc.getMinX(),sc.getMaxX()).setMaxTicks(6);var st=sn.getTicks();for(int i=0;i<sn.getTickCount();i++){sc.mapToPixel(st[i],sc.getMinY(),out);if(out[0]<sc.getPlotBounds().x()||out[0]>sc.getPlotBounds().maxX())System.out.printf(Locale.US,"OUT_OF_RANGE_TICK value=%.3f pixel=%.3f plot=%.3f..%.3f%n",st[i],out[0],sc.getPlotBounds().x(),sc.getPlotBounds().maxX());}
  }
}
