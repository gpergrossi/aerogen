package dev.mortus.aerogen.world.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.mortus.aerogen.world.islands.Island;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FeatureRandomMeta extends Feature {

	List<Feature> treeTypes;
	List<Float> treeWeights;
	float totalWeights = 0f;
	
	public FeatureRandomMeta() {
		this.treeTypes = new ArrayList<>();
		this.treeWeights = new ArrayList<>();
	}
	
	public FeatureRandomMeta addFeature(float weight, Feature feature) {
		this.treeTypes.add(feature);
		this.treeWeights.add(weight);
		this.totalWeights += weight;
		return this;
	}
	
	protected Feature getRandomFeature(Random rand) {
		if (totalWeights == 0f) return null;
		float roll = rand.nextFloat()*totalWeights;
		for (int i = 0; i < treeTypes.size(); i++) {
			roll -= treeWeights.get(i);
			if (roll <= 0) return treeTypes.get(i);
		}
		return null;
	}
	
	@Override
	public boolean generate(World world, Island island, BlockPos position, Random rand) {
		Feature feature = getRandomFeature(rand);
		return feature.generate(world, island, position, rand);
	}
	
}
