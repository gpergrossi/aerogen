package dev.mortus.cells;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import dev.mortus.chunks.Chunk;
import dev.mortus.util.math.geom.Polygon;
import dev.mortus.util.math.geom.Rect;
import dev.mortus.util.math.geom.Vec2;
import dev.mortus.voronoi.Site;
import dev.mortus.voronoi.Voronoi;
import dev.mortus.voronoi.VoronoiBuilder;

public class CellChunk extends Chunk {

	private static final long MAX_INTEGER = (long) Integer.MAX_VALUE;
	public static final double CHUNK_SIZE = 16*16;
	public static long WORLD_SEED = 8964591453215L;
	
	private long generateSeed() {
		Random r = new Random(WORLD_SEED);
		long x = (chunkX * (long) r.nextInt(Integer.MAX_VALUE)) % MAX_INTEGER;
		long y = (chunkY * (long) r.nextInt(Integer.MAX_VALUE)) % MAX_INTEGER;
		
		r.setSeed(x ^ (y << 32) ^ r.nextLong());
		return r.nextLong();			
	}
	
	CellChunkLoader loader;
	Rect bounds;
	long seed;
	Vec2 site;
	
	Voronoi voronoi; 
	Vec2[] sites;
	long[] seeds;
	
	public CellChunk(CellChunkLoader loader, int chunkX, int chunkY) {
		super(chunkX, chunkY);
		this.loader = loader;
	}

	@Override
	public void load() {
		if (site == null) minimumLoad();
		bounds = new Rect(chunkX * CHUNK_SIZE, chunkY * CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);
		drawBounds = bounds.toRectangle2D();

		sites = new Vec2[9];
		seeds = new long[9];
		int i = 0;
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				CellChunk chunk = this;
				if (x != 0 || y != 0) chunk = loader.getChunk(chunkX+x, chunkY+y);
				sites[i] = chunk.getSite();
				seeds[i] = chunk.getSeed();
				i++;
			}
		}
		
		VoronoiBuilder vb = new VoronoiBuilder(9);
		for (Vec2 s : sites) {
			vb.addSite(s);
		}
		voronoi = vb.build();
	}
	
	protected Vec2 getSite() {
		if (site == null) minimumLoad();
		return site;
	}

	protected long getSeed() {
		if (site == null) minimumLoad();
		return seed;
	}
	
	protected void minimumLoad() {
		seed = generateSeed();
		Random rand = new Random(seed);
		site = new Vec2((chunkX + rand.nextDouble())*CHUNK_SIZE, (chunkY + rand.nextDouble())*CHUNK_SIZE);
	}

	@Override
	public void unload() {
		
	}

	
	
	// For drawing
	Rectangle2D drawBounds;
	
	@Override
	public void draw(Graphics2D g) {
		g.setClip(drawBounds);
		
		if (voronoi != null) {
			for (int i = 0; i < 9; i++) {
				Site site = voronoi.getSites(sites[i]);
				//long seed = seeds[i];
				//Random r = new Random(seed);
				
				// Draw shape
				//g.setColor(Color.getHSBColor(r.nextFloat(), 1.0f, 0.5f + r.nextFloat()*0.5f));
				Polygon poly = site.getPolygon();
				Shape polyShape = poly.getShape2D();
				if (polyShape != null) g.draw(polyShape);
				
				// Draw original point
				g.setColor(Color.WHITE);
				Ellipse2D sitePt = new Ellipse2D.Double(site.getX()-1, site.getY()-1, 2, 2);
				g.fill(sitePt);
			}
		}

		int minX = (int) bounds.minX();
		int minY = (int) bounds.minY();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				g.drawRect(minX + x*16, minY + y*16, 16, 16);
			}	
		}
		
		g.setClip(null);
	}

}
