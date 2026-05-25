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

	public final static OmeNgffReference DUMMY = new OmeNgffReference("", "");

}
