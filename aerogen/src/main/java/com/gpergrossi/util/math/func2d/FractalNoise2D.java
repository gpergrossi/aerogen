package com.gpergrossi.util.math.func2d;

import java.util.Random;

public class FractalNoise2D implements Function2D {

	private long seed;
	private double persistence;
	private double frequency;
	private int octaves;
	private SimplexNoise2D[] generators;
	
	public FractalNoise2D(long seed, double frequency, int octaves, double persistence) {
		this.seed = seed;
		this.persistence = persistence;
		this.frequency = frequency;
		this.octaves = octaves;
		createGenerators();
	}
	
	public FractalNoise2D(long seed, double frequency, int octaves) {
		this(seed, frequency, octaves, 0.5);
	}
	
	public FractalNoise2D(double frequency, int octaves) {
		this((long)(Math.random()*Long.MAX_VALUE), frequency, octaves, 0.5);
	}
	
	/**
	 * Returns a value between -1.0 and 1.0. More octaves
	 * make values more likely to be in the middle.
	 */
	public double getValue(double x, double y) {
		double value = 0;
		double dividend = 0;
		double multiple = 1;
		for(int i = 0; i < octaves; i++) {
			value += generators[i].getValue(x, y)*multiple;
			dividend += multiple;
			multiple *= this.persistence;
		}
		return value/dividend;
	}
	
	public void setPersistence(double persistence) {
		this.persistence = persistence;
	}
	
	public void setFrequency(double frequency) {
		this.frequency = frequency;
		createGenerators();
	}
	
	public void setOctaves(int octaves) {
		this.octaves = octaves;
		createGenerators();
	}
	
	private void createGenerators() {
		Random r = new Random(this.seed);
		generators = new SimplexNoise2D[octaves];
		double power = 1.0;
		for(int i = 0; i < octaves; i++) {
			generators[i] = new SimplexNoise2D(r.nextLong(), this.frequency*power);
			power *= 2.0;
		}
	}

}
