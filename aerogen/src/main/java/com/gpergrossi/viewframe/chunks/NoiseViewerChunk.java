package com.gpergrossi.viewframe.chunks;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.gpergrossi.util.math.func2d.Function2D;
import com.gpergrossi.util.math.func2d.SandDunes;

public class NoiseViewerChunk extends View2DChunk<NoiseViewerChunk> {

	public static NoiseViewerChunk constructor(ChunkManager<NoiseViewerChunk> manager, int chunkX, int chunkY) {
		return new NoiseViewerChunk(manager, chunkX, chunkY);
	}

	float chunkSize;
	int chunkSizeI;
			
	public NoiseViewerChunk(ChunkManager<NoiseViewerChunk> manager, int chunkX, int chunkY) {
		super(manager, chunkX, chunkY);
		chunkSize = (float) loader.getChunkSize();
		chunkSizeI = (int) chunkSize;
	}

	static Function2D noise = new SandDunes(9809414L); 
//	static Function2D noise = new RemapOperation(new FractalNoise2D(1.0/256.0, 4), t->t*0.5+0.5);
	
	BufferedImage image;

	@Override
	public void load() {
		image = new BufferedImage(chunkSizeI, chunkSizeI, BufferedImage.TYPE_INT_ARGB);
		
		int[] rgba = new int[chunkSizeI * chunkSizeI];
		image.getRGB(0, 0, chunkSizeI, chunkSizeI, rgba, 0, chunkSizeI);
		
		for (int y = 0; y < chunkSizeI; y++) {
			for (int x = 0; x < chunkSizeI; x++) {
				double px = x + chunkX*chunkSize;
				double py = y + chunkY*chunkSize;
				float val = (float) noise.getValue(px, py);
							
				rgba[y*chunkSizeI+x] = new Color(val, val, val).getRGB();
			}
		}
		
		image.setRGB(0, 0, chunkSizeI, chunkSizeI, rgba, 0, chunkSizeI);
	}

	@Override
	public void unload() {
		image = null;
	}

	@Override
	public void draw(Graphics2D g) {
		g.drawImage(image, (int) (chunkX*chunkSize), (int) (chunkY*chunkSize), null);
	}
	
}
