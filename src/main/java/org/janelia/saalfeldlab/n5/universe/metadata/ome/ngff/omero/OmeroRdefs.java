package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero;

/**
 * The rendering definitions: which plane to show first, and how channels are
 * combined.
 */
public class OmeroRdefs {

	public static final String MODEL_COLOR = "color";

	public static final String MODEL_GREYSCALE = "greyscale";

	/** first timepoint to show the user */
	public final int defaultT;

	/** first z-slice to show the user */
	public final int defaultZ;

	/** {@link #MODEL_COLOR} or {@link #MODEL_GREYSCALE} */
	public final String model;

	public OmeroRdefs(final int defaultT, final int defaultZ, final String model) {

		this.defaultT = defaultT;
		this.defaultZ = defaultZ;
		this.model = model;
	}

	public OmeroRdefs() {

		this(0, 0, MODEL_COLOR);
	}

	public boolean isColor() {

		return MODEL_COLOR.equals(model);
	}

	public boolean isGreyscale() {

		return MODEL_GREYSCALE.equals(model);
	}
}
