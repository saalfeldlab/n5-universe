package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;


public class OmeNgffReference {
	
	private final String name;
	private final String path;

	public OmeNgffReference(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public OmeNgffReference(String name) {
		this(name,"");
	}

	public String getName() {

		return name;
	}

	public String getPath() {
		return path == null ? "" : path;
	}

	/**
	 * Returns the name of the coordinate system this reference points to,
	 * qualified by its path when it references an external one, e.g.
	 * {@code "CBCT/physical"}. If this reference has no path (i.e. it refers
	 * to a coordinate system local to the current group, such as a
	 * scene-level coordinate system), this is simply {@link #getName()}.
	 *
	 * @return the qualified coordinate system name
	 */
	public String getQualifiedName() {
		return (path == null || path.isEmpty()) ? name : path + "/" + name;
	}

	public final static OmeNgffReference DUMMY = new OmeNgffReference("", "");

	public String toString() {
		return path == null ? name : path + "/" + name;
	}

}
