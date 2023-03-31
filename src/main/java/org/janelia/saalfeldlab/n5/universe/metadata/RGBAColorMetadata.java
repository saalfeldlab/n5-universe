package org.janelia.saalfeldlab.n5.universe.metadata;

import net.imglib2.type.numeric.ARGBType;

/**
 * A {@link ColorMetadata} that stores rgba componenents as integers
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class RGBAColorMetadata implements ColorMetadata {

	private int red;
	private int green;
	private int blue;
	private int alpha;

	private transient ARGBType color;

	public RGBAColorMetadata( int red, int green, int blue, int alpha ) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
	}

	@Override
	public ARGBType getColor() {
		return new ARGBType( ARGBType.rgba(red, green, blue, alpha));
	}

}
