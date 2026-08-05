package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero;

/**
 * The display window of an {@link OmeroChannel}.
 * <p>
 * {@code min} and {@code max} are the limits of the data type or the acquired
 * data range, {@code start} and {@code end} are the limits currently used for
 * display.
 */
public class OmeroWindow {

	public final double min;

	public final double max;

	public final double start;

	public final double end;

	public OmeroWindow(final double min, final double max, final double start, final double end) {

		this.min = min;
		this.max = max;
		this.start = start;
		this.end = end;
	}

	/**
	 * Creates a window whose display range covers the whole data range.
	 *
	 * @param min the minimum
	 * @param max the maximum
	 */
	public OmeroWindow(final double min, final double max) {

		this(min, max, min, max);
	}
}
