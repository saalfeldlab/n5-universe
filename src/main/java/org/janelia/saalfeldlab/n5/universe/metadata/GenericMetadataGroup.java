package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.Arrays;

public class GenericMetadataGroup<T extends N5Metadata> extends AbstractN5Metadata implements N5MetadataGroup<T> {

	private final T[] children;

	public GenericMetadataGroup(String path, T[] children) {

		super(path);
		this.children = children;
	}

	@Override
	public String[] getPaths() {

		return Arrays.stream(children).map(N5Metadata::getPath).toArray(String[]::new);
	}

	@Override
	public T[] getChildrenMetadata() {

		return children;
	}

}
