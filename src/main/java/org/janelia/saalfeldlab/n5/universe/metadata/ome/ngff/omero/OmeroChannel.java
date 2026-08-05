package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero;

/**
 * Rendering settings for one channel, i.e. one element along the {@code c}
 * dimension of the image.
 */
public class OmeroChannel {

	public static final String FAMILY_LINEAR = "linear";

	public static final String FAMILY_POLYNOMIAL = "polynomial";

	public static final String FAMILY_EXPONENTIAL = "exponential";

	public static final String FAMILY_LOGARITHMIC = "logarithmic";

	public static final String DEFAULT_COLOR = "FFFFFF";

	public final boolean active;

	public final double coefficient;

	/** color as an RRGGBB hex string, without a leading '#' */
	public final String color;

	public final String family;

	public final boolean inverted;

	public final String label;

	public final OmeroWindow window;

	public OmeroChannel(final boolean active, final double coefficient, final String color,
			final String family, final boolean inverted, final String label,
			final OmeroWindow window) {

		this.active = active;
		this.coefficient = coefficient;
		this.color = color;
		this.family = family;
		this.inverted = inverted;
		this.label = label;
		this.window = window;
	}

	/**
	 * Creates an active, non-inverted channel with a linear transfer function.
	 *
	 * @param label the channel label
	 * @param color color as an RRGGBB hex string
	 * @param window the display window
	 */
	public OmeroChannel(final String label, final String color, final OmeroWindow window) {

		this(true, 1.0, color, FAMILY_LINEAR, false, label, window);
	}

	/**
	 * @return this channel's color packed as 0xAARRGGBB with full alpha, or
	 *         opaque white if the color is not a valid hex string
	 */
	public int getColorARGB() {

		return 0xff000000 | parseColor(color);
	}

	/**
	 * Parses an OMERO color string, e.g. {@code "0000FF"}, into an RGB integer.
	 * A leading {@code '#'} is tolerated.
	 *
	 * @param color the color string
	 * @return the color as 0xRRGGBB, or white if it cannot be parsed
	 */
	public static int parseColor(final String color) {

		if (color == null)
			return 0xffffff;

		final String hex = color.startsWith("#") ? color.substring(1) : color;
		try {
			return Integer.parseInt(hex, 16) & 0xffffff;
		} catch (final NumberFormatException e) {
			return 0xffffff;
		}
	}

	/**
	 * Formats the given RGB (or ARGB) integer as an OMERO color string.
	 *
	 * @param rgb the color
	 * @return an uppercase RRGGBB hex string
	 */
	public static String toColorString(final int rgb) {

		return String.format("%06X", rgb & 0xffffff);
	}
}
