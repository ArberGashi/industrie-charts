import com.arbergashi.charts.api.*;
import com.arbergashi.charts.api.types.*;
import com.arbergashi.charts.core.geometry.*;
import com.arbergashi.charts.core.rendering.*;
import com.arbergashi.charts.engine.spatial.*;
import com.arbergashi.charts.model.*;
import com.arbergashi.charts.render.*;
import com.arbergashi.charts.render.circular.*;
import com.arbergashi.charts.render.common.*;
import com.arbergashi.charts.render.financial.*;
import com.arbergashi.charts.render.grid.*;
import com.arbergashi.charts.render.medical.*;
import com.arbergashi.charts.render.specialized.*;
import com.arbergashi.charts.render.standard.*;
import com.arbergashi.charts.render.statistical.*;
import com.arbergashi.charts.util.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Recording-canvas probes retained for the dated renderer audit. Output records
 * the exercised behavior, including known defects, rather than asserting pass.
 */
public class ChartsRendererAudit {
    static class Op {
        final String kind; final float[] xs,ys; final int argb; final float stroke;
        Op(String k,float[] x,float[] y,int c,float s) { kind=k;xs=x;ys=y;argb=c;stroke=s; }
        String signature() {return kind+Arrays.toString(xs)+Arrays.toString(ys)+argb+stroke;}
    }
    static class RecordingCanvas implements ArberCanvas {
        List<Op> ops=new ArrayList<>(); int textCount; int color; float stroke; float penX,penY; ArberRect clip;
        public void setColor(ArberColor c){color=c.argb();}
        public void setStroke(float w){stroke=w;}
        public void moveTo(float x,float y){penX=x;penY=y;}
        public void lineTo(float x,float y){drawPolyline(new float[]{penX,x},new float[]{penY,y},2);penX=x;penY=y;}
        public void drawPolyline(float[]x,float[]y,int n){ops.add(new Op("line",Arrays.copyOf(x,n),Arrays.copyOf(y,n),color,stroke));}
        public void drawRect(float x,float y,float w,float h){ops.add(new Op("rect",new float[]{x,x+w},new float[]{y,y+h},color,stroke));}
        public void fillRect(float x,float y,float w,float h){ops.add(new Op("fillRect",new float[]{x,x+w},new float[]{y,y+h},color,stroke));}
        public void fillPolygon(float[]x,float[]y,int n){ops.add(new Op("fill",Arrays.copyOf(x,n),Arrays.copyOf(y,n),color,stroke));}
        public void drawVoxelField(VoxelBuffer b){}
        public ArberMatrix getTransform(){return null;}
        public void setClip(ArberRect c){clip=c;}
        public ArberRect getClip(){return clip;}
        public void drawText(float x,float y,String t){textCount++;}
        String signature(){return ops.stream().map(Op::signature).reduce("",String::concat);}
        double maxY(){return ops.stream().flatMapToDouble(o->{double[]a=new double[o.ys.length];for(int i=0;i<a.length;i++)a[i]=o.ys[i];return Arrays.stream(a);}).max().orElse(Double.NaN);}
    }
    static DefaultPlotContext context(ChartModel m,double x0,double x1,double y0,double y1){
        return new DefaultPlotContext(new ArberRect(0,0,640,400),m,x0,x1,y0,y1,ChartThemes.getDarkTheme(),null);
    }
    static RecordingCanvas render(ChartRenderer r,ChartModel m,PlotContext c){RecordingCanvas a=new RecordingCanvas();r.render(a,m,c);return a;}
    public static void main(String[] args) {
        DefaultChartModel weights=new DefaultChartModel("weights");
        weights.setPoint(0,40,0,0,40,"A");weights.setPoint(1,60,0,0,60,"B");
        var weightContext=context(weights,0,1,0,100);
        var donutA=render(new DonutRenderer().setCenterText("Revenue").setCenterSubText("2026"),weights,weightContext);
        var donutB=render(new DonutRenderer(),weights,weightContext);
        System.out.println("DONUT textCalls="+donutA.textCount+" sameCommandsWithAndWithoutText="+donutA.signature().equals(donutB.signature()));
        PieRenderer pie=new PieRenderer();
        var pieDark=render(pie,weights,weightContext);
        var nordContext=context(weights,0,1,0,100).setTheme(ChartThemes.getNordTheme());
        var pieReused=render(pie,weights,nordContext);
        var pieFresh=render(new PieRenderer(),weights,nordContext);
        System.out.printf("PIE dark=%08x reusedNord=%08x freshNord=%08x%n",pieDark.ops.get(0).argb,pieReused.ops.get(0).argb,pieFresh.ops.get(0).argb);
        LatencyTracker tracker=new LatencyTracker(256);for(int i=0;i<200;i++)tracker.record(1_000_000L+i*1000L);
        ChartAssets.setProperty("Chart.circular.performance.enabled","true");
        ChartAssets.setProperty("Chart.performance.audit.enabled","true");
        ChartAssets.setProperty("Chart.scale.physical.enabled","true");
        var latency=render(new CircularLatencyOverlayRenderer(tracker),weights,weightContext);
        var physical=render(new PhysicalScaleRenderer(),weights,weightContext);
        RecordingCanvas performance=new RecordingCanvas();new PerformanceAuditRenderer(tracker).render(performance,weightContext,ChartThemes.getDarkTheme());
        System.out.println("OVERLAYS circularDraw="+latency.ops.size()+" circularText="+latency.textCount+" physicalDraw="+physical.ops.size()+" performanceDraw="+performance.ops.size()+" performanceText="+performance.textCount);
        DefaultStatisticalChartModel stats=new DefaultStatisticalChartModel("box");stats.setBoxPlot(0.5,50,20,60,0,100,"A");
        var box=render(new BoxPlotRenderer(),stats,context(stats,0,1,0,100));
        for(Op o:box.ops) if(o.kind.equals("fillRect") || (o.kind.equals("line")&&o.stroke>1.6))System.out.println("BOX "+o.kind+" y="+Arrays.toString(o.ys)+" stroke="+o.stroke);
        DefaultChartModel financial=new DefaultChartModel("live");
        for(int i=0;i<50;i++)financial.setPoint(i,200+i,198+i,202+i,1,"P");
        var financialContext=context(financial,0,49,0,20); ATRRenderer atr=new ATRRenderer();
        var before=render(atr,financial,financialContext);long oldStamp=financial.getUpdateStamp();
        financial.clear();for(int i=0;i<50;i++)financial.setPoint(i,200+i,180+i,220+i,1,"P");
        var after=render(atr,financial,financialContext);var fresh=render(new ATRRenderer(),financial,financialContext);
        System.out.println("ATR stamp="+oldStamp+"->"+financial.getUpdateStamp()+" reusedUnchanged="+before.signature().equals(after.signature())+" reusedEqualsFresh="+after.signature().equals(fresh.signature()));
        DefaultChartModel flow=new DefaultChartModel("disjoint");flow.setPoint(0,5,0,0,5,"A:B");flow.setPoint(1,5,0,0,5,"C:D");
        var sankey=render(new SankeyProRenderer(),flow,context(flow,0,1,0,5));
        System.out.println("SANKEY maxY="+sankey.maxY()+" plotMaxY=400");
        var ternary=new DefaultTernaryChartModel(new ArrayList<>(List.of(new DefaultTernaryChartModel.DefaultTernaryPoint(1,0,0),new DefaultTernaryChartModel.DefaultTernaryPoint(0,1,0),new DefaultTernaryChartModel.DefaultTernaryPoint(0,0,1))),List.of("A","B","C"));
        var ternaryContext=context(ternary,0,1,0,1);var points=render(new TernaryPlotRenderer(),ternary,ternaryContext);RecordingCanvas ternaryGrid=new RecordingCanvas();new TernaryGridLayer().renderGrid(ternaryGrid,ternaryContext);
        for(int i=0;i<3;i++){Op o=points.ops.get(i);double x=0,y=0;for(int j=0;j<o.xs.length;j++){x+=o.xs[j];y+=o.ys[j];}System.out.println("TERNARY point"+i+"="+x/o.xs.length+","+y/o.ys.length);}
        for(int i=0;i<3;i++){Op o=ternaryGrid.ops.get(i);System.out.println("TERNARY gridEdge"+i+"="+Arrays.toString(o.xs)+","+Arrays.toString(o.ys));}
        try { new MedicalGridLayer().renderGridBatch(new SpatialPathBatchBuilder(),weightContext,new GridBatchConfig());System.out.println("MEDICAL_GRID batch=OK"); }
        catch(Exception e){System.out.println("MEDICAL_GRID batch="+e);}
        CircularFastMedicalModel partial=new CircularFastMedicalModel(128,2);for(int i=0;i<20;i++)partial.add(i,new double[]{1,2});
        var ecg=render(new ECGRenderer(),partial,context(partial,0,128,-1,3));
        int pointsCount=ecg.ops.stream().mapToInt(o->o.xs.length).sum();int zeroY=0;
        double[] originPixel=new double[2];context(partial,0,128,-1,3).mapToPixel(0,0,originPixel);
        for(Op o:ecg.ops)for(int i=0;i<o.xs.length;i++)if(o.xs[i]==originPixel[0]&&o.ys[i]==originPixel[1])zeroY++;
        System.out.println("ECG partialValid="+partial.getPointCount()+" recordedVertices="+pointsCount+" uninitializedOriginVertices="+zeroY);
        DefaultChartModel predictiveModel=new DefaultChartModel("predictive");predictiveModel.setXY(0,1);predictiveModel.setXY(1,2);
        var predictive=render(new PredictivePathRenderer(PredictivePathRenderer.Style.BAR),predictiveModel,context(predictiveModel,0,100,0,100));
        System.out.println("PREDICTIVE_BAR "+predictive.ops.get(0).signature());
        SpatialPathBatchBuilder ternaryBatch=new SpatialPathBatchBuilder();
        new TernaryPlotRenderer().renderSpatial(ternary,ternaryContext,ternaryBatch);
        System.out.print("TERNARY_SPATIAL count="+ternaryBatch.getBatch().getPointCount()+" moveFlags=");
        for(int i=0;i<ternaryBatch.getBatch().getPointCount();i++)System.out.print(ternaryBatch.isMoveTo(i)+",");
        System.out.println(" firstMarker="+SpatialStyleDescriptor.unpackMarkerId(ternaryBatch.getBatch().getStyleKey(0)));
        for(String gridName:List.of("DefaultGridLayer","MedicalGridLayer","FinancialGridLayer","StatisticalGridLayer","AnalysisGridLayer","IsometricGridLayer","SmithChartGridLayer","PolarGridLayer","TernaryGridLayer","GeoGridLayer","LogarithmicGridLayer")) {
            try {
                GridLayer grid=(GridLayer)Class.forName("com.arbergashi.charts.render.grid."+gridName).getConstructor().newInstance();
                var gridContext=context(weights,0,10,-2,2); RecordingCanvas rc=new RecordingCanvas(); grid.renderGrid(rc,gridContext);
                SpatialPathBatchBuilder b=new SpatialPathBatchBuilder(); String batchResult;
                try {grid.renderGridBatch(b,gridContext,new GridBatchConfig());batchResult="points="+b.getBatch().getPointCount();}
                catch(Exception ex){batchResult=ex.getClass().getSimpleName();}
                System.out.println("GRID "+gridName+" canvasOps="+rc.ops.size()+" textCalls="+rc.textCount+" batch="+batchResult);
            } catch(Exception ex){throw new RuntimeException(ex);}
        }
        CRC32 crc=new CRC32();crc.update(new byte[640*360*4]);System.out.println("EMPTY_RASTER_CRC32="+crc.getValue());
    }
}
