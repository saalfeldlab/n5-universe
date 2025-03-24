package org.janelia.saalfeldlab.n5.universe;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.hdf5.HDF5Utils;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum StorageFormat {
	ZARR(Pattern.compile("zarr", Pattern.CASE_INSENSITIVE), uri -> Pattern.compile("\\.zarr$", Pattern.CASE_INSENSITIVE).matcher(new File(uri.getPath()).toString()).find()),
	N5(Pattern.compile("n5", Pattern.CASE_INSENSITIVE), uri -> Pattern.compile("\\.n5$", Pattern.CASE_INSENSITIVE).matcher(new File(uri.getPath()).toString()).find()),
	HDF5(Pattern.compile("h(df)?5", Pattern.CASE_INSENSITIVE), uri -> {
		final boolean hasHdf5Extension = Pattern.compile("\\.h(df)?5$", Pattern.CASE_INSENSITIVE).matcher(uri.getPath()).find();
		return hasHdf5Extension || HDF5Utils.isHDF5(uri.getPath());
	});

	static final Pattern STORAGE_SCHEME_PATTERN = Pattern.compile("^(\\s*(?<storageScheme>(n5|h(df)?5|zarr)):(//)?)?(?<uri>.*)$", Pattern.CASE_INSENSITIVE);
	private final static String STORAGE_SCHEME_GROUP = "storageScheme";
	private final static String URI_GROUP = "uri";

	final Pattern schemePattern;
	private final Predicate<URI> uriTest;

	StorageFormat(final Pattern schemePattern, final Predicate<URI> test) {

		this.schemePattern = schemePattern;
		this.uriTest = test;
	}

	public static StorageFormat guessStorageFromUri(URI uri) {

		for (final StorageFormat format : StorageFormat.values()) {
			if (format.uriTest.test(uri))
				return format;
		}
		return null;
	}

	public static Pair<StorageFormat, URI> parseUri(String uri) {

		final Pair<StorageFormat, String> storageFromScheme = getStorageFromNestedScheme(uri);
		final URI asUri = N5Factory.parseUriFromString(storageFromScheme.getB());
		if (storageFromScheme.getA() != null)
			return new ValuePair<>(storageFromScheme.getA(), asUri);
		else
			return new ValuePair<>(guessStorageFromUri(asUri), asUri);

	}

	public static Pair<StorageFormat, String> getStorageFromNestedScheme(String uri) {

		final Matcher storageSchemeMatcher = StorageFormat.STORAGE_SCHEME_PATTERN.matcher(uri);
		storageSchemeMatcher.matches();
		final String storageFormatScheme = storageSchemeMatcher.group(STORAGE_SCHEME_GROUP);
		final String uriGroup = storageSchemeMatcher.group(URI_GROUP);
		if (storageFormatScheme != null) {
			for (final StorageFormat format : StorageFormat.values()) {
				if (format.schemePattern.asPredicate().test(storageFormatScheme))
					return new ValuePair<>(format, uriGroup);
			}
		}
		return new ValuePair<>(null, uriGroup);
	}

	private static final String ZARRAY = ".zarray";
	private static final String ZGROUP = ".zgroup";
	private static final String ZATTRS = ".zattrs";
	private static final String[] ZARR2_KEYS = new String[]{ZARRAY, ZGROUP, ZATTRS};
	private static final String Z3ATTRS = ".zattrs";
	private static final String N5_ATTRIBUTES = "attributes.json";

	public static @Nullable StorageFormat guessStorageFromKeys(final URI root, final KeyValueAccess kva) {
		final URI uri;
		if (root.isAbsolute())
			uri = root;
		else
			uri = URI.create("file://" + root);
		if (Arrays.stream(ZARR2_KEYS).anyMatch(it -> kva.exists(kva.compose(uri, it))))
			return StorageFormat.ZARR;
		else if (kva.exists(kva.compose(uri, N5_ATTRIBUTES)))
			return StorageFormat.N5;
		else return null;
	}
}
