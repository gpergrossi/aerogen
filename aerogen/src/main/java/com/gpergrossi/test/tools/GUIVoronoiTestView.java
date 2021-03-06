package com.gpergrossi.test.tools;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Random;

import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.shapes.LineSeg;
import com.gpergrossi.util.geom.shapes.Ray;
import com.gpergrossi.util.geom.shapes.Spline;
import com.gpergrossi.util.geom.shapes.Convex;
import com.gpergrossi.util.geom.shapes.Line;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.viewframe.View;
import com.gpergrossi.viewframe.ViewerFrame;
import com.gpergrossi.viewframe.chunks.ChunkLoader;
import com.gpergrossi.viewframe.chunks.InfiniteVoronoiChunk;
import com.gpergrossi.viewframe.chunks.InfiniteVoronoiChunkLoader;
import com.gpergrossi.viewframe.chunks.View2DChunkManager;
import com.gpergrossi.voronoi.Site;
import com.gpergrossi.voronoi.Voronoi;
import com.gpergrossi.voronoi.VoronoiBuilder;
import com.gpergrossi.voronoi.VoronoiWorker;

public class GUIVoronoiTestView extends View {
	
	public static ViewerFrame frame;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					System.setProperty("sun.java2d.opengl", "true");
					frame = new ViewerFrame(new GUIVoronoiTestView(0, 0, 1024, 768));
					frame.setVisible(true);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected double getMinZoom() {
		return 0.25;
	}
	
	@Override
	protected void renderSettings(Graphics2D g2d) {
		// Background Black
		g2d.setBackground(new Color(0,0,0,255));
	}
	
	boolean useChunkLoader = false;
	
	VoronoiBuilder voronoiBuilder;
	VoronoiWorker voronoiWorker;

	ChunkLoader<InfiniteVoronoiChunk> chunkLoader;
	View2DChunkManager<InfiniteVoronoiChunk> chunkManager;
//	ChunkLoader<MinecraftViewChunk> chunkLoader;
//	View2DChunkManager<MinecraftViewChunk> chunkManager;

	Spline spline;
	
	double seconds;
	double printTime;
	double radiansPerDegree = (Math.PI/180.0);
	
	Convex poly = new Circle(0, 200, 100).toPolygon(5);
	
	public GUIVoronoiTestView (double x, double y, double width, double height) {
		super (x, y, width, height);
		
		voronoiBuilder = new VoronoiBuilder();
		voronoiBuilder.setBounds(new Circle(300, 0, 100).toPolygon(5));
	}

	
	
	@Override
	public void init() {
		if (!useChunkLoader) {
			spline = new Spline();
			for (int i = 0; i <= 8; i++) {
				Double2D randomPt = new Double2D(Math.random()*3000-1500, Math.random()*2000-1000);
				spline.addGuidePoint(i, randomPt);
			}
		} else {
//			chunkLoader = new MinecraftViewChunkLoader(8964591453215L);
//			chunkManager = new View2DChunkManager<MinecraftViewChunk>(chunkLoader, 3);
			chunkLoader = new InfiniteVoronoiChunkLoader(8964591453215L);
			chunkManager = new View2DChunkManager<InfiniteVoronoiChunk>(chunkLoader, 1);
		}
	}
	
	@Override
	public void start() {
		if (useChunkLoader) chunkManager.start();
	}

	@Override
	public void stop() {
		if (useChunkLoader) chunkManager.stop();
	}

	@Override
	public void update(double secondsPassed) {
		seconds += secondsPassed;

		printTime += secondsPassed;
		if (printTime > 1) {
			printTime -= 1;
			frame.setTitle("FPS = "+String.format("%.2f", getFPS()));
		}
	}
	
	@Override
	public void drawWorld(Graphics2D g2d) {
		if (!useChunkLoader) {
			// Clock dots
			g2d.setColor(Color.WHITE);
			for (int i = 0; i < 60; i++) {
				double an = i * (Math.PI / 30.0);
				double ew = 5, eh = 5;
				double ex = Math.cos(an)*100-ew/2;
				double ey = Math.sin(an)*100-eh/2;
				Ellipse2D ellipse = new Ellipse2D.Double(ex, ey, ew, eh);
		
				if (ellipse.contains(mX, mY)) g2d.setColor(Color.YELLOW);
				g2d.fill(ellipse);
				g2d.setColor(Color.WHITE);
			}
			
			// Clock center
			Ellipse2D.Double ellipse2 = new Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0);
			if (ellipse2.contains(mX, mY)) g2d.setColor(Color.YELLOW);
			g2d.fill(ellipse2);
			g2d.setColor(Color.WHITE);
			
			// Clock hand
			AffineTransform before = g2d.getTransform();
			Path2D.Double path = new Path2D.Double();
			path.moveTo(100, 0);
			path.lineTo(0, 2);
			path.lineTo(0, -2);
			path.closePath();
			g2d.rotate(seconds*(Math.PI / 30.0));
			g2d.fill(path);
			g2d.setTransform(before);
			
			Double2D mouse = new Double2D(this.getMouseWorldX(), this.getMouseWorldY());
			if (poly.contains(mouse)) g2d.setColor(Color.ORANGE); 
			else g2d.setColor(Color.WHITE);
			g2d.fill(poly.asAWTShape());
			
			g2d.setColor(Color.BLUE);
			double a = seconds*(Math.PI / 30.0);
			Ray ray = new Ray(0, 0, Math.cos(a), Math.sin(a));
			LineSeg seg = poly.clip(ray);
			if (seg != null) g2d.draw(seg.asAWTShape());
			
			g2d.setColor(Color.WHITE);
			
			// Draw spline
			Double2D.Mutable current = new Double2D.Mutable();
			Double2D.Mutable previous = new Double2D.Mutable();
			spline.getPoint(previous, 0);
			
			double steps = 800;
			double step = 8.0/steps;
			for (double d = -step; d < 8+step; d += step) {
				spline.getPoint(current, d + Math.random()*step);
				
				g2d.draw(new Line2D.Double(previous.x(), previous.y(), current.x(), current.y()));
				
				Double2D.Mutable swap = previous;
				previous = current;
				current = swap;
			}
			
			for (Double2D pt : spline.getGuidePoints()) {
				g2d.fill(new Ellipse2D.Double(pt.x()-10, pt.y()-10, 20, 20));
			}
			
			Double2D dir = mouse.perpendicular().normalize();
			Line line = new LineSeg(mouse.x(), mouse.y(), mouse.x()+dir.x(), mouse.y()+dir.y());
			LineSeg mirror = line.toSegment(100, 1000);
			Convex refl = poly.reflect(mirror);
			Double2D reflC = refl.getCentroid();
			
			g2d.draw(mirror.asAWTShape());
			g2d.drawOval((int)reflC.x()-20, (int)reflC.y()-20, 40, 40);
			g2d.draw(refl.asAWTShape());		
			
		}
		
		if (useChunkLoader) { 
			Rectangle2D bounds = this.getViewWorldBounds();
			chunkManager.setView(bounds);
			chunkManager.update();
			chunkManager.draw(g2d);
		}
		
		if (voronoiWorker != null) {
			if (voronoiWorker.isDone()) {
//				voronoiWorker.debugDraw(g2d);
				Voronoi v = voronoiWorker.getResult();
				Random r = new Random(0);
				
				for (Site site : v.getSites()) {					
					// Draw shape
					g2d.setColor(Color.getHSBColor(r.nextFloat(), 1.0f, 0.5f + r.nextFloat()*0.5f));
					Convex poly = site.getPolygon();
					Shape polyShape = poly.asAWTShape();
					if (polyShape != null) g2d.fill(polyShape);
					
					// Draw original point
					g2d.setColor(Color.WHITE);
					Ellipse2D sitePt = new Ellipse2D.Double(site.getX()-1, site.getY()-1, 2, 2);
					g2d.fill(sitePt);
					
					// Draw centroid
					g2d.setColor(Color.BLACK);
					Double2D centroid = poly.getCentroid();
					Ellipse2D siteCentroid = new Ellipse2D.Double(centroid.x()-1, centroid.y()-1, 2, 2);
					g2d.fill(siteCentroid);
				}
			} else {
				voronoiWorker.debugDraw(g2d);
			}
		} else {
			for (Double2D site : voronoiBuilder.getSites()) {
				Ellipse2D ellipse = new Ellipse2D.Double(site.x()-1, site.y()-1, 2, 2);
				g2d.fill(ellipse);
			}
			g2d.draw(voronoiBuilder.getBounds().asAWTShape());
		}
		
//		Rect r = new Rect(-60, -40, 120, 80);
//		g2d.draw(r.getShape2D());
//		
//		Vec2[] verts = new Vec2[4];
//		verts[0] = new Vec2(getMouseWorldX()+20, getMouseWorldY());
//		verts[1] = new Vec2(getMouseWorldX(), getMouseWorldY()+20);
//		verts[2] = new Vec2(getMouseWorldX()-20, getMouseWorldY());
//		verts[3] = new Vec2(getMouseWorldX()-20, getMouseWorldY()-20);
//		Polygon tri = new Polygon(verts);
//		
//		if (tri.intersects(r)) g2d.fill(tri.getShape2D());
//		else g2d.draw(tri.getShape2D());
//		
//		Rect r2 = new Rect(200, -5, 10, 10);
//		if (tri.intersects(r2)) g2d.fill(r2.getShape2D());
//		else g2d.draw(r2.getShape2D());
		
//		// Mouse velocity trail
//		g2d.setColor(Color.WHITE);
//		Line2D mVel = new Line2D.Double(mX, mY, mX+mVelX, mY+mVelY);
//		g2d.draw(mVel);
	}

	@Override
	public void drawOverlayUI(Graphics2D g2d) {
		if (useChunkLoader) {
			g2d.setColor(Color.WHITE);
			g2d.clearRect(0, 0, 200, 30);
			g2d.drawString("Chunks loaded: "+chunkManager.getNumLoaded(), 10, 20);
		}
	}

	double startPX, startPY;
	double startViewX, startViewY;
	boolean panning = false;
	
	double mX, mY;
	double mDX, mDY;
	double mVelX, mVelY;

	@Override
	public void mousePressed() {}

	@Override
	public void mouseDragged() {}

	@Override
	public void mouseReleased() {
		int click = this.getMouseClick();
		double px = this.getMouseWorldX();
		double py = this.getMouseWorldY();
		
		if (click == View.RIGHT_CLICK || click == View.LEFT_CLICK) {
			Point2D clickP = new Point2D.Double(px, py);
			int x = (int) Math.floor((clickP.getX()+2) / 4);
			int y = (int) Math.floor((clickP.getY()+2) / 4);
			clickP = new Point2D.Double(x*4, y*4);

			if (click == View.RIGHT_CLICK) {
				for (Double2D site : voronoiBuilder.getSites()) {
					Point2D point = site.toPoint2D();
					if (clickP.distance(point) < 4) {
						System.out.println("Removing site");
						voronoiBuilder.removeSite(voronoiBuilder.findSiteIndex(site));
						break;
					}
				}
			}
			
			if (click == View.LEFT_CLICK) {
				int id = voronoiBuilder.addSite(new Double2D(clickP.getX(), clickP.getY()));
				System.out.println("Adding site "+id);
			}
			
			voronoiWorker = null;
		}
	}

	@Override
	public void mouseMoved() {
		if (spline != null) {
			double mx = getMouseScreenX();
			double screenX = getViewWidth();
			double alpha = mx / screenX;
			System.out.println("catmull rom alpha = "+alpha);
			spline.setCatmullRomAlpha(alpha);
		}
	}

	@Override
	public void mouseScrolled() {}

	@Override
	public void keyPressed() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (voronoiWorker == null) voronoiWorker = voronoiBuilder.getBuildWorker();
			else voronoiWorker.doWork(0);
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if (voronoiWorker == null) {
				voronoiWorker = voronoiBuilder.getBuildWorker();
			} else if (voronoiWorker.isDone()) {
				Voronoi v = voronoiWorker.getResult();
				for (Site s : v.getSites()) {
					if (s.getPolygon().getArea() == 0) {
						throw new RuntimeException("Zero Area!");
					}
				}
				voronoiBuilder.clearSites(true);
				for (Site s : v.getSites()) {
					voronoiBuilder.addSite(s.getPolygon().getCentroid());
				}
				voronoiWorker = voronoiBuilder.getBuildWorker();
			}
			try {
				while (!voronoiWorker.isDone()) {
					voronoiWorker.doWork(0);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				voronoiWorker = null;
			}
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			if (voronoiWorker != null) { 
				voronoiWorker.debugAdvanceSweepline(-1);
			}
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			if (voronoiWorker != null) { 
				voronoiWorker.debugAdvanceSweepline(+1);
			}
		} else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			if (voronoiWorker != null) { 
				voronoiWorker.stepBack();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_S) {
			System.out.println("Saving");
			try {
				voronoiBuilder.savePoints();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_L) {
			System.out.println("Loading");
			try {
				voronoiBuilder.loadPoints();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_C) {
			System.out.println("Clearing");
			voronoiBuilder.clearSites();
			voronoiBuilder.setBounds(new Circle(300, 0, 100).toPolygon(5));
			voronoiWorker = null;
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			this.setSlowZoom(9.0/14.0);
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			this.setSlowZoom(14.0/9.0);
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			if (!this.isRecording()) {
				System.out.println("Recording started");
				this.startRecording();
			} else {
				System.out.println("Recording finished");
				this.stopRecording();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_SLASH) {
			for (double d = Math.PI/2.0; d < Math.PI*1.0; d += Math.PI/17.3) {
				voronoiBuilder.addSite(new Double2D(Math.cos(d)*d*100, Math.sin(d)*d*100));
				voronoiBuilder.addSite(new Double2D(Math.cos(d+Math.PI*2.0/3.0)*d*100, Math.sin(d+Math.PI*2.0/3.0)*d*100));
				voronoiBuilder.addSite(new Double2D(Math.cos(d-Math.PI*2.0/3.0)*d*100, Math.sin(d-Math.PI*2.0/3.0)*d*100));
			}
		} else if (e.getKeyCode() == KeyEvent.VK_SEMICOLON) {
			spline = new Spline();
			for (int i = 0; i <= 8; i++) {
				Double2D randomPt = new Double2D(Math.random()*3000-1500, Math.random()*2000-1000);
				spline.addGuidePoint(i, randomPt);
			}
		}
	}

	@Override
	public void keyReleased() {
		KeyEvent e = getKeyEvent();
		if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			setSlowZoom(1.0);
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			setSlowZoom(1.0);
		}
	}
	
	@Override
	public void keyTyped() { }
	
}
