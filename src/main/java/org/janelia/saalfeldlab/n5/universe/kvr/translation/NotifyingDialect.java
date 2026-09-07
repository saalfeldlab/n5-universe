package org.janelia.saalfeldlab.n5.universe.kvr.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;
import java.util.Map;
import org.janelia.saalfeldlab.n5.ContainerDialect;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.HierarchyStore;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Exception.N5ClassCastException;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5Exception.N5JsonParseException;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;

/**
 * A {@link ContainerDialect} wrapper that runs a callback after every mutating
 * operation.
 * <p>
 * This is how {@link TranslatedN5Writer} implements writing to the original
 * store: Its {@code NotifyingDialect} delegates to a {@code ContainerDialect}
 * on a translated {@code DirectoryStore}. After mutation, the callback triggers
 * inverse translation to the original hierarchy and writes it back to the
 * original store.
 * <p>
 * A {@code NotifyingDialect} never delegates to another one. Wrapping an
 * existing {@code NotifyingDialect} instead re-wraps its delegate and extends
 * its callback chain.
 */
final class NotifyingDialect implements ContainerDialect {

	final ContainerDialect delegate;

	private final Runnable onModified;

	/**
	 * Wrap {@code dialect} to run {@code onModified} after every mutating
	 * operation.
	 * <p>
	 * If {@code dialect} is already a {@code NotifyingDialect}, wrap its
	 * delegate instead and after mutating operations first run {@code
	 * onModified} then {@code dialect}'s callback.
	 *
	 * @param delegate
	 * 		the dialect to wrap
	 * @param onModified
	 * 		run after every modifying operation
	 */
	NotifyingDialect(final ContainerDialect delegate, final Runnable onModified) {

		if (delegate instanceof NotifyingDialect) {
			final NotifyingDialect notifyingDialect = (NotifyingDialect) delegate;
			this.delegate = notifyingDialect.delegate;
			this.onModified = () -> {
				onModified.run();
				notifyingDialect.onModified.run();
			};
		} else {
			this.delegate = delegate;
			this.onModified = onModified;
		}
	}

	@Override
	public ContainerDialect withStore(final HierarchyStore store) {
		return new NotifyingDialect(delegate.withStore(store), onModified);
	}


	// -- read: delegate only --

	@Override
	public HierarchyStore getHierarchyStore() {
		return delegate.getHierarchyStore();
	}

	@Override
	public Gson getGson() {
		return delegate.getGson();
	}

	@Override
	public <T> T getAttribute(final N5DirectoryPath path, final String attributePath, final Type type)
			throws N5IOException, N5ClassCastException {
		return delegate.getAttribute(path, attributePath, type);
	}

	@Override
	public DatasetAttributes getDatasetAttributes(final N5DirectoryPath path) throws N5IOException {
		return delegate.getDatasetAttributes(path);
	}

	@Override
	public boolean datasetExists(final N5DirectoryPath path) throws N5IOException {
		return delegate.datasetExists(path);
	}

	@Override
	public boolean groupExists(final N5DirectoryPath path) throws N5IOException {
		return delegate.groupExists(path);
	}

	@Override
	public String[] list(final N5DirectoryPath path) throws N5IOException {
		return delegate.list(path);
	}

	@Override
	public Map<String, Class<?>> listAttributes(final N5DirectoryPath path)
			throws N5IOException, N5JsonParseException {
		return delegate.listAttributes(path);
	}

	@Override
	public JsonElement getAttributes(final N5DirectoryPath path) throws N5IOException {
		return delegate.getAttributes(path);
	}


	// -- write: delegate and notify --

	@Override
	public <T> void setAttribute(final N5DirectoryPath path, final String attributePath, final T attribute)
			throws N5IOException {
		delegate.setAttribute(path, attributePath, attribute);
		onModified.run();
	}

	@Override
	public void setAttributes(final N5DirectoryPath path, final Map<String, ?> attributes) throws N5IOException {
		delegate.setAttributes(path, attributes);
		onModified.run();
	}

	@Override
	public boolean removeAttribute(final N5DirectoryPath path, final String attributePath) throws N5IOException {
		final boolean removed = delegate.removeAttribute(path, attributePath);
		onModified.run();
		return removed;
	}

	@Override
	public <T> T removeAttribute(final N5DirectoryPath path, final String attributePath, final Class<T> clazz)
			throws N5Exception {
		final T removed = delegate.removeAttribute(path, attributePath, clazz);
		onModified.run();
		return removed;
	}

	@Override
	public void setDatasetAttributes(final N5DirectoryPath path, final DatasetAttributes attributes)
			throws N5IOException {
		delegate.setDatasetAttributes(path, attributes);
		onModified.run();
	}

	@Override
	public void createGroup(final N5DirectoryPath path) throws N5IOException {
		delegate.createGroup(path);
		onModified.run();
	}

	@Override
	public void createDataset(final N5DirectoryPath path, final DatasetAttributes attributes) throws N5IOException {
		delegate.createDataset(path, attributes);
		onModified.run();
	}

	@Override
	public boolean remove(final N5DirectoryPath path) throws N5IOException {
		final boolean removed = delegate.remove(path);
		onModified.run();
		return removed;
	}
}
