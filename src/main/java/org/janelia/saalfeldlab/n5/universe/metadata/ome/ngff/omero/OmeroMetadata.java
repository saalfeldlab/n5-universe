package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero;

/**
 * The "omero" transitional metadata of an OME-NGFF image, describing how the
 * image should be rendered.
 *
 * @see <a href=
 *      "https://ngff.openmicroscopy.org/latest/#omero-md">ngff.openmicroscopy.org</a>
 */
public class OmeroMetadata {

	public static final String KEY = "omero";

	public final int id;

	public final String name;

	public final String version;

	// one entry per element of the {@code c} dimension
	public final OmeroChannel[] channels;

	public final OmeroRdefs rdefs;

	public OmeroMetadata(final int id, final String name, final String version,
			final OmeroChannel[] channels, final OmeroRdefs rdefs) {

		this.id = id;
		this.name = name;
		this.version = version;
		this.channels = channels;
		this.rdefs = rdefs;
	}

	public OmeroMetadata(final int id, final String name, final String version, final OmeroChannel[] channels) {

		this(id, name, version, channels, new OmeroRdefs());
	}

	public int numChannels() {

		return channels == null ? 0 : channels.length;
	}

	public OmeroChannel getChannel(final int i) {

		return channels[i];
	}
}
