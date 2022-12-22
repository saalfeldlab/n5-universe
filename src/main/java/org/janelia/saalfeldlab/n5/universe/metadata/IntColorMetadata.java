package org.janelia.saalfeldlab.n5.universe.metadata;

import net.imglib2.type.numeric.ARGBType;

/**
 * A {@link ColorMetadata} that stores color as a single int.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class IntColorMetadata implements ColorMetadata {

	private int rgba;

	public IntColorMetadata( int rgba ) {
		this.rgba = rgba;
	}

	@Override
	public ARGBType getColor() {
		return new ARGBType( rgba );
	}

}
