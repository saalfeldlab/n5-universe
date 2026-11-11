package org.janelia.saalfeldlab.n5.universe;

/**
 * Enumerates available backends.
 * <p>
 * This enum used to perform the backend dispatch itself: each constant
 * paired a {@code Predicate<URI>} with a generator for the matching
 * {@code KeyValueAccess}, and {@code getKeyValueAccess} returned the
 * first constant whose predicate matched.
 * That logic now lives inside {@link N5Factory}, which resolves the backend
 * from the URI itself.
 * <p>
 * What is left here is a plain enum with no behaviour, which is how callers
 * outside this project used it anyway: naming a constant to pass to
 * {@code N5Factory}.
 *
 * @deprecated let {@link N5Factory} resolve the backend from the URI — the
 * 		overloads that take no backend do exactly that, and do it better.
 * 		The enum itself has no replacement; it survives only to keep the
 * 		deprecated {@code N5Factory.openReader}/{@code openWriter}
 * 		overloads that accept one compiling, and goes away with them.
 */
@Deprecated
public enum KeyValueAccessBackend {
	GOOGLE_CLOUD, AWS, HTTP, FILE
}
