package org.janelia.saalfeldlab.n5.universe.factory;

import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Path.N5FilePath;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The {@link StorageFormat} inferred for a location: the format (if one could
 * be determined), the URI with any storage-scheme prefix stripped off, and
 * <em>how</em> the format was determined.
 * <p>
 * Build one with {@link #of(String)}, which looks at the URI only. If the
 * result is a mere guess ({@link #isExplicit()} is false), confirm it against
 * the container itself with {@link #resolveFromKeys}.
 */
public final class InferredStorageFormat {

	/**
	 * How the format was determined, in decreasing order of trust.
	 */
	public enum Source {

		/** An explicit scheme prefix, e.g. {@code "zarr3://…"} — the caller asked for this format. */
		SCHEME,

		/** Keys or file content at the location. Authoritative, but costs I/O. */
		CONTENT,

		/** The path suffix, e.g. {@code ".n5"}. A guess. */
		SUFFIX,

		/** Nothing determined a format; {@link #format()} is {@code null}. */
		UNKNOWN
	}

	private final StorageFormat format;

	private final URI uri;

	private final Source source;

	private InferredStorageFormat(final StorageFormat format, final URI uri, final Source source) {

		this.format = format;
		this.uri = Objects.requireNonNull(uri);
		this.source = Objects.requireNonNull(source);
	}

	public static InferredStorageFormat of(final URI uri) {
		return of(uri.toString());
	}

	/**
	 * Infer the storage format of {@code uriString} from the URI:
	 * <ol>
	 * <li>from an explicit scheme prefix if there is one,</li>
	 * <li>otherwise from the path suffix.</li>
	 * </ol>
	 * If nothing can be inferred this way, do nothing else, in particular <em>do
	 * not try to read from the URI</em>. The returned {@code InferredStorageFormat}
	 * will have {@link #source()}{@link Source#UNKNOWN ==UNKNOWN} in this case.
	 *
	 * @param uriString
	 * 		a URI, optionally prefixed with a storage scheme such as {@code "zarr3://"}
	 *
	 * @return what was inferred; never {@code null}
	 *
	 * @throws N5Exception
	 * 		if {@code uriString} cannot be split into scheme and URI
	 */
	public static InferredStorageFormat of(final String uriString) {

		final Matcher matcher = schemeAndUriMatcher(uriString);

		final URI uri = N5Factory.parseUriFromString(matcher.group(URI_GROUP));

		final String schemeToken = matcher.group(SCHEME_GROUP);
		if (schemeToken != null) {
			final StorageFormat fromScheme = fromScheme(schemeToken);
			if (fromScheme != null)
				return new InferredStorageFormat(fromScheme, uri, Source.SCHEME);
		}

		final StorageFormat fromSuffix = fromSuffix(uri);
		return fromSuffix != null
				? new InferredStorageFormat(fromSuffix, uri, Source.SUFFIX)
				: new InferredStorageFormat(null, uri, Source.UNKNOWN);
	}

	/**
	 * @return the inferred format, or {@code null} if none was
	 * 		({@link #source()} is then {@link Source#UNKNOWN})
	 */
	public StorageFormat format() {

		return format;
	}

	/**
	 * @return the URI, with any storage-scheme prefix removed
	 */
	public URI uri() {

		return uri;
	}

	/**
	 * @return how {@link #format()} was determined
	 */
	public Source source() {

		return source;
	}

	/**
	 * Whether the caller stated the format explicitly, as a scheme prefix. Such
	 * a format should be used as-is; anything else is a guess.
	 *
	 * @return true if the format came from a scheme prefix
	 */
	public boolean isExplicit() {

		return source == Source.SCHEME;
	}

	/**
	 * Confirm a guessed format against what is actually at the location.
	 * <p>
	 * Returns {@code this} when the format was explicit (nothing to confirm) or
	 * when probing finds nothing; otherwise a new
	 * {@code InferredStorageFormat} with the probed format and
	 * {@link Source#CONTENT}. Probing is the only way to tell Zarr v2 from v3.
	 *
	 * @param root the container to probe. It is expected to be rooted at
	 * 		{@link #uri()}; the keys are looked up relative to it.
	 * @return this, or the probed result
	 */
	public InferredStorageFormat resolveFromKeys(final KeyValueRoot root) {

		if (isExplicit())
			return this;

		final StorageFormat fromKeys = fromKeys(root);
		return fromKeys != null ? new InferredStorageFormat(fromKeys, uri, Source.CONTENT) : this;
	}

	@Override
	public boolean equals(final Object o) {

		if (this == o)
			return true;
		if (!(o instanceof InferredStorageFormat))
			return false;
		final InferredStorageFormat other = (InferredStorageFormat)o;
		return format == other.format && uri.equals(other.uri) && source == other.source;
	}

	@Override
	public int hashCode() {

		return Objects.hash(format, uri, source);
	}

	@Override
	public String toString() {

		return "InferredStorageFormat[format=" + format + ", uri=" + uri + ", source=" + source + "]";
	}

	// ------------------------------------------------------------------------
	//
	// -- inference --
	//

	// NB: package-private, as are schemeAndUriMatcher, fromScheme and fromSuffix below. They
	// used to back the deprecated Pair-returning shims on StorageFormat, which were dropped in
	// the move to this package (they need imglib2), and could now be private.
	static final String SCHEME_GROUP = "scheme";
	static final String URI_GROUP = "uri";

	/*
	 * Splits "zarr3://some/path" into scheme and URI. The scheme alternation is
	 * built from StorageFormat.schemePattern(), so the accepted tokens have a
	 * single source of truth. Compiled once, here — not per call.
	 */
	private static final Pattern SCHEME_AND_URI = Pattern.compile(
			"^(?:\\s*(?<" + SCHEME_GROUP + ">"
					+ Arrays.stream(StorageFormat.values())
							.map(format -> format.schemePattern().pattern())
							.collect(Collectors.joining("|"))
					+ "):(?://)?)?(?<" + URI_GROUP + ">.*)$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Match {@code uriString} against {@link #SCHEME_AND_URI}, so that
	 * {@link #SCHEME_GROUP} and {@link #URI_GROUP} can be read off the result.
	 * <p>
	 * The pattern is total but for one case: {@code .} does not cross line
	 * terminators without {@code DOTALL}, so an embedded newline fails to
	 * match. That is the only way this throws.
	 *
	 * @param uriString
	 * 		a URI, optionally prefixed with a storage scheme
	 *
	 * @return a {@link Matcher} that has matched
	 *
	 * @throws N5Exception
	 * 		if {@code uriString} does not match
	 */
	static Matcher schemeAndUriMatcher(final String uriString) {

		final Matcher matcher = SCHEME_AND_URI.matcher(uriString);
		if (!matcher.matches())
			throw new N5Exception("Could not parse URI: \"" + uriString + "\"");
		return matcher;
	}

	private static final Pattern ZARR_SUFFIX = Pattern.compile("\\.zarr/*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern N5_SUFFIX = Pattern.compile("\\.n5/*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern HDF5_SUFFIX = Pattern.compile("\\.h(?:df)?5/*$", Pattern.CASE_INSENSITIVE);
	// NB: The URI of a directory may end in a slash, therefore the trailing "/*" in the patterns.
	// (In fact, FileSystemKeyValueRoot.uri() always ends in a slash.)

	private static final String[] ZARR2_KEYS = {".zarray", ".zgroup", ".zattrs"};
	private static final String ZARR3_ATTRIBUTES = "zarr.json";
	private static final String N5_ATTRIBUTES = "attributes.json";

	static StorageFormat fromScheme(final String schemeToken) {

		for (final StorageFormat format : StorageFormat.values()) {
			if (format.schemePattern().matcher(schemeToken).matches())
				return format;
		}
		return null;
	}

	/**
	 * A {@code ".zarr"} suffix says nothing about the version, so this answers
	 * {@link StorageFormat#ZARR} and leaves the version to be resolved later.
	 */
	static StorageFormat fromSuffix(final URI uri) {

		final String path = uri.getPath();
		if (path == null)
			// opaque URI, e.g. "mailto:" — no path to match a suffix against
			return null;

		if (ZARR_SUFFIX.matcher(path).find())
			return StorageFormat.ZARR;
		if (N5_SUFFIX.matcher(path).find())
			return StorageFormat.N5;
		// NB: isHdf5File takes the raw path on purpose -- new File() normalises a trailing
		// slash away, which is what fromKeys() below relies on too.
		if (HDF5_SUFFIX.matcher(path).find() || isHdf5File(path))
			return StorageFormat.HDF5;
		return null;
	}

	private static StorageFormat fromKeys(final KeyValueRoot root) {

		// A local HDF5 file can be wrapped in a KeyValueRoot but is not a
		// container, so this has to be decided before any key lookup.
		final String path = root.uri().getPath();
		if (path != null && isHdf5File(path))
			return StorageFormat.HDF5;

		if (root.isFile(N5FilePath.of(ZARR3_ATTRIBUTES)))
			return StorageFormat.ZARR3;
		if (Arrays.stream(ZARR2_KEYS).anyMatch(key -> root.isFile(N5FilePath.of(key))))
			return StorageFormat.ZARR2;
		if (root.isFile(N5FilePath.of(N5_ATTRIBUTES)))
			return StorageFormat.N5;
		return null;
	}


	/** The 8-byte HDF5 signature, as in {@code HDF5Utils.HDF5_SIG}. */
	private static final byte[] HDF5_SIG = {(byte)137, 72, 68, 70, 13, 10, 26, 10};

	/**
	 * Check whether {@code path} is a regular file that starts with the
	 * HDF5 signature.
	 * <p>
	 * Inlined from HDF5Utils.isHDF5 (n5-hdf5) so that recognising an HDF5
	 * container needs no HDF5 dependency. We want to be able to detect HDF5
	 * even if we cannot open it.
	 *
	 * @param path path to check
	 * @return true if {@code path} is an existing regular file whose first eight
	 * 		bytes are the HDF5 signature; false for a missing, non-regular,
	 * 		shorter or unreadable file
	 */
	private static boolean isHdf5File(final String path) {

		final File file = new File(path);
		if (!file.isFile())
			return false;

		try (final DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			final byte[] sig = new byte[HDF5_SIG.length];
			in.readFully(sig);
			return Arrays.equals(sig, HDF5_SIG);
		} catch (final IOException e) {
			return false;
		}
	}
}
