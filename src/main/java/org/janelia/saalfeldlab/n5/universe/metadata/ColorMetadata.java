package org.janelia.saalfeldlab.n5.universe.metadata;

import net.imglib2.type.numeric.ARGBType;

/**
 * Interface for metadata that describes 
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface ColorMetadata {

	public ARGBType getColor();

}
